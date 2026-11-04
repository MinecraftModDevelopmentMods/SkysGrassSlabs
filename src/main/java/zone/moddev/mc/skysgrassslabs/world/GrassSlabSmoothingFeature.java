package zone.moddev.mc.skysgrassslabs.world;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;

/** Deterministic two-pass slope smoothing for newly generated Overworld chunks. */
public final class GrassSlabSmoothingFeature extends Feature<NoneFeatureConfiguration> {
    public static final String FEATURE_ID = "skysgrassslabs:grass_slab_smoothing";
    private static final ThreadLocal<Decisions> DECISIONS =
            ThreadLocal.withInitial(Decisions::new);

    public GrassSlabSmoothingFeature() {
        this(NoneFeatureConfiguration.CODEC);
    }

    public GrassSlabSmoothingFeature(com.mojang.serialization.Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        if (GrassSlabsApi.hasSmoothingOwner() || !SkysGrassSlabsConfig.isSmoothingActive() ||
                world.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        int ownerX = origin.getX() >> 4;
        int ownerZ = origin.getZ() >> 4;
        if (world instanceof WorldGenRegion) {
            WorldGenRegion region = (WorldGenRegion) world;
            ownerX = region.getCenter().x;
            ownerZ = region.getCenter().z;
        }
        if (LegacyWorldDataHook.isLegacyChunk(ownerX, ownerZ) ||
                !chunkAvailable(world, ownerX, ownerZ)) {
            return false;
        }
        ChunkAccess owner = world.getChunk(ownerX, ownerZ);
        StructureProtection structures = StructureProtection.read(owner, position ->
                chunkAvailable(world, position.x, position.z)
                        ? world.getChunk(position.x, position.z) : null);
        if (!structures.canInspectSafely()) {
            return false;
        }
        Decisions decisions = DECISIONS.get();
        Arrays.fill(decisions.targetHeights, Integer.MIN_VALUE);
        Arrays.fill(decisions.originalTargets, null);
        int startX = ownerX << 4;
        int startZ = ownerZ << 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int localZ = 0; localZ < 16; ++localZ) {
            for (int localX = 0; localX < 16; ++localX) {
                int surfaceY = surfaceY(owner, localX, localZ, cursor);
                if (surfaceY < owner.getMinBuildHeight() ||
                        surfaceY >= owner.getMaxBuildHeight() - 1) continue;
                BlockPos surface = new BlockPos(startX + localX, surfaceY, startZ + localZ);
                BlockPos target = surface.above();
                BlockState lower = owner.getBlockState(surface);
                BlockState targetState = owner.getBlockState(target);
                if (structures.contains(surface) || structures.contains(target)) {
                    continue;
                }
                if (!SmoothingDecision.isEligibleTarget(
                        lower.getBlock() == Blocks.GRASS_BLOCK,
                        SmoothingFoliage.canReplace(world, owner, target), targetState.getFluidState().isEmpty(),
                        lower.isCollisionShapeFullBlock(world, surface),
                        owner.getBlockEntity(target) != null || owner.getBlockEntityNbt(target) != null)) {
                    continue;
                }
                if (hasHigherGrassNeighbour(world, ownerX, ownerZ, localX, localZ,
                        surfaceY, cursor)) {
                    int index = (localZ << 4) | localX;
                    decisions.targetHeights[index] = target.getY();
                    decisions.originalTargets[index] = targetState;
                }
            }
        }

        boolean changed = false;
        BlockState slab = ModBlocks.GRASS_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                .setValue(SlabBlock.WATERLOGGED, Boolean.FALSE)
                .setValue(SnowyDirtBlock.SNOWY, Boolean.FALSE);
        for (int index = 0; index < decisions.targetHeights.length; ++index) {
            if (decisions.targetHeights[index] == Integer.MIN_VALUE) continue;
            int localX = index & 15;
            int localZ = index >>> 4;
            // Keep the planned height; earlier writes must not move another column's target.
            BlockPos target = new BlockPos(startX + localX, decisions.targetHeights[index], startZ + localZ);
            if (owner.getBlockState(target) != decisions.originalTargets[index]
                    || !SmoothingFoliage.canReplace(world, owner, target)) {
                continue;
            }
            owner.setBlockState(target, slab, false);
            if (owner.getBlockState(target) != slab) {
                continue;
            }
            if (decisions.originalTargets[index].getBlock() instanceof DoublePlantBlock) {
                owner.setBlockState(target.above(), Blocks.AIR.defaultBlockState(), false);
            }
            BlockPos support = target.below();
            if (owner.getBlockState(support).getBlock() == Blocks.GRASS_BLOCK) {
                owner.setBlockState(support, Blocks.DIRT.defaultBlockState(), false);
            }
            changed = true;
        }
        return changed;
    }

