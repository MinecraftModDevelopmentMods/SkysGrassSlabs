package zone.moddev.mc.skysgrassslabs.world;

import java.util.List;
import java.util.Random;
import net.minecraft.block.BlockDirtSnowy;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.IChunkGenSettings;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.CompositeFeature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.placement.IPlacementConfig;
import net.minecraftforge.registries.ForgeRegistries;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Deterministic two-pass slope smoothing for newly generated Overworld chunks. */
public final class GrassSlabSmoothingFeature extends ContextFeature<NoFeatureConfig> {
    public static final String FEATURE_ID = "skysgrassslabs:grass_slab_smoothing";
    private static final GrassSlabSmoothingFeature FEATURE = new GrassSlabSmoothingFeature();
    private static final ThreadLocal<boolean[]> DECISIONS =
            ThreadLocal.withInitial(() -> new boolean[256]);
    private static CompositeFeature<?, ?> configuredFeature;
    private static boolean installed;

    public static synchronized void install() {
        if (installed) return;
        configuredFeature = Biome.createCompositeFeature(FEATURE, new NoFeatureConfig(),
                Biome.PASSTHROUGH, IPlacementConfig.NO_PLACEMENT_CONFIG);
        for (Biome biome : ForgeRegistries.BIOMES.getValues()) {
            if (biome.getCategory() == Biome.Category.NETHER ||
                    biome.getCategory() == Biome.Category.THEEND) {
                continue;
            }
            List<CompositeFeature<?, ?>> features = biome.getFeatures(
                    GenerationStage.Decoration.VEGETAL_DECORATION);
            if (!features.contains(configuredFeature)) features.add(0, configuredFeature);
        }
        installed = true;
    }

    @Override
    boolean place(IWorld world, IChunkGenerator<? extends IChunkGenSettings> generator,
            Random random, BlockPos origin, NoFeatureConfig config) {
        if (!SkysGrassSlabsConfig.isSmoothingActive() ||
                world.getDimension().getType() != DimensionType.OVERWORLD) {
            return false;
        }

        // Forge 25 supplies a decoration origin one chunk north-west of the owner.
        int ownerX = (origin.getX() >> 4) + 1;
        int ownerZ = (origin.getZ() >> 4) + 1;
        if (LegacyWorldDataHook.isLegacyChunk(ownerX, ownerZ) ||
                !chunkAvailable(world, ownerX, ownerZ)) {
            return false;
        }
        IChunk owner = world.getChunk(ownerX, ownerZ);
        boolean[] decisions = DECISIONS.get();
        java.util.Arrays.fill(decisions, false);
        int startX = ownerX << 4;
        int startZ = ownerZ << 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int localZ = 0; localZ < 16; ++localZ) {
            for (int localX = 0; localX < 16; ++localX) {
                int surfaceY = surfaceY(owner, localX, localZ, cursor);
                if (surfaceY < 0 || surfaceY >= 255) continue;
                BlockPos surface = new BlockPos(startX + localX, surfaceY, startZ + localZ);
                BlockPos target = surface.up();
                IBlockState lower = owner.getBlockState(surface);
                IBlockState targetState = owner.getBlockState(target);
                if (!SmoothingDecision.isEligibleTarget(
                        lower.getBlock() == Blocks.GRASS_BLOCK,
                        targetState.isAir(world, target), targetState.getFluidState().isEmpty(),
                        lower.isTopSolid(), owner.getTileEntity(target) != null)) {
                    continue;
                }
                if (hasHigherGrassNeighbour(world, ownerX, ownerZ, localX, localZ,
                        surfaceY, cursor)) {
                    decisions[(localZ << 4) | localX] = true;
                }
            }
        }

        boolean changed = false;
        IBlockState slab = ModBlocks.GRASS_SLAB.getDefaultState()
                .with(BlockSlab.TYPE, SlabType.BOTTOM)
                .with(BlockSlab.WATERLOGGED, Boolean.FALSE)
                .with(BlockDirtSnowy.SNOWY, Boolean.FALSE);
        for (int index = 0; index < decisions.length; ++index) {
            if (!decisions[index]) continue;
            int localX = index & 15;
            int localZ = index >>> 4;
            int surfaceY = surfaceY(owner, localX, localZ, cursor);
            BlockPos support = new BlockPos(startX + localX, surfaceY, startZ + localZ);
            owner.setBlockState(support.up(), slab, false);
            if (owner.getBlockState(support).getBlock() == Blocks.GRASS_BLOCK) {
                owner.setBlockState(support, Blocks.DIRT.getDefaultState(), false);
            }
            changed = true;
        }
        return changed;
    }

    private static boolean hasHigherGrassNeighbour(IWorld world, int ownerX, int ownerZ,
            int localX, int localZ, int lowerY, BlockPos.MutableBlockPos cursor) {
        return isHigherGrass(world, ownerX, ownerZ, localX - 1, localZ, lowerY, cursor) ||
                isHigherGrass(world, ownerX, ownerZ, localX + 1, localZ, lowerY, cursor) ||
                isHigherGrass(world, ownerX, ownerZ, localX, localZ - 1, lowerY, cursor) ||
                isHigherGrass(world, ownerX, ownerZ, localX, localZ + 1, lowerY, cursor);
    }

    private static boolean isHigherGrass(IWorld world, int ownerX, int ownerZ,
            int localX, int localZ, int lowerY, BlockPos.MutableBlockPos cursor) {
        int chunkX = ownerX + Math.floorDiv(localX, 16);
        int chunkZ = ownerZ + Math.floorDiv(localZ, 16);
        if (!chunkAvailable(world, chunkX, chunkZ)) return false;
        IChunk chunk = world.getChunk(chunkX, chunkZ);
        int x = Math.floorMod(localX, 16);
        int z = Math.floorMod(localZ, 16);
        int higherY = surfaceY(chunk, x, z, cursor);
        cursor.setPos((chunkX << 4) + x, higherY, (chunkZ << 4) + z);
        return SmoothingDecision.isOneBlockGrassTransition(lowerY, higherY,
                chunk.getBlockState(cursor).getBlock() == Blocks.GRASS_BLOCK);
    }

    private static int surfaceY(IChunk chunk, int localX, int localZ,
            BlockPos.MutableBlockPos cursor) {
        int y = chunk.getTopBlockY(Heightmap.Type.WORLD_SURFACE_WG, localX, localZ);
        int x = chunk.getPos().getXStart() + localX;
        int z = chunk.getPos().getZStart() + localZ;
        while (y >= 0 && chunk.getBlockState(cursor.setPos(x, y, z)).isAir()) --y;
        return y;
    }

    private static boolean chunkAvailable(IWorld world, int chunkX, int chunkZ) {
        return !(world instanceof WorldGenRegion) ||
                ((WorldGenRegion) world).isChunkInBounds(chunkX, chunkZ);
    }

    private GrassSlabSmoothingFeature() {
    }
}
