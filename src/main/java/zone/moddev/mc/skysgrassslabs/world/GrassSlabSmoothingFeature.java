package zone.moddev.mc.skysgrassslabs.world;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationSettings;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.placement.NoPlacementConfig;
import net.minecraft.world.gen.placement.Placement;
import net.minecraftforge.registries.ForgeRegistries;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Deterministic two-pass slope smoothing for newly generated Overworld chunks. */
public final class GrassSlabSmoothingFeature extends Feature<NoFeatureConfig> {
    public static final String FEATURE_ID = "skysgrassslabs:grass_slab_smoothing";
    public static final GrassSlabSmoothingFeature FEATURE = configureFeature();
    private static final ThreadLocal<boolean[]> DECISIONS =
            ThreadLocal.withInitial(() -> new boolean[256]);
    private static ConfiguredFeature<?, ?> configuredFeature;
    private static boolean installed;

    public GrassSlabSmoothingFeature() {
        super(NoFeatureConfig::deserialize);
    }

    private static GrassSlabSmoothingFeature configureFeature() {
        GrassSlabSmoothingFeature feature = new GrassSlabSmoothingFeature();
        feature.setRegistryName(new ResourceLocation(SkysGrassSlabs.MOD_ID,
                "grass_slab_smoothing"));
        return feature;
    }

    public static synchronized void install() {
        if (installed) return;
        configuredFeature = FEATURE.withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG)
                .withPlacement(Placement.NOPE.configure(new NoPlacementConfig()));
        for (Biome biome : ForgeRegistries.BIOMES.getValues()) {
            if (biome.getCategory() == Biome.Category.NETHER ||
                    biome.getCategory() == Biome.Category.THEEND) {
                continue;
            }
            List<ConfiguredFeature<?, ?>> features = biome.getFeatures(
                    GenerationStage.Decoration.VEGETAL_DECORATION);
            if (!features.contains(configuredFeature)) features.add(0, configuredFeature);
        }
        installed = true;
    }

    @Override
    public boolean place(IWorld world, ChunkGenerator<? extends GenerationSettings> generator,
            Random random, BlockPos origin, NoFeatureConfig config) {
        if (!SkysGrassSlabsConfig.isSmoothingActive() ||
                world.getDimension().getType() != DimensionType.OVERWORLD) {
            return false;
        }

        int ownerX = origin.getX() >> 4;
        int ownerZ = origin.getZ() >> 4;
        if (world instanceof WorldGenRegion) {
            WorldGenRegion region = (WorldGenRegion) world;
            ownerX = region.getMainChunkX();
            ownerZ = region.getMainChunkZ();
        }
        if (LegacyWorldDataHook.isLegacyChunk(ownerX, ownerZ) ||
                !chunkAvailable(world, ownerX, ownerZ)) {
            return false;
        }
        IChunk owner = world.getChunk(ownerX, ownerZ);
        boolean[] decisions = DECISIONS.get();
        Arrays.fill(decisions, false);
        int startX = ownerX << 4;
        int startZ = ownerZ << 4;
        BlockPos.Mutable cursor = new BlockPos.Mutable();

        for (int localZ = 0; localZ < 16; ++localZ) {
            for (int localX = 0; localX < 16; ++localX) {
                int surfaceY = surfaceY(owner, localX, localZ, cursor);
                if (surfaceY < 0 || surfaceY >= 255) continue;
                BlockPos surface = new BlockPos(startX + localX, surfaceY, startZ + localZ);
                BlockPos target = surface.up();
                BlockState lower = owner.getBlockState(surface);
                BlockState targetState = owner.getBlockState(target);
                if (!SmoothingDecision.isEligibleTarget(
                        lower.getBlock() == Blocks.GRASS_BLOCK,
                        targetState.isAir(world, target), targetState.getFluidState().isEmpty(),
                        lower.isCollisionShapeOpaque(world, surface),
                        owner.getTileEntity(target) != null)) {
                    continue;
                }
                if (hasHigherGrassNeighbour(world, ownerX, ownerZ, localX, localZ,
                        surfaceY, cursor)) {
                    decisions[(localZ << 4) | localX] = true;
                }
            }
        }

        boolean changed = false;
        BlockState slab = ModBlocks.GRASS_SLAB.getDefaultState()
                .with(SlabBlock.TYPE, SlabType.BOTTOM)
                .with(SlabBlock.WATERLOGGED, Boolean.FALSE)
                .with(SnowyDirtBlock.SNOWY, Boolean.FALSE);
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
            int localX, int localZ, int lowerY, BlockPos.Mutable cursor) {
        return isHigherGrass(world, ownerX, ownerZ, localX - 1, localZ, lowerY, cursor) ||
                isHigherGrass(world, ownerX, ownerZ, localX + 1, localZ, lowerY, cursor) ||
                isHigherGrass(world, ownerX, ownerZ, localX, localZ - 1, lowerY, cursor) ||
                isHigherGrass(world, ownerX, ownerZ, localX, localZ + 1, lowerY, cursor);
    }

    private static boolean isHigherGrass(IWorld world, int ownerX, int ownerZ,
            int localX, int localZ, int lowerY, BlockPos.Mutable cursor) {
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
            BlockPos.Mutable cursor) {
        int y = chunk.getTopBlockY(Heightmap.Type.WORLD_SURFACE_WG, localX, localZ);
        int x = chunk.getPos().getXStart() + localX;
        int z = chunk.getPos().getZStart() + localZ;
        while (y >= 0 && chunk.getBlockState(cursor.setPos(x, y, z)).isAir()) --y;
        return y;
    }

    private static boolean chunkAvailable(IWorld world, int chunkX, int chunkZ) {
        return !(world instanceof WorldGenRegion) ||
                ((WorldGenRegion) world).chunkExists(chunkX, chunkZ);
    }
}
