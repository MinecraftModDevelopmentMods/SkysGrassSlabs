package zone.moddev.mc.skysgrassslabs.gametest;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.world.BiomeModifier.Phase;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.GrassSlabSmoothingFeature;

/** Controlled runtime proof for owning-chunk, border, rejection and idempotence rules. */
public final class WorldgenGameTests {
    private WorldgenGameTests() {
    }

    public static void smoothingIsFirstVegetationFeature(GameTestHelper helper) {
        var biome = helper.getLevel().getBiome(helper.absolutePos(BlockPos.ZERO));
        var modifiers = helper.getLevel().registryAccess()
                .lookupOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        var modifier = modifiers.get(Identifier.fromNamespaceAndPath(
                SkysGrassSlabs.MOD_ID, "grass_slab_smoothing"))
                .orElseThrow().value();
        require(helper, modifier instanceof zone.moddev.mc.skysgrassslabs.world.SmoothingBiomeModifier,
                "registered smoothing biome modifier is missing");

        var vegetation = biome.value().getGenerationSettings().features()
                .get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal());
        require(helper, vegetation.size() > 0, "biome has no vegetation features");
        PlacedFeature first = vegetation.get(0).value();
        require(helper, first.feature().value() instanceof GrassSlabSmoothingFeature,
                "smoother is not the first vegetation feature");
        helper.succeed();
    }

    public static void bothModifierSignaturesPreservePhaseOrderAndDeduplication(GameTestHelper helper) {
        var biome = helper.getLevel().getBiome(helper.absolutePos(BlockPos.ZERO));
        var value = biome.value();
        var original = new BiomeInfo(value.getModifiedClimateSettings(), value.getAttributes(),
                value.getModifiedSpecialEffects(), value.getGenerationSettings());
        var key = biome.unwrapKey().orElseThrow();
        var legacy = BiomeInfo.Builder.copyOf(key, original);
        var current = BiomeInfo.Builder.copyOf(key, original);
        var step = GenerationStep.Decoration.VEGETAL_DECORATION;
        for (var builder : java.util.List.of(legacy, current)) {
            builder.getGenerationSettings().getFeatures(step).removeIf(feature ->
                    feature.value().feature().value() instanceof GrassSlabSmoothingFeature);
        }
        var before = java.util.List.copyOf(current.getGenerationSettings().getFeatures(step));
        var modifier = zone.moddev.mc.skysgrassslabs.world.SmoothingBiomeModifier.INSTANCE;
        for (Phase phase : Phase.values()) {
            if (phase == Phase.AFTER_EVERYTHING) {
                continue;
            }
            modifier.modify(biome, phase, legacy);
            modifier.modify(helper.getLevel().registryAccess(), biome, phase, current);
            require(helper, legacy.getGenerationSettings().getFeatures(step).equals(before)
                    && current.getGenerationSettings().getFeatures(step).equals(before),
                    "a nonfinal phase changed vegetation features");
        }
        modifier.modify(biome, Phase.AFTER_EVERYTHING, legacy);
        modifier.modify(helper.getLevel().registryAccess(), biome, Phase.AFTER_EVERYTHING, current);
        var expected = java.util.List.copyOf(legacy.getGenerationSettings().getFeatures(step));
        require(helper, current.getGenerationSettings().getFeatures(step).equals(expected),
                "the two loader entry points changed feature order");
        require(helper, expected.size() == before.size() + 1
                && expected.getFirst().value().feature().value() instanceof GrassSlabSmoothingFeature,
                "smoothing was not inserted exactly once at index zero");
        modifier.modify(helper.getLevel().registryAccess(), biome, Phase.AFTER_EVERYTHING, current);
        modifier.modify(biome, Phase.AFTER_EVERYTHING, current);
        require(helper, current.getGenerationSettings().getFeatures(step).equals(expected),
                "repeat calls duplicated the smoothing feature");
        helper.succeed();
    }

    public static void smoothingIsBorderSafeAndIdempotent(GameTestHelper helper) {
        ChunkPos owner = ChunkPos.containing(helper.absolutePos(new BlockPos(1, 2, 1)));
        int y = 120;

        BlockPos center = new BlockPos(owner.getMinBlockX() + 8, y, owner.getMinBlockZ() + 8);
        makeOneBlockRise(helper, center, center.east());

        BlockPos border = new BlockPos(owner.getMinBlockX(), y, owner.getMinBlockZ() + 8);
        makeOneBlockRise(helper, border, border.west());

        BlockPos flat = new BlockPos(owner.getMinBlockX() + 8, y, owner.getMinBlockZ() + 12);
        makeFlat(helper, flat);

        BlockPos twoHigh = new BlockPos(owner.getMinBlockX() + 12, y, owner.getMinBlockZ() + 8);
        makeTwoBlockRise(helper, twoHigh, twoHigh.east());

        BlockPos wet = new BlockPos(owner.getMinBlockX() + 4, y, owner.getMinBlockZ() + 8);
        makeOneBlockRise(helper, wet, wet.east());
        helper.getLevel().setBlock(wet.above(), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);

        BlockPos chest = new BlockPos(owner.getMinBlockX() + 4, y + 1, owner.getMinBlockZ() + 12);
        helper.getLevel().setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);

        helper.runAfterDelay(5, () -> {
            primeLoadedHeightmaps(helper, owner);
            GrassSlabSmoothingFeature feature = new GrassSlabSmoothingFeature();
            require(helper, feature.place(helper.getLevel(),
                    helper.getLevel().getChunkSource().getGenerator(),
                    RandomSource.create(19780401L), center),
                    "controlled feature pass made no changes");
            require(helper, helper.getLevel().getBlockState(center.above())
                    .is(ModBlocks.GRASS_SLAB.get()), "one-block transition was not smoothed");
            require(helper, helper.getLevel().getBlockState(border.above())
                    .is(ModBlocks.GRASS_SLAB.get()), "owning-chunk border transition was not smoothed");
            require(helper, helper.getLevel().getBlockState(border.west().above())
                    .is(Blocks.GRASS_BLOCK), "feature wrote outside the owning chunk");
            require(helper, helper.getLevel().getBlockState(flat.above()).isAir(),
                    "flat terrain was modified");
            require(helper, helper.getLevel().getBlockState(twoHigh.above()).isAir(),
                    "two-block cliff was modified");
            require(helper, helper.getLevel().getBlockState(wet.above()).is(Blocks.WATER),
                    "water target was overwritten");
            require(helper, helper.getLevel().getBlockState(chest).is(Blocks.CHEST)
                    && helper.getLevel().getBlockEntity(chest) != null,
                    "block entity was overwritten");
            int firstCount = countGrassSlabs(helper, owner);
            helper.runAfterDelay(10, () -> {
                boolean secondChanged = feature.place(helper.getLevel(),
                        helper.getLevel().getChunkSource().getGenerator(),
                        RandomSource.create(19780401L), center);
                int secondCount = countGrassSlabs(helper, owner);
                require(helper, !secondChanged && secondCount == firstCount,
                        "settled second pass changed output: before=" + firstCount
                                + ", after=" + secondCount);
                helper.succeed();
            });
        });
    }

    private static void primeLoadedHeightmaps(GameTestHelper helper, ChunkPos owner) {
        for (int chunkX = owner.x() - 1; chunkX <= owner.x() + 1; chunkX++) {
            for (int chunkZ = owner.z() - 1; chunkZ <= owner.z() + 1; chunkZ++) {
                var chunk = helper.getLevel().getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk != null) {
                    Heightmap.primeHeightmaps(chunk, Set.of(Heightmap.Types.WORLD_SURFACE_WG));
                }
            }
        }
    }

    private static int countGrassSlabs(GameTestHelper helper, ChunkPos chunk) {
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = helper.getLevel().getMinY();
                        y <= helper.getLevel().getMaxY(); y++) {
                    if (helper.getLevel().getBlockState(cursor.set(x, y, z))
                            .is(ModBlocks.GRASS_SLAB.get())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static void makeOneBlockRise(GameTestHelper helper, BlockPos lower, BlockPos higher) {
        clearColumnAbove(helper, lower, 2);
        clearColumnAbove(helper, higher, 2);
        solidRing(helper, lower);
        helper.getLevel().setBlock(lower, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(higher, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(higher.above(), Blocks.GRASS_BLOCK.defaultBlockState(),
                Block.UPDATE_ALL);
    }

    private static void makeFlat(GameTestHelper helper, BlockPos center) {
        clearColumnAbove(helper, center, 2);
        clearColumnAbove(helper, center.east(), 2);
        solidRing(helper, center);
        helper.getLevel().setBlock(center, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(center.east(), Blocks.GRASS_BLOCK.defaultBlockState(),
                Block.UPDATE_ALL);
    }

    private static void makeTwoBlockRise(GameTestHelper helper, BlockPos lower, BlockPos higher) {
        clearColumnAbove(helper, lower, 3);
        clearColumnAbove(helper, higher, 3);
        solidRing(helper, lower);
        helper.getLevel().setBlock(lower, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(higher, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(higher.above(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(higher.above(2), Blocks.GRASS_BLOCK.defaultBlockState(),
                Block.UPDATE_ALL);
    }

    private static void clearColumnAbove(GameTestHelper helper, BlockPos base, int height) {
        for (int offset = 1; offset <= height; ++offset) {
            helper.getLevel().setBlock(base.above(offset), Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL);
        }
    }

    private static void solidRing(GameTestHelper helper, BlockPos center) {
        helper.getLevel().setBlock(center.north(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(center.south(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(center.west(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(center.east(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
