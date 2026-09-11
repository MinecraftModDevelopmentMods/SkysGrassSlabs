package zone.moddev.mc.skysgrassslabs.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.block.DirtSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.GrassSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.GrassSpread;
import zone.moddev.mc.skysgrassslabs.block.PathSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.TurfBlock;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.init.ModRecipes;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.entity.ai.TurfEatingGoal;
import zone.moddev.mc.skysgrassslabs.event.CommonEvents;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Runtime coverage for save-facing block state, tool, lifecycle, recipe and loot contracts. */
@GameTestHolder(value = SkysGrassSlabs.MOD_ID)
@PrefixGameTestTemplate(false)
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

        BlockState topPath = dirtTop.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false);
        BlockState bottomPath = grassBottom.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false);
        require(helper, topPath != null && topPath.is(ModBlocks.PATH_SLAB.get())
                && topPath.getValue(SlabBlock.TYPE) == SlabType.TOP, "top orientation was lost");
        require(helper, bottomPath != null && bottomPath.is(ModBlocks.PATH_SLAB.get())
                && bottomPath.getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                "bottom orientation was lost");
        require(helper, waterlogged.getToolModifiedState(context,
                ItemAbilities.SHOVEL_FLATTEN, false) == null, "waterlogged dirt flattened");
        BlockState fullPath = doubled.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false);
        require(helper, fullPath != null && fullPath.is(Blocks.DIRT_PATH),
                "double dirt slab did not normalize to vanilla path");

        helper.getLevel().setBlock(pos, dirtTop, Block.UPDATE_ALL);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
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
        path.tick(top, helper.getLevel(), pos, RandomSource.create(1L));
        BlockState decayed = helper.getLevel().getBlockState(pos);
        require(helper, decayed.is(ModBlocks.DIRT_SLAB.get())
                && decayed.getValue(SlabBlock.TYPE) == SlabType.TOP,
                "covered path did not preserve orientation when decaying");

        helper.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        BlockState wetPath = top.setValue(SlabBlock.WATERLOGGED, true);
        helper.getLevel().setBlock(pos, wetPath, 3);
        path.tick(wetPath, helper.getLevel(), pos, RandomSource.create(2L));
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
                    RandomSource.create(2L));
            require(helper,
                    helper.getLevel().getBlockState(grassPos).is(ModBlocks.DIRT_SLAB.get()),
                    "covered grass slab did not decay");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "slabs005")
    public static void recipesTagsAndLootLoad(GameTestHelper helper) {
        require(helper, Items.WHEAT_SEEDS.builtInRegistryHolder().is(Tags.Items.SEEDS),
                "wheat seeds are absent from c:seeds");
        require(helper, Items.BEETROOT_SEEDS.builtInRegistryHolder().is(Tags.Items.SEEDS),
                "beetroot seeds are absent from c:seeds");
        for (String recipe : List.of("dirt_slab", "grass_slab", "grass_block_from_seeds",
                "grass_slab_from_seeds")) {
            require(helper, helper.getLevel().getRecipeManager().byKey(
                    ResourceLocation.fromNamespaceAndPath(SkysGrassSlabs.MOD_ID, recipe)).isPresent(),
                    "missing recipe " + recipe);
        }

        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockState grass = ModBlocks.GRASS_SLAB.get().defaultBlockState();
        helper.getLevel().setBlock(pos, grass, 3);
        List<ItemStack> ordinary = Block.getDrops(grass, helper.getLevel(), pos, null,
                null, new ItemStack(Items.IRON_SHOVEL));
        ItemStack silkTool = new ItemStack(Items.IRON_SHOVEL);
        silkTool.enchant(helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH), 1);
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
                .byKey(ResourceLocation.fromNamespaceAndPath(
                        SkysGrassSlabs.MOD_ID, "grass_slab_from_seeds"))
                .orElseThrow().value();
        require(helper, matchesSeedRecipe(seedRecipe, helper, Items.WHEAT_SEEDS),
                "wheat seeds did not match the grass slab recipe");
        require(helper, matchesSeedRecipe(seedRecipe, helper, Items.BEETROOT_SEEDS),
                "beetroot seeds did not match the grass slab recipe");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "slabs006")
    public static void snowPlantsBonemealAndWaterAreTopAware(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        GrassSlabBlock grass = (GrassSlabBlock) ModBlocks.GRASS_SLAB.get();
        BlockState bottom = grass.defaultBlockState();
        BlockState top = bottom.setValue(SlabBlock.TYPE, SlabType.TOP);

        require(helper, !grass.isValidBonemealTarget(helper.getLevel(), pos, bottom),
                "bottom grass slab accepted bonemeal");
        require(helper, grass.isValidBonemealTarget(helper.getLevel(), pos, top),
                "top grass slab rejected bonemeal");
        require(helper, grass.canSustainPlant(bottom, helper.getLevel(), pos, Direction.UP,
                Blocks.DANDELION.defaultBlockState()).isFalse(),
                "bottom grass slab sustained a plant");
        require(helper, !grass.canSustainPlant(top, helper.getLevel(), pos, Direction.UP,
                Blocks.DANDELION.defaultBlockState()).isFalse(),
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
        BlockState clearGrass = grass.updateShape(snowBlockAppearance, Direction.UP,
                Blocks.AIR.defaultBlockState(), helper.getLevel(), pos, pos.above());
        require(helper, !clearGrass.getValue(GrassSlabBlock.SNOWY),
                "grass slab retained its snowy appearance after snow was removed");
        DirtSlabBlock dirt = (DirtSlabBlock) ModBlocks.DIRT_SLAB.get();
        BlockState snowyDirt = dirt.updateShape(dirt.defaultBlockState(), Direction.NORTH,
                Blocks.SNOW.defaultBlockState(), helper.getLevel(), pos, pos.north());
        require(helper, snowyDirt.getValue(net.minecraft.world.level.block.SnowyDirtBlock.SNOWY),
                "nearby snow did not select the snowy dirt-slab appearance");
        BlockState clearDirt = dirt.updateShape(snowyDirt, Direction.NORTH,
                Blocks.AIR.defaultBlockState(), helper.getLevel(), pos, pos.north());
        require(helper, !clearDirt.getValue(net.minecraft.world.level.block.SnowyDirtBlock.SNOWY),
                "dirt slab retained its snowy appearance after snow was removed");

        BlockState wetGrass = top.setValue(SlabBlock.WATERLOGGED, true);
        helper.getLevel().setBlock(pos, wetGrass, Block.UPDATE_ALL);
        grass.randomTick(wetGrass, helper.getLevel(), pos, RandomSource.create(3L));
        BlockState wetDirt = helper.getLevel().getBlockState(pos);
        require(helper, wetDirt.is(ModBlocks.DIRT_SLAB.get())
                && wetDirt.getValue(SlabBlock.TYPE) == SlabType.TOP
                && wetDirt.getValue(SlabBlock.WATERLOGGED),
                "waterlogged grass did not become orientation-preserving dirt");

        require(helper, ModWorldState.get(helper.getLevel()).schemaVersion() == 1,
                "world schema marker is not version 1");
        require(helper, SkysGrassSlabsConfig.generateGrassSlabs(),
                "fresh common config did not default worldgen to true");
        require(helper, ResourceLocation.fromNamespaceAndPath(SkysGrassSlabs.MOD_ID, "dirt_slab")
                .equals(BuiltInRegistries.BLOCK.getKey(ModBlocks.DIRT_SLAB.get())),
                "dirt slab registry ID changed");
        require(helper, ResourceLocation.fromNamespaceAndPath(SkysGrassSlabs.MOD_ID, "grass_slab")
                .equals(BuiltInRegistries.BLOCK.getKey(ModBlocks.GRASS_SLAB.get())),
                "grass slab registry ID changed");
        require(helper, ResourceLocation.fromNamespaceAndPath(SkysGrassSlabs.MOD_ID, "path_slab")
                .equals(BuiltInRegistries.BLOCK.getKey(ModBlocks.PATH_SLAB.get())),
                "path slab registry ID changed");
        require(helper, BuiltInRegistries.FEATURE.containsKey(
                ResourceLocation.fromNamespaceAndPath(
                        SkysGrassSlabs.MOD_ID, "grass_slab_smoothing")),
                "worldgen feature registry ID changed");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "slabs007")
    public static void turfMatchesPhysicalCarpetAndDropsFromInvalidSoil(GameTestHelper helper) {
        helper.setDayTime(18000);
        TurfBlock turf = (TurfBlock) ModBlocks.TURF.get();
        BlockState state = turf.defaultBlockState();
        BlockPos dirtSupport = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos dirtTurf = dirtSupport.above();

        require(helper, state.getShape(helper.getLevel(), dirtTurf,
                CollisionContext.empty()).bounds().maxY == 1.0D / 16.0D,
                "turf outline is not one pixel high");
        require(helper, state.getCollisionShape(helper.getLevel(), dirtTurf,
                CollisionContext.empty()).bounds().maxY == 1.0D / 16.0D,
                "turf collision is not one pixel high");
        require(helper, !state.hasBlockEntity(), "turf unexpectedly has a block entity");
        require(helper, state.getFlammability(helper.getLevel(), dirtTurf, Direction.UP) == 20
                && state.getFireSpreadSpeed(helper.getLevel(), dirtTurf, Direction.UP) == 60,
                "turf does not match carpet flammability");
        require(helper, !ModBlocks.TURF.get().builtInRegistryHolder().is(BlockTags.WOOL_CARPETS)
                && !ModBlocks.TURF_ITEM.get().builtInRegistryHolder().is(ItemTags.WOOL_CARPETS),
                "turf leaked into wool-carpet tags");

        helper.getLevel().setBlock(dirtSupport, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack turfStack = new ItemStack(ModBlocks.TURF_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, turfStack);
        turfStack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(dirtSupport), Direction.UP,
                        dirtSupport, false)));
        require(helper, helper.getLevel().getBlockState(dirtTurf).is(ModBlocks.TURF.get()),
                "turf did not place on full dirt");
        require(helper, turfStack.isEmpty(), "normal turf placement did not consume one item");

        BlockPos stoneSupport = helper.absolutePos(new BlockPos(3, 1, 1));
        BlockPos stoneTurf = stoneSupport.above();
        helper.getLevel().setBlock(stoneSupport, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        ItemStack stoneStack = new ItemStack(ModBlocks.TURF_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stoneStack);
        stoneStack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(stoneSupport), Direction.UP,
                        stoneSupport, false)));
        require(helper, helper.getLevel().getBlockState(stoneTurf).is(ModBlocks.TURF.get()),
                "turf did not initially place on a full non-dirt block");
        turf.randomTick(state, helper.getLevel(), stoneTurf, RandomSource.create(7L));
        require(helper, helper.getLevel().getBlockState(stoneTurf).isAir(),
                "invalid-support turf survived its random tick");
        helper.assertItemEntityPresent(ModBlocks.TURF_ITEM.get(), new BlockPos(3, 2, 1), 1.5D);

        BlockPos slabSupport = helper.absolutePos(new BlockPos(5, 1, 1));
        BlockPos slabTurf = slabSupport.above();
        helper.getLevel().setBlock(slabSupport, Blocks.STONE_SLAB.defaultBlockState(),
                Block.UPDATE_ALL);
        ItemStack rejected = new ItemStack(ModBlocks.TURF_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rejected);
        rejected.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(slabSupport), Direction.UP,
                        slabSupport, false)));
        require(helper, helper.getLevel().getBlockState(slabTurf).isAir()
                && rejected.getCount() == 1, "turf placed on a partial support");

        helper.getLevel().setBlock(dirtTurf, state, Block.UPDATE_ALL);
        helper.getLevel().setBlock(dirtTurf.above(), Blocks.STONE.defaultBlockState(),
                Block.UPDATE_ALL);
        turf.randomTick(state, helper.getLevel(), dirtTurf, RandomSource.create(8L));
        require(helper, helper.getLevel().getBlockState(dirtTurf).is(ModBlocks.TURF.get()),
                "covered turf incorrectly gained a decay stage");

        BlockPos lightCell = dirtTurf.above();
        BlockPos lowLightTarget = dirtTurf.east().below();
        helper.getLevel().setBlock(lightCell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(lightCell.above(), Blocks.STONE.defaultBlockState(),
                Block.UPDATE_ALL);
        helper.getLevel().setBlock(lightCell.north(), Blocks.STONE.defaultBlockState(),
                Block.UPDATE_ALL);
        helper.getLevel().setBlock(lightCell.south(), Blocks.STONE.defaultBlockState(),
                Block.UPDATE_ALL);
        helper.getLevel().setBlock(lightCell.east(), Blocks.STONE.defaultBlockState(),
                Block.UPDATE_ALL);
        helper.getLevel().setBlock(lightCell.west(), Blocks.STONE.defaultBlockState(),
                Block.UPDATE_ALL);
        helper.getLevel().setBlock(lowLightTarget, Blocks.DIRT.defaultBlockState(),
                Block.UPDATE_ALL);
        helper.runAfterDelay(4, () -> {
            require(helper, helper.getLevel().getMaxLocalRawBrightness(lightCell) < 9,
                    "low-light turf fixture remained bright");
            turf.randomTick(state, helper.getLevel(), dirtTurf,
                    new FixedRandom(2, 2, 1));
            require(helper, helper.getLevel().getBlockState(dirtTurf).is(ModBlocks.TURF.get())
                    && helper.getLevel().getBlockState(lowLightTarget).is(Blocks.DIRT),
                    "low-light turf decayed or spread; turf="
                            + helper.getLevel().getBlockState(dirtTurf) + ", support="
                            + helper.getLevel().getBlockState(dirtSupport) + ", target="
                            + helper.getLevel().getBlockState(lowLightTarget) + ", brightness="
                            + helper.getLevel().getMaxLocalRawBrightness(lightCell));

            helper.getLevel().setBlock(dirtSupport, Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL);
            require(helper, helper.getLevel().getBlockState(dirtTurf).isAir(),
                    "turf did not use carpet support-loss behaviour");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "slabs008")
    public static void turfPlacementGreensDryDirtSlabs(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        BlockPos bottomPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos topPos = helper.absolutePos(new BlockPos(2, 2, 1));
        BlockPos doublePos = helper.absolutePos(new BlockPos(3, 2, 1));
        BlockPos wetPos = helper.absolutePos(new BlockPos(4, 2, 1));
        BlockState dirt = ModBlocks.DIRT_SLAB.get().defaultBlockState();

        helper.getLevel().setBlock(bottomPos, dirt, Block.UPDATE_ALL);
        ItemStack bottomTurf = useTurfOn(player, bottomPos);
        require(helper, helper.getLevel().getBlockState(bottomPos).is(ModBlocks.GRASS_SLAB.get())
                && helper.getLevel().getBlockState(bottomPos).getValue(SlabBlock.TYPE)
                        == SlabType.BOTTOM
                && bottomTurf.isEmpty(), "bottom dirt slab turf conversion failed");

        helper.getLevel().setBlock(topPos, dirt.setValue(SlabBlock.TYPE, SlabType.TOP),
                Block.UPDATE_ALL);
        ItemStack topTurf = useTurfOn(player, topPos);
        require(helper, helper.getLevel().getBlockState(topPos).is(ModBlocks.GRASS_SLAB.get())
                && helper.getLevel().getBlockState(topPos).getValue(SlabBlock.TYPE) == SlabType.TOP
                && topTurf.isEmpty(), "top dirt slab turf conversion failed");

        helper.getLevel().setBlock(doublePos, dirt.setValue(SlabBlock.TYPE, SlabType.DOUBLE),
                Block.UPDATE_ALL);
        ItemStack doubleTurf = useTurfOn(player, doublePos);
        require(helper, helper.getLevel().getBlockState(doublePos).is(Blocks.GRASS_BLOCK)
                && doubleTurf.isEmpty(), "double dirt slab did not normalize to grass");

        BlockState wet = dirt.setValue(SlabBlock.WATERLOGGED, true);
        helper.getLevel().setBlock(wetPos, wet, Block.UPDATE_ALL);
        ItemStack wetTurf = useTurfOn(player, wetPos);
        require(helper, helper.getLevel().getBlockState(wetPos).equals(wet)
                && wetTurf.getCount() == 1, "waterlogged dirt slab accepted turf");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "slabs009")
    public static void turfSpreadsOutwardAndActsAsSlabSource(GameTestHelper helper) {
        helper.setDayTime(6000);
        BlockPos support = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos turfPos = support.above();
        BlockPos target = turfPos.east().below();
        BlockPos glow = turfPos.west().above();
        TurfBlock turf = (TurfBlock) ModBlocks.TURF.get();
        BlockState turfState = turf.defaultBlockState();
        helper.getLevel().setBlock(support, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(turfPos, turfState, Block.UPDATE_ALL);
        helper.getLevel().setBlock(glow, Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);

        BlockPos slabTarget = turfPos.west().below();
        BlockPos sourceTurf = turfPos;
        helper.getLevel().setBlock(slabTarget.west().above(),
                Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);

        helper.runAfterDelay(20, () -> {
            turf.randomTick(turfState, helper.getLevel(), turfPos,
                    new FixedRandom(1, 2, 1));
            require(helper, helper.getLevel().getBlockState(support).is(Blocks.DIRT),
                    "turf converted its own support");

            helper.getLevel().setBlock(target, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            turf.randomTick(turfState, helper.getLevel(), turfPos,
                    new FixedRandom(2, 2, 1));
            require(helper, helper.getLevel().getBlockState(target).is(Blocks.GRASS_BLOCK),
                    "turf did not spread to vanilla dirt");

            for (SlabType type : SlabType.values()) {
                BlockState dirtSlab = ModBlocks.DIRT_SLAB.get().defaultBlockState()
                        .setValue(SlabBlock.TYPE, type);
                helper.getLevel().setBlock(target, dirtSlab, Block.UPDATE_ALL);
                turf.randomTick(turfState, helper.getLevel(), turfPos,
                        new FixedRandom(2, 2, 1));
                BlockState result = helper.getLevel().getBlockState(target);
                if (type == SlabType.DOUBLE) {
                    require(helper, result.is(Blocks.GRASS_BLOCK),
                            "turf did not normalize a double dirt slab");
                } else {
                    require(helper, result.is(ModBlocks.GRASS_SLAB.get())
                            && result.getValue(SlabBlock.TYPE) == type,
                            "turf lost dirt-slab orientation " + type + ": " + result);
                }
            }

            require(helper, helper.getLevel().getBlockState(sourceTurf).is(ModBlocks.TURF.get()),
                    "turf source disappeared before dirt-slab tick: "
                            + helper.getLevel().getBlockState(sourceTurf));
            require(helper, helper.getLevel().getBlockState(sourceTurf.below()).is(Blocks.DIRT),
                    "turf source lost exact dirt support: "
                            + helper.getLevel().getBlockState(sourceTurf.below()));
            require(helper, helper.getLevel().isAreaLoaded(slabTarget, 3),
                    "dirt slab test area is not loaded");
            require(helper, helper.getLevel().getMaxLocalRawBrightness(slabTarget.above()) >= 9,
                    "dirt slab target is too dark: "
                            + helper.getLevel().getMaxLocalRawBrightness(slabTarget.above()));
            helper.getLevel().setBlock(slabTarget,
                    ModBlocks.DIRT_SLAB.get().defaultBlockState(), Block.UPDATE_ALL);
            ((DirtSlabBlock) ModBlocks.DIRT_SLAB.get()).randomTick(
                    helper.getLevel().getBlockState(slabTarget), helper.getLevel(), slabTarget,
                    new FixedRandom(2, 4, 1));
            require(helper, helper.getLevel().getBlockState(slabTarget)
                    .is(ModBlocks.GRASS_SLAB.get()), "dirt slab did not recognize turf as grass");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "slabs010")
    public static void turfRecipeReturnsSoilAndUnchangedShovel(GameTestHelper helper) {
        CraftingRecipe recipe = (CraftingRecipe) helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(
                        SkysGrassSlabs.MOD_ID, "turf")).orElseThrow().value();
        require(helper, recipe.getSerializer() == ModRecipes.TURF_CUTTING.get(),
                "turf recipe serializer changed");
        ItemStack iron = new ItemStack(Items.IRON_SHOVEL);
        iron.setDamageValue(7);
        CompoundTag customData = new CompoundTag();
        customData.putString("turf_test", "preserved");
        iron.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        CraftingInput blockGrid = craftingGrid(2, 2,
                new ItemStack(Blocks.GRASS_BLOCK), iron);
        require(helper, recipe.matches(blockGrid, helper.getLevel())
                && recipe.assemble(blockGrid, helper.getLevel().registryAccess())
                        .is(ModBlocks.TURF_ITEM.get()),
                "grass block and shovel did not craft turf in a 2x2 grid");
        NonNullList<ItemStack> blockRemainders = recipe.getRemainingItems(blockGrid);
        require(helper, blockRemainders.get(0).is(Blocks.DIRT.asItem()),
                "grass block did not return dirt");
        require(helper, blockRemainders.get(1).is(Items.IRON_SHOVEL)
                && blockRemainders.get(1).getDamageValue() == 7
                && blockRemainders.get(1).getOrDefault(
                        DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(customData),
                "shovel remainder lost durability or custom data");

        CraftingInput slabGrid = craftingGrid(2, 2,
                new ItemStack(ModBlocks.GRASS_SLAB_ITEM.get()), ItemStack.EMPTY,
                ItemStack.EMPTY, new ItemStack(Items.DIAMOND_SHOVEL));
        require(helper, recipe.matches(slabGrid, helper.getLevel()),
                "grass slab and second vanilla shovel did not craft turf");
        NonNullList<ItemStack> slabRemainders = recipe.getRemainingItems(slabGrid);
        require(helper, slabRemainders.get(0).is(ModBlocks.DIRT_SLAB_ITEM.get())
                && slabRemainders.get(3).is(Items.DIAMOND_SHOVEL),
                "grass slab recipe remainders are incorrect");

        CraftingInput netheriteGrid = craftingGrid(2, 2,
                new ItemStack(Blocks.GRASS_BLOCK), new ItemStack(Items.NETHERITE_SHOVEL));
        require(helper, recipe.matches(netheriteGrid, helper.getLevel())
                && recipe.getRemainingItems(netheriteGrid).get(1).is(Items.NETHERITE_SHOVEL),
                "third compatible shovel did not match and return unchanged");
        CraftingInput extraGrid = craftingGrid(2, 2,
                new ItemStack(Blocks.GRASS_BLOCK), new ItemStack(Items.NETHERITE_SHOVEL),
                new ItemStack(Items.WHEAT_SEEDS));
        require(helper, !recipe.matches(extraGrid, helper.getLevel()),
                "turf recipe accepted an extra ingredient");
        CraftingInput invalidGrid = craftingGrid(2, 2,
                new ItemStack(Blocks.GRASS_BLOCK), new ItemStack(Items.STICK));
        require(helper, !recipe.matches(invalidGrid, helper.getLevel()),
                "turf recipe accepted a non-shovel");
        require(helper, ResourceLocation.fromNamespaceAndPath(SkysGrassSlabs.MOD_ID, "turf")
                .equals(BuiltInRegistries.BLOCK.getKey(ModBlocks.TURF.get())),
                "turf block registry ID changed");
        require(helper, ResourceLocation.fromNamespaceAndPath(SkysGrassSlabs.MOD_ID, "turf")
                .equals(BuiltInRegistries.ITEM.getKey(ModBlocks.TURF_ITEM.get())),
                "turf item registry ID changed");
        require(helper, ResourceLocation.fromNamespaceAndPath(SkysGrassSlabs.MOD_ID, "turf_cutting")
                .equals(BuiltInRegistries.RECIPE_SERIALIZER.getKey(ModRecipes.TURF_CUTTING.get())),
                "turf recipe serializer ID changed");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "slabs011")
    public static void grassCoveringsDirtifySupportAndDoNotAttractGrass(GameTestHelper helper) {
        BlockPos grassSupport = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos grassSlab = grassSupport.above();
        helper.getLevel().setBlock(grassSupport, Blocks.GRASS_BLOCK.defaultBlockState(),
                Block.UPDATE_ALL);
        helper.getLevel().setBlock(grassSlab, ModBlocks.GRASS_SLAB.get().defaultBlockState(),
                Block.UPDATE_ALL);
        require(helper, helper.getLevel().getBlockState(grassSupport).is(Blocks.DIRT),
                "grass slab did not dirtify its vanilla grass support");
        require(helper, !GrassSpread.growTarget(helper.getLevel(), grassSupport),
                "grass spread reached dirt beneath a grass slab");

        BlockPos turfSupport = helper.absolutePos(new BlockPos(3, 1, 1));
        BlockPos turf = turfSupport.above();
        helper.getLevel().setBlock(turfSupport, Blocks.GRASS_BLOCK.defaultBlockState(),
                Block.UPDATE_ALL);
        helper.getLevel().setBlock(turf, ModBlocks.TURF.get().defaultBlockState(),
                Block.UPDATE_ALL);
        require(helper, helper.getLevel().getBlockState(turfSupport).is(Blocks.DIRT),
                "turf did not dirtify its vanilla grass support");
        require(helper, !GrassSpread.growTarget(helper.getLevel(), turfSupport),
                "grass spread reached dirt beneath turf");

        BlockPos fence = helper.absolutePos(new BlockPos(5, 1, 1));
        helper.getLevel().setBlock(fence.west(), Blocks.STONE.defaultBlockState(),
                Block.UPDATE_ALL);
        helper.getLevel().setBlock(fence.east(), ModBlocks.TURF.get().defaultBlockState(),
                Block.UPDATE_ALL);
        BlockState fenceState = Blocks.OAK_FENCE.getStateForPlacement(
                placeContext(helper, fence, new ItemStack(Items.OAK_FENCE)));
        require(helper, fenceState != null, "fence placement state was unavailable");
        require(helper, fenceState.getValue(FenceBlock.WEST),
                "fence control side did not connect to a full block");
        require(helper, !fenceState.getValue(FenceBlock.EAST),
                "fence connected to turf as though it were a full block");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "slabs012")
    public static void sheepEatTurfOnceAndRespectMobGriefing(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos turf = support.above();
        helper.getLevel().setBlock(support, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(turf, ModBlocks.TURF.get().defaultBlockState(),
                Block.UPDATE_ALL);

        Sheep sheep = EntityType.SHEEP.create(helper.getLevel());
        require(helper, sheep != null, "could not create sheep fixture");
        sheep.setPos(turf.getX() + 0.5D, turf.getY(), turf.getZ() + 0.5D);
        sheep.setSheared(true);
        CommonEvents.addTurfEatingGoal(new EntityJoinLevelEvent(sheep, helper.getLevel()));
        CommonEvents.addTurfEatingGoal(new EntityJoinLevelEvent(sheep, helper.getLevel()));
        long goalCount = sheep.goalSelector.getAvailableGoals().stream()
                .filter(goal -> goal.getGoal() instanceof TurfEatingGoal).count();
        require(helper, goalCount == 1, "sheep received duplicate turf eating goals");

        TurfEatingGoal eating = new TurfEatingGoal(sheep);
        eating.start();
        for (int tick = 0; tick < 36; ++tick) {
            eating.tick();
        }
        require(helper, helper.getLevel().getBlockState(turf).isAir(),
                "sheep did not consume turf when mobGriefing was enabled");
        require(helper, !sheep.isSheared(), "eating turf did not regrow sheep wool");

        helper.getLevel().setBlock(turf, ModBlocks.TURF.get().defaultBlockState(),
                Block.UPDATE_ALL);
        sheep.setSheared(true);
        GameRules.BooleanValue mobGriefing = helper.getLevel().getGameRules()
                .getRule(GameRules.RULE_MOBGRIEFING);
        mobGriefing.set(false, helper.getLevel().getServer());
        TurfEatingGoal protectedEating = new TurfEatingGoal(sheep);
        protectedEating.start();
        for (int tick = 0; tick < 36; ++tick) {
            protectedEating.tick();
        }
        mobGriefing.set(true, helper.getLevel().getServer());
        require(helper, helper.getLevel().getBlockState(turf).is(ModBlocks.TURF.get()),
                "sheep destroyed turf while mobGriefing was disabled");
        require(helper, !sheep.isSheared(),
                "mobGriefing disabled the vanilla eating benefit");
        helper.succeed();
    }

    private static boolean matchesSeedRecipe(CraftingRecipe recipe, GameTestHelper helper,
            net.minecraft.world.item.Item seed) {
        CraftingInput grid = craftingGrid(2, 2,
                new ItemStack(ModBlocks.DIRT_SLAB_ITEM.get()), new ItemStack(seed));
        return recipe.matches(grid, helper.getLevel())
                && recipe.assemble(grid, helper.getLevel().registryAccess())
                        .is(ModBlocks.GRASS_SLAB_ITEM.get());
    }

    private static CraftingInput craftingGrid(int width, int height, ItemStack... stacks) {
        NonNullList<ItemStack> items = NonNullList.withSize(width * height, ItemStack.EMPTY);
        for (int slot = 0; slot < stacks.length; ++slot) {
            items.set(slot, stacks[slot]);
        }
        return CraftingInput.of(width, height, items);
    }

    private static ItemStack useTurfOn(Player player, BlockPos pos) {
        ItemStack turf = new ItemStack(ModBlocks.TURF_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, turf);
        turf.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        return turf;
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

    private static final class FixedRandom extends LegacyRandomSource {
        private final int[] values;
        private int index;

        FixedRandom(int... values) {
            super(0L);
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(values[index++ % values.length], bound);
        }
    }
}
