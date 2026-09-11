package zone.moddev.mc.skysgrassslabs.gametest;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.GrassSlabSmoothingFeature;

/** Controlled runtime proof for owning-chunk, border, rejection and idempotence rules. */
@GameTestHolder(value = SkysGrassSlabs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldgenGameTests {
    private WorldgenGameTests() {
    }

    @GameTest(template = "empty", batch = "worldgen000", timeoutTicks = 100)
    public static void smoothingIsFirstVegetationFeature(GameTestHelper helper) {
        var biome = helper.getLevel().getBiome(helper.absolutePos(BlockPos.ZERO));
        var modifiers = helper.getLevel().registryAccess()
                .registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        var modifier = modifiers.get(ResourceLocation.fromNamespaceAndPath(
                SkysGrassSlabs.MOD_ID, "grass_slab_smoothing"));
        require(helper, modifier instanceof zone.moddev.mc.skysgrassslabs.world.SmoothingBiomeModifier,
                "registered smoothing biome modifier is missing");

        BiomeInfo.Builder builder = BiomeInfo.Builder.copyOf(
                biome.value().modifiableBiomeInfo().getOriginalBiomeInfo());
        modifier.modify(biome, BiomeModifier.Phase.AFTER_EVERYTHING, builder);
        var vegetation = builder.build().generationSettings().features()
                .get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal());
        require(helper, vegetation.size() > 0, "biome has no vegetation features");
        PlacedFeature first = vegetation.get(0).value();
        require(helper, first.feature().value().feature() instanceof GrassSlabSmoothingFeature,
                "smoother is not the first vegetation feature");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "worldgen001", timeoutTicks = 300)
    public static void smoothingIsBorderSafeAndIdempotent(GameTestHelper helper) {
        ChunkPos owner = new ChunkPos(helper.absolutePos(new BlockPos(1, 2, 1)));
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
            GrassSlabSmoothingFeature feature = new GrassSlabSmoothingFeature(
                    NoneFeatureConfiguration.CODEC);
            FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(
                    Optional.empty(), helper.getLevel(),
                    helper.getLevel().getChunkSource().getGenerator(),
                    RandomSource.create(19780401L),
                    center, NoneFeatureConfiguration.INSTANCE);
            require(helper, feature.place(context), "controlled feature pass made no changes");
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
                boolean secondChanged = feature.place(context);
                int secondCount = countGrassSlabs(helper, owner);
                require(helper, !secondChanged && secondCount == firstCount,
                        "settled second pass changed output: before=" + firstCount
                                + ", after=" + secondCount);
                helper.succeed();
            });
        });
    }

    private static int countGrassSlabs(GameTestHelper helper, ChunkPos chunk) {
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = helper.getLevel().getMinBuildHeight();
                        y < helper.getLevel().getMaxBuildHeight(); y++) {
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