    private static boolean hasHigherGrassNeighbour(WorldGenLevel world, int ownerX, int ownerZ,
            int localX, int localZ, int lowerY, BlockPos.MutableBlockPos cursor) {
        return isHigherGrass(world, ownerX, ownerZ, localX - 1, localZ, lowerY, cursor) ||
                isHigherGrass(world, ownerX, ownerZ, localX + 1, localZ, lowerY, cursor) ||
                isHigherGrass(world, ownerX, ownerZ, localX, localZ - 1, lowerY, cursor) ||
                isHigherGrass(world, ownerX, ownerZ, localX, localZ + 1, lowerY, cursor);
    }

    private static boolean isHigherGrass(WorldGenLevel world, int ownerX, int ownerZ,
            int localX, int localZ, int lowerY, BlockPos.MutableBlockPos cursor) {
        int chunkX = ownerX + Math.floorDiv(localX, 16);
        int chunkZ = ownerZ + Math.floorDiv(localZ, 16);
        if (!chunkAvailable(world, chunkX, chunkZ)) return false;
        ChunkAccess chunk = world.getChunk(chunkX, chunkZ);
        int x = Math.floorMod(localX, 16);
        int z = Math.floorMod(localZ, 16);
        int higherY = surfaceY(chunk, x, z, cursor);
        cursor.set((chunkX << 4) + x, higherY, (chunkZ << 4) + z);
        return SmoothingDecision.isOneBlockGrassTransition(lowerY, higherY,
                isNaturalGrassSurface(chunk, cursor));
    }

    private static int surfaceY(ChunkAccess chunk, int localX, int localZ,
            BlockPos.MutableBlockPos cursor) {
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ);
        int x = chunk.getPos().getMinBlockX() + localX;
        int z = chunk.getPos().getMinBlockZ() + localZ;
        int minimumY = chunk.getMinBuildHeight();
        while (y >= minimumY && (chunk.getBlockState(cursor.set(x, y, z)).isAir()
                || SmoothingFoliage.isPlant(chunk.getBlockState(cursor)))) {
            --y;
        }
        if (y > minimumY && isGrassCap(chunk.getBlockState(cursor.set(x, y, z)))
                && chunk.getBlockState(cursor.below()).getBlock() == Blocks.DIRT) {
            --y;
        }
        return y;
    }

    private static boolean chunkAvailable(WorldGenLevel world, int chunkX, int chunkZ) {
        return world.hasChunk(chunkX, chunkZ);
    }

    private static boolean isNaturalGrassSurface(ChunkAccess chunk, BlockPos position) {
        BlockState state = chunk.getBlockState(position);
        return state.getBlock() == Blocks.GRASS_BLOCK || state.getBlock() == Blocks.DIRT
                && isGrassCap(chunk.getBlockState(position.above()));
    }

    private static boolean isGrassCap(BlockState state) {
        return state.getBlock() == ModBlocks.GRASS_SLAB && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM
                && !state.getValue(SlabBlock.WATERLOGGED);
    }

    private static final class Decisions {
        final int[] targetHeights = new int[256];
        final BlockState[] originalTargets = new BlockState[256];
    }
}
