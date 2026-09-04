package zone.moddev.mc.skysgrassslabs.world;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.placement.NoPlacementConfig;
import net.minecraft.world.gen.placement.Placement;
import net.minecraftforge.event.world.BiomeLoadingEvent;
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
        super(NoFeatureConfig.CODEC);
    }

    private static GrassSlabSmoothingFeature configureFeature() {
        GrassSlabSmoothingFeature feature = new GrassSlabSmoothingFeature();
        feature.setRegistryName(new ResourceLocation(SkysGrassSlabs.MOD_ID,
                "grass_slab_smoothing"));
        return feature;
    }

    public static synchronized void install() {
        if (installed) return;
        ResourceLocation id = new ResourceLocation(SkysGrassSlabs.MOD_ID,
                "grass_slab_smoothing");
        configuredFeature = Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, id,
                FEATURE.configured(NoFeatureConfig.INSTANCE)
                        .decorated(Placement.NOPE.configured(NoPlacementConfig.INSTANCE)));
        installed = true;
    }

    public static void onBiomeLoading(BiomeLoadingEvent event) {
        if (configuredFeature == null || event.getCategory() == Biome.Category.NETHER ||
                event.getCategory() == Biome.Category.THEEND) {
            return;
        }
        List<Supplier<ConfiguredFeature<?, ?>>> features = event.getGeneration().getFeatures(
                GenerationStage.Decoration.VEGETAL_DECORATION);
        for (Supplier<ConfiguredFeature<?, ?>> feature : features) {
            if (feature.get() == configuredFeature) return;
        }
        features.add(0, () -> configuredFeature);
    }

    @Override
    public boolean place(ISeedReader world, ChunkGenerator generator,
            Random random, BlockPos origin, NoFeatureConfig config) {
        if (!SkysGrassSlabsConfig.isSmoothingActive() ||
                world.getLevel().dimension() != World.OVERWORLD) {
            return false;
        }

        int ownerX = origin.getX() >> 4;
        int ownerZ = origin.getZ() >> 4;
        if (world instanceof WorldGenRegion) {
            WorldGenRegion region = (WorldGenRegion) world;
            ownerX = region.getCenterX();
            ownerZ = region.getCenterZ();
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
                BlockPos target = surface.above();
                BlockState lower = owner.getBlockState(surface);
                BlockState targetState = owner.getBlockState(target);
                if (!SmoothingDecision.isEligibleTarget(
                        lower.getBlock() == Blocks.GRASS_BLOCK,
                        targetState.isAir(), targetState.getFluidState().isEmpty(),
                        lower.isCollisionShapeFullBlock(world, surface),
                        owner.getBlockEntityNbt(target) != null)) {
                    continue;
                }
                if (hasHigherGrassNeighbour(world, ownerX, ownerZ, localX, localZ,
                        surfaceY, cursor)) {
                    decisions[(localZ << 4) | localX] = true;
                }
            }
        }

        boolean changed = false;
        BlockState slab = ModBlocks.GRASS_SLAB.defaultBlockState()
                 .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                 .setValue(SlabBlock.WATERLOGGED, Boolean.FALSE)
                 .setValue(SnowyDirtBlock.SNOWY, Boolean.FALSE);
        for (int index = 0; index < decisions.length; ++index) {
            if (!decisions[index]) continue;
            int localX = index & 15;
            int localZ = index >>> 4;
            int surfaceY = surfaceY(owner, localX, localZ, cursor);
            BlockPos support = new BlockPos(startX + localX, surfaceY, startZ + localZ);
            owner.setBlockState(support.above(), slab, false);
            if (owner.getBlockState(support).getBlock() == Blocks.GRASS_BLOCK) {
                owner.setBlockState(support, Blocks.DIRT.defaultBlockState(), false);
            }
            changed = true;
        }
        return changed;
    }

    private static boolean hasHigherGrassNeighbour(ISeedReader world, int ownerX, int ownerZ,
            int localX, int localZ, int lowerY, BlockPos.Mutable cursor) {
        return isHigherGrass(world, ownerX, ownerZ, localX - 1, localZ, lowerY, cursor) ||
                isHigherGrass(world, ownerX, ownerZ, localX + 1, localZ, lowerY, cursor) ||
                isHigherGrass(world, ownerX, ownerZ, localX, localZ - 1, lowerY, cursor) ||
                isHigherGrass(world, ownerX, ownerZ, localX, localZ + 1, lowerY, cursor);
    }

    private static boolean isHigherGrass(ISeedReader world, int ownerX, int ownerZ,
            int localX, int localZ, int lowerY, BlockPos.Mutable cursor) {
        int chunkX = ownerX + Math.floorDiv(localX, 16);
        int chunkZ = ownerZ + Math.floorDiv(localZ, 16);
        if (!chunkAvailable(world, chunkX, chunkZ)) return false;
        IChunk chunk = world.getChunk(chunkX, chunkZ);
        int x = Math.floorMod(localX, 16);
        int z = Math.floorMod(localZ, 16);
        int higherY = surfaceY(chunk, x, z, cursor);
        cursor.set((chunkX << 4) + x, higherY, (chunkZ << 4) + z);
        return SmoothingDecision.isOneBlockGrassTransition(lowerY, higherY,
                chunk.getBlockState(cursor).getBlock() == Blocks.GRASS_BLOCK);
    }

    private static int surfaceY(IChunk chunk, int localX, int localZ,
            BlockPos.Mutable cursor) {
        int y = chunk.getHeight(Heightmap.Type.WORLD_SURFACE_WG, localX, localZ);
        int x = chunk.getPos().getMinBlockX() + localX;
        int z = chunk.getPos().getMinBlockZ() + localZ;
        while (y >= 0 && chunk.getBlockState(cursor.set(x, y, z)).isAir()) --y;
        return y;
    }

    private static boolean chunkAvailable(ISeedReader world, int chunkX, int chunkZ) {
        return world.hasChunk(chunkX, chunkZ);
    }
}
