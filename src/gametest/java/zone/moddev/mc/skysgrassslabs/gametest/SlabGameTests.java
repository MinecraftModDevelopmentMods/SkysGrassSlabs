package zone.moddev.mc.skysgrassslabs.gametest;

import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.block.DirtSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.GrassSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.PathSlabBlock;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.config.BetaConfig;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Runtime coverage for save-facing block state, tool, lifecycle, recipe and loot contracts. */
@PrefixGameTestTemplate(false)
@GameTestHolder(SkysGrassSlabs.MOD_ID)
public final class SlabGameTests {
    private static final String EMPTY = "empty";

    private SlabGameTests() {
    }

    @GameTest(template = EMPTY, batch = "slabs001")
    public static void shovelFlatteningPreservesOrientation(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        UseOnContext context = context(helper, pos, new ItemStack(Items.IRON_SHOVEL));
        BlockState dirtTop = ModBlocks.DIRT_SLAB.get().defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState grassBottom = ModBlocks.GRASS_SLAB.get().defaultBlockState();
        BlockState waterlogged = dirtTop.setValue(SlabBlock.WATERLOGGED, true);
        BlockState doubled = dirtTop.setValue(SlabBlock.TYPE, SlabType.DOUBLE)
                .setValue(SlabBlock.WATERLOGGED, false);

        BlockState topPath = dirtTop.getToolModifiedState(context, ToolActions.SHOVEL_FLATTEN, false);
        BlockState bottomPath = grassBottom.getToolModifiedState(context, ToolActions.SHOVEL_FLATTEN, false);
        require(helper, topPath != null && topPath.is(ModBlocks.PATH_SLAB.get())
                && topPath.getValue(SlabBlock.TYPE) == SlabType.TOP, "top orientation was lost");
        require(helper, bottomPath != null && bottomPath.is(ModBlocks.PATH_SLAB.get())
                && bottomPath.getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                "bottom orientation was lost");
        require(helper, waterlogged.getToolModifiedState(context,
                ToolActions.SHOVEL_FLATTEN, false) == null, "waterlogged dirt flattened");
        BlockState fullPath = doubled.getToolModifiedState(context, ToolActions.SHOVEL_FLATTEN, false);
        require(helper, fullPath != null && fullPath.is(Blocks.DIRT_PATH),
                "double dirt slab did not normalize to vanilla path");

        helper.getLevel().setBlock(pos, dirtTop, Block.UPDATE_ALL);
        Player player = helper.makeMockPlayer();
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        shovel.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        require(helper, helper.getLevel().getBlockState(pos).is(ModBlocks.PATH_SLAB.get())
                && helper.getLevel().getBlockState(pos).getValue(SlabBlock.TYPE) == SlabType.TOP,
                "vanilla shovel use did not create a top path slab");
        require(helper, shovel.getDamageValue() == 1, "shovel durability was not consumed");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "slabs002")
    public static void stackingNormalizesGrassAndPath(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(pos, ModBlocks.GRASS_SLAB.get().defaultBlockState(), 3);
        BlockState grassResult = ((GrassSlabBlock) ModBlocks.GRASS_SLAB.get())
                .getStateForPlacement(placeContext(helper, pos,
                        new ItemStack(ModBlocks.GRASS_SLAB_ITEM.get())));
        require(helper, grassResult != null && grassResult.is(Blocks.GRASS_BLOCK),
                "two grass slabs did not normalize");

        helper.getLevel().setBlock(pos, ModBlocks.PATH_SLAB.get().defaultBlockState(), 3);
        BlockState pathResult = ((PathSlabBlock) ModBlocks.PATH_SLAB.get())
                .getStateForPlacement(placeContext(helper, pos,
                        new ItemStack(ModBlocks.PATH_SLAB_ITEM.get())));
        require(helper, pathResult != null && pathResult.is(Blocks.DIRT_PATH),
                "two path slabs did not normalize");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "slabs003")
    public static void pathProfilesAndDecayMatchContract(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        PathSlabBlock path = (PathSlabBlock) ModBlocks.PATH_SLAB.get();
        BlockState bottom = path.defaultBlockState();
        BlockState top = bottom.setValue(SlabBlock.TYPE, SlabType.TOP);
        double bottomMax = path.getShape(bottom, helper.getLevel(), pos, null).bounds().maxY;
        double topMin = path.getShape(top, helper.getLevel(), pos, null).bounds().minY;
        double topMax = path.getShape(top, helper.getLevel(), pos, null).bounds().maxY;
        require(helper, bottomMax == 7.0D / 16.0D, "bottom path is not seven pixels high");
        require(helper, topMin == 8.0D / 16.0D && topMax == 15.0D / 16.0D,
                "top path profile is incorrect");

        helper.getLevel().setBlock(pos, top, 3);
        helper.getLevel().setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        path.tick(top, helper.getLevel(), pos, new Random(1L));
        BlockState decayed = helper.getLevel().getBlockState(pos);
        require(helper, decayed.is(ModBlocks.DIRT_SLAB.get())
                && decayed.getValue(SlabBlock.TYPE) == SlabType.TOP,
                "covered path did not preserve orientation when decaying");

        helper.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        BlockState wetPath = top.setValue(SlabBlock.WATERLOGGED, true);
        helper.getLevel().setBlock(pos, wetPath, 3);
        path.tick(wetPath, helper.getLevel(), pos, new Random(2L));
        BlockState wetDirt = helper.getLevel().getBlockState(pos);
        require(helper, wetDirt.is(ModBlocks.DIRT_SLAB.get())
                && wetDirt.getValue(SlabBlock.TYPE) == SlabType.TOP
                && wetDirt.getValue(SlabBlock.WATERLOGGED),
                "waterlogged path did not become matching waterlogged dirt");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "slabs004")
    public static void grassLifecycleInteroperatesWithVanilla(GameTestHelper helper) {
        helper.setDayTime(6000);
        BlockPos dirtPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos sourcePos = dirtPos.east();
        helper.getLevel().setBlock(dirtPos.west().above(), Blocks.GLOWSTONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(dirtPos, ModBlocks.DIRT_SLAB.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(sourcePos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        BlockPos grassPos = helper.absolutePos(new BlockPos(1, 2, 3));
        BlockPos vanillaDirt = grassPos.east();
        helper.getLevel().setBlock(grassPos.west().above(), Blocks.GLOWSTONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(grassPos, ModBlocks.GRASS_SLAB.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(vanillaDirt, Blocks.DIRT.defaultBlockState(), 3);

        helper.runAfterDelay(20, () -> {
            ((DirtSlabBlock) ModBlocks.DIRT_SLAB.get()).randomTick(
                    helper.getLevel().getBlockState(dirtPos), helper.getLevel(), dirtPos,
                    new FixedRandom(2, 3, 1));
            require(helper,
                    helper.getLevel().getBlockState(dirtPos).is(ModBlocks.GRASS_SLAB.get()),
                    "vanilla grass did not reach dirt slab; areaLoaded="
                            + helper.getLevel().isAreaLoaded(dirtPos, 3) + ", brightness="
                            + helper.getLevel().getMaxLocalRawBrightness(dirtPos.above())
                            + ", source=" + helper.getLevel().getBlockState(sourcePos)
                            + ", target=" + helper.getLevel().getBlockState(dirtPos));

            ((GrassSlabBlock) ModBlocks.GRASS_SLAB.get()).randomTick(
                    helper.getLevel().getBlockState(grassPos), helper.getLevel(), grassPos,
                    new FixedRandom(2, 3, 1));
            require(helper, helper.getLevel().getBlockState(vanillaDirt).is(Blocks.GRASS_BLOCK),
                    "grass slab did not reach vanilla dirt");

            helper.getLevel().setBlock(grassPos.above(), Blocks.STONE.defaultBlockState(), 3);
            ((GrassSlabBlock) ModBlocks.GRASS_SLAB.get()).randomTick(
                    helper.getLevel().getBlockState(grassPos), helper.getLevel(), grassPos,
                    new Random(2L));
            require(helper,
                    helper.getLevel().getBlockState(grassPos).is(ModBlocks.DIRT_SLAB.get()),
                    "covered grass slab did not decay");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "slabs005")
    public static void recipesTagsAndLootLoad(GameTestHelper helper) {
        require(helper, Items.WHEAT_SEEDS.builtInRegistryHolder().is(Tags.Items.SEEDS),
                "wheat seeds are absent from forge:seeds");
        require(helper, Items.BEETROOT_SEEDS.builtInRegistryHolder().is(Tags.Items.SEEDS),
                "beetroot seeds are absent from forge:seeds");
        for (String recipe : List.of("dirt_slab", "grass_slab", "grass_block_from_seeds",
                "grass_slab_from_seeds")) {
            require(helper, helper.getLevel().getRecipeManager().byKey(
                    new ResourceLocation(SkysGrassSlabs.MOD_ID, recipe)).isPresent(),
                    "missing recipe " + recipe);
        }

        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockState grass = ModBlocks.GRASS_SLAB.get().defaultBlockState();
        helper.getLevel().setBlock(pos, grass, 3);
        List<ItemStack> ordinary = Block.getDrops(grass, helper.getLevel(), pos, null,
                null, new ItemStack(Items.IRON_SHOVEL));
        ItemStack silkTool = new ItemStack(Items.IRON_SHOVEL);
        silkTool.enchant(Enchantments.SILK_TOUCH, 1);
        List<ItemStack> silk = Block.getDrops(grass, helper.getLevel(), pos, null, null, silkTool);
        require(helper, ordinary.size() == 1 && ordinary.get(0).is(ModBlocks.DIRT_SLAB_ITEM.get()),
                "ordinary grass slab drop is incorrect");
        require(helper, silk.size() == 1 && silk.get(0).is(ModBlocks.GRASS_SLAB_ITEM.get()),
                "Silk Touch grass slab drop is incorrect");

        BlockState path = ModBlocks.PATH_SLAB.get().defaultBlockState();
        helper.getLevel().setBlock(pos, path, 3);
        List<ItemStack> pathDrops = Block.getDrops(path, helper.getLevel(), pos, null, null, silkTool);
        require(helper, pathDrops.size() == 1
                && pathDrops.get(0).is(ModBlocks.DIRT_SLAB_ITEM.get()),
                "path slab did not drop dirt slab under Silk Touch");

        BlockState doubleDirt = ModBlocks.DIRT_SLAB.get().defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.DOUBLE);
        helper.getLevel().setBlock(pos, doubleDirt, 3);
        List<ItemStack> doubleDrops = Block.getDrops(doubleDirt, helper.getLevel(), pos, null,
                null, new ItemStack(Items.IRON_SHOVEL));
        require(helper, doubleDrops.size() == 1
                && doubleDrops.get(0).is(ModBlocks.DIRT_SLAB_ITEM.get())
                && doubleDrops.get(0).getCount() == 2,
                "double dirt slab did not drop two slabs");

        CraftingRecipe seedRecipe = (CraftingRecipe) helper.getLevel().getRecipeManager()
                .byKey(new ResourceLocation(SkysGrassSlabs.MOD_ID, "grass_slab_from_seeds"))
                .orElseThrow();
        CraftingContainer grid = new CraftingContainer(new AbstractContainerMenu(null, -1) {
            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        }, 2, 2);
        require(helper, matchesSeedRecipe(grid, seedRecipe, helper, Items.WHEAT_SEEDS),
                "wheat seeds did not match the grass slab recipe");
        require(helper, matchesSeedRecipe(grid, seedRecipe, helper, Items.BEETROOT_SEEDS),
                "beetroot seeds did not match the grass slab recipe");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "slabs006")
    public static void snowPlantsBonemealAndWaterAreTopAware(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        GrassSlabBlock grass = (GrassSlabBlock) ModBlocks.GRASS_SLAB.get();
        BlockState bottom = grass.defaultBlockState();
        BlockState top = bottom.setValue(SlabBlock.TYPE, SlabType.TOP);

        require(helper, !grass.isValidBonemealTarget(helper.getLevel(), pos, bottom, false),
                "bottom grass slab accepted bonemeal");
        require(helper, grass.isValidBonemealTarget(helper.getLevel(), pos, top, false),
                "top grass slab rejected bonemeal");
        require(helper, !grass.canSustainPlant(bottom, helper.getLevel(), pos, Direction.UP,
                (net.minecraftforge.common.IPlantable) Blocks.DANDELION),
                "bottom grass slab sustained a plant");
        require(helper, grass.canSustainPlant(top, helper.getLevel(), pos, Direction.UP,
                (net.minecraftforge.common.IPlantable) Blocks.DANDELION),
                "top grass slab rejected a plant");

        helper.getLevel().setBlock(pos, top, Block.UPDATE_ALL);
        require(helper, Blocks.SNOW.defaultBlockState().canSurvive(helper.getLevel(), pos.above()),
                "snow did not survive on a top grass slab");
        helper.getLevel().setBlock(pos, bottom, Block.UPDATE_ALL);
        require(helper, !Blocks.SNOW.defaultBlockState().canSurvive(helper.getLevel(), pos.above()),
                "snow survived on a bottom grass slab");
        BlockState snowBlockAppearance = grass.updateShape(top, Direction.UP,
                Blocks.SNOW_BLOCK.defaultBlockState(), helper.getLevel(), pos, pos.above());
        require(helper, snowBlockAppearance.getValue(GrassSlabBlock.SNOWY),
                "snow block did not select the snowy grass-slab appearance");

        BlockState wetGrass = top.setValue(SlabBlock.WATERLOGGED, true);
        helper.getLevel().setBlock(pos, wetGrass, Block.UPDATE_ALL);
        grass.randomTick(wetGrass, helper.getLevel(), pos, new Random(3L));
        BlockState wetDirt = helper.getLevel().getBlockState(pos);
        require(helper, wetDirt.is(ModBlocks.DIRT_SLAB.get())
                && wetDirt.getValue(SlabBlock.TYPE) == SlabType.TOP
                && wetDirt.getValue(SlabBlock.WATERLOGGED),
                "waterlogged grass did not become orientation-preserving dirt");

        require(helper, ModWorldState.get(helper.getLevel()).schemaVersion() == 1,
                "world schema marker is not version 1");
        require(helper, BetaConfig.GENERATE_GRASS_SLABS.get(),
                "fresh common config did not default worldgen to true");
        require(helper, new ResourceLocation(SkysGrassSlabs.MOD_ID, "dirt_slab")
                .equals(ForgeRegistries.BLOCKS.getKey(ModBlocks.DIRT_SLAB.get())),
                "dirt slab registry ID changed");
        require(helper, new ResourceLocation(SkysGrassSlabs.MOD_ID, "grass_slab")
                .equals(ForgeRegistries.BLOCKS.getKey(ModBlocks.GRASS_SLAB.get())),
                "grass slab registry ID changed");
        require(helper, new ResourceLocation(SkysGrassSlabs.MOD_ID, "path_slab")
                .equals(ForgeRegistries.BLOCKS.getKey(ModBlocks.PATH_SLAB.get())),
                "path slab registry ID changed");
        require(helper, ForgeRegistries.FEATURES.containsKey(
                new ResourceLocation(SkysGrassSlabs.MOD_ID, "grass_slab_smoothing")),
                "worldgen feature registry ID changed");
        helper.succeed();
    }

    private static boolean matchesSeedRecipe(CraftingContainer grid, CraftingRecipe recipe,
            GameTestHelper helper, net.minecraft.world.item.Item seed) {
        grid.clearContent();
        grid.setItem(0, new ItemStack(ModBlocks.DIRT_SLAB_ITEM.get()));
        grid.setItem(1, new ItemStack(seed));
        return recipe.matches(grid, helper.getLevel())
                && recipe.assemble(grid).is(ModBlocks.GRASS_SLAB_ITEM.get());
    }

    private static UseOnContext context(GameTestHelper helper, BlockPos pos, ItemStack stack) {
        return new UseOnContext(helper.getLevel(), null, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static BlockPlaceContext placeContext(GameTestHelper helper, BlockPos pos,
            ItemStack stack) {
        return new BlockPlaceContext(helper.getLevel(), null, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static final class FixedRandom extends Random {
        private static final long serialVersionUID = 1L;
        private final int[] values;
        private int index;

        FixedRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(values[index++ % values.length], bound);
        }
    }
}
