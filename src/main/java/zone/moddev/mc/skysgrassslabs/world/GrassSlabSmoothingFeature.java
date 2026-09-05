package zone.moddev.mc.skysgrassslabs.world;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
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
import net.minecraftforge.event.world.BiomeLoadingEvent;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Deterministic two-pass slope smoothing for newly generated Overworld chunks. */
public final class GrassSlabSmoothingFeature extends Feature<NoneFeatureConfiguration> {
    public static final String FEATURE_ID = "skysgrassslabs:grass_slab_smoothing";
    public static final GrassSlabSmoothingFeature FEATURE = configureFeature();
    private static final ThreadLocal<boolean[]> DECISIONS =
            ThreadLocal.withInitial(() -> new boolean[256]);
    private static ConfiguredFeature<?, ?> configuredFeature;
    private static boolean installed;

    public GrassSlabSmoothingFeature() {
        super(NoneFeatureConfiguration.CODEC);
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
        configuredFeature = Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, id,
                FEATURE.configured(NoneFeatureConfiguration.INSTANCE));
        installed = true;
    }

    public static void onBiomeLoading(BiomeLoadingEvent event) {
        if (configuredFeature == null || event.getCategory() == Biome.BiomeCategory.NETHER ||
                event.getCategory() == Biome.BiomeCategory.THEEND) {
            return;
        }
        List<Supplier<ConfiguredFeature<?, ?>>> features = event.getGeneration().getFeatures(
                GenerationStep.Decoration.VEGETAL_DECORATION);
        for (Supplier<ConfiguredFeature<?, ?>> feature : features) {
            if (feature.get() == configuredFeature) return;
        }
        features.add(0, () -> configuredFeature);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        if (!SkysGrassSlabsConfig.isSmoothingActive() ||
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
        boolean[] decisions = DECISIONS.get();
        Arrays.fill(decisions, false);
        int startX = ownerX << 4;
        int startZ = ownerZ << 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

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
                chunk.getBlockState(cursor).getBlock() == Blocks.GRASS_BLOCK);
    }

    private static int surfaceY(ChunkAccess chunk, int localX, int localZ,
            BlockPos.MutableBlockPos cursor) {
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ);
        int x = chunk.getPos().getMinBlockX() + localX;
        int z = chunk.getPos().getMinBlockZ() + localZ;
        while (y >= 0 && chunk.getBlockState(cursor.set(x, y, z)).isAir()) --y;
        return y;
    }

    private static boolean chunkAvailable(WorldGenLevel world, int chunkX, int chunkZ) {
        return world.hasChunk(chunkX, chunkZ);
    }
}
