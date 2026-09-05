package zone.moddev.mc.skysgrassslabs.integrationtest;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmlserverevents.FMLServerAboutToStartEvent;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.moddev.mc.skysgrassslabs.block.GrassSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.GrassSpread;
import zone.moddev.mc.skysgrassslabs.block.PathSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.TurfBlock;
import zone.moddev.mc.skysgrassslabs.entity.ai.TurfEatingAI;
import zone.moddev.mc.skysgrassslabs.event.CommonEvents;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.recipe.TurfCuttingRecipe;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Build-only Forge runtime probe. This source set is excluded from release jars. */
@Mod(IntegrationTestMod.MOD_ID)
public final class IntegrationTestMod {
    public static final String MOD_ID = "skysgrassslabsintegrationtest";
    private static final String PHASE_PROPERTY = "skysgrassslabs.integrationPhase";
    private static final String MARKER_NAME = "skysgrassslabs-integration.properties";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BlockPos ORIGIN = new BlockPos(8, 200, 8);
    private MinecraftServer pendingServer;
    private int pendingTicks;

    public IntegrationTestMod() {
        MinecraftForge.EVENT_BUS.addListener(this::serverAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
    }

    private void serverAboutToStart(FMLServerAboutToStartEvent event) {
        String phase = System.getProperty(PHASE_PROPERTY, "fresh").trim();
        if (!phase.startsWith("forward-sylvester")) return;
        File levelDat = event.getServer().getWorldPath(LevelResource.LEVEL_DATA_FILE).toFile();
        File temporary = new File(levelDat.getParentFile(), "level.dat.sky-test");
        try (InputStream input = Files.newInputStream(levelDat.toPath())) {
            CompoundTag root = NbtIo.readCompressed(input);
            CompoundTag data = root.getCompound("Data");
            // The audit scans the saved Sylvester chunks directly. A distant test-only
            // spawn keeps Forge from trying to instantiate unrelated missing legacy
            // entities before that audit can run.
            data.putInt("SpawnX", 2000000);
            data.putInt("SpawnY", 80);
            data.putInt("SpawnZ", 2000000);
            try (OutputStream output = Files.newOutputStream(temporary.toPath())) {
                NbtIo.writeCompressed(root, output);
            }
            Files.move(temporary.toPath(), levelDat.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not relocate the disposable test spawn",
                    exception);
        } finally {
            temporary.delete();
        }
    }

    private void serverStarted(FMLServerStartedEvent event) {
        // Forge 37 moves entities from legacy chunk NBT into separate entity storage
        // after the server-started event. Audit after one second so fixture entities
        // have completed that normal migration before their saved state is checked.
        pendingServer = event.getServer();
        pendingTicks = 20;
    }

    private void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingServer == null || --pendingTicks > 0) {
            return;
        }
        MinecraftServer server = pendingServer;
        pendingServer = null;
        runAudit(server);
    }

    private void runAudit(MinecraftServer server) {
        ServerLevel world = server.getLevel(Level.OVERWORLD);
        String phase = System.getProperty(PHASE_PROPERTY, "fresh").trim();
        Path marker = worldRoot(server).resolve(MARKER_NAME);
        try {
            Properties evidence = read(marker);
            if ("fresh".equals(phase)) {
                clearProbe(world);
                int gameplay = verifyGameplay(world);
                int generated = verifyWorldGeneration(world);
                evidence.setProperty("gameplay_checks", Integer.toString(gameplay));
                evidence.setProperty("worldgen_checks", Integer.toString(generated));
                evidence.setProperty("fresh_complete", "true");
            } else if ("reload".equals(phase)) {
                require("true".equals(evidence.getProperty("fresh_complete")),
                        "Fresh integration evidence was not retained");
                verifyReload(world);
                evidence.setProperty("reload_complete", "true");
            } else if (phase.startsWith("upgrade-110-")) {
                verifyForwardFixture(world, "1.10.2", "1.0.0.110021",
                        "2030960E217C3F61AE4919C91058696B02F9FAE570BE1CD7B698696EA7BEB861");
                if (phase.contains("-combined-")) {
                    verifyLegacyHookCoexistence(worldRoot(server));
                }
                evidence.setProperty(phase.replace('-', '_') + "_complete", "true");
            } else if (phase.startsWith("upgrade-111-")) {
                verifyForwardFixture(world, "1.11.2", "1.0.1.111021",
                        "56D8B8C1FA7F2289C9F9A3BCF2BEB2D15F0F880373D0647BB9BDBFA7E1D5FE54");
                evidence.setProperty(phase.replace('-', '_') + "_complete", "true");
            } else if (phase.startsWith("upgrade-112-")) {
                verifyForwardFixture(world, "1.12.2", "1.0.1.112021",
                        "C6E83E66AFB35AE47661FB560F81A458B95FB50D87E940CE682B7C91DB034543");
                evidence.setProperty(phase.replace('-', '_') + "_complete", "true");
            } else if (phase.startsWith("upgrade-113-")) {
                verifyForwardFixture(world, "1.13.2", "1.0.1.113021",
                        "42772E921FE7EAF8A8D1EA7C12F48C04626FDD0B880B827FB3A82FB7A5ACFC7A");
                verifyFlattenedFixtureStates(world, "1.13");
                evidence.setProperty(phase.replace('-', '_') + "_complete", "true");
            } else if (phase.startsWith("upgrade-114-")) {
                verifyForwardFixture(world, "1.14.4", "1.0.1.114041",
                        "213A09F31EE02CE1C01E4C504147C13D3BD63A6AA11EBE52CE41A38735D45D1B");
                verifyFlattenedFixtureStates(world, "1.14");
                evidence.setProperty(phase.replace('-', '_') + "_complete", "true");
            } else if (phase.startsWith("upgrade-115-")) {
                verifyForwardFixture(world, "1.15.2", "1.0.1.115021",
                        "9AE28332EA21700C5DE8D3597FC40F5B06D85E8A7FB3C0DE650A2F8BC5E0895C");
                verifyFlattenedFixtureStates(world, "1.15");
                evidence.setProperty(phase.replace('-', '_') + "_complete", "true");
            } else if (phase.startsWith("upgrade-116-")) {
                verifyForwardFixture(world, "1.16.5", "1.0.1.116051",
                        "54388D358581579723EFBEF6B06DDEF775CEB6FF8DD9289DFDDE82B951A9A2D7");
                verifyFlattenedFixtureStates(world, "1.16");
                evidence.setProperty(phase.replace('-', '_') + "_complete", "true");
            } else if (phase.startsWith("forward-sylvester")) {
                SylvesterCounts counts = auditSylvester(worldRoot(server));
                require(counts.grassTop == 0L && counts.grassBottom == 1661527L,
                        "Sylvester grass slabs changed: " + counts.grassTop + " top, " +
                                counts.grassBottom + " bottom");
                require(counts.dirtTop == 12L && counts.dirtBottom == 2956L,
                        "Sylvester dirt slabs changed: " + counts.dirtTop + " top, " +
                                counts.dirtBottom + " bottom");
                require(ModWorldState.get(world) != null && ModWorldState.SCHEMA_VERSION == 1,
                        "Sylvester world state was not readable");
                if ("forward-sylvester".equals(phase)) {
                    require(counts.dirtItems == 6563L,
                            "Sylvester dirt slab item count changed: " + counts.dirtItems);
                    writeSylvesterEvidence(evidence, counts);
                } else {
                    require("true".equals(evidence.getProperty(
                                    "forward_sylvester_complete")),
                            "First Sylvester audit evidence was not retained");
                    require(Long.toString(counts.grassTop).equals(evidence.getProperty(
                                    "sylvester_grass_top")) &&
                            Long.toString(counts.grassBottom).equals(evidence.getProperty(
                                    "sylvester_grass_bottom")) &&
                            Long.toString(counts.dirtTop).equals(evidence.getProperty(
                                    "sylvester_dirt_top")) &&
                            Long.toString(counts.dirtBottom).equals(evidence.getProperty(
                                    "sylvester_dirt_bottom")),
                            "Sylvester slabs changed on reload");
                    require(Long.toString(counts.dirtItems).equals(evidence.getProperty(
                                    "sylvester_dirt_items")),
                            "Sylvester dirt slab items changed on reload: " + counts.dirtItems);
                    evidence.setProperty("sylvester_reload_dirt_items",
                            Long.toString(counts.dirtItems));
                }
                evidence.setProperty(phase.replace('-', '_') + "_complete", "true");
            } else {
                throw new IllegalStateException("Unknown integration phase " + phase);
            }
            write(marker, evidence);
            LOGGER.info("SKYSGRASSSLABS INTEGRATION PASS phase={}", phase);
        } catch (Throwable failure) {
            LOGGER.error("Sky's Grass Slabs integration audit failed", failure);
            throw new RuntimeException(failure);
        } finally {
            server.halt(false);
        }
    }

    private static int verifyGameplay(ServerLevel world) {
        int checks = 0;
        verifyRegistries();
        checks += 8;

        BlockState dirtTop = slab(ModBlocks.DIRT_SLAB, SlabType.TOP, false);
        BlockState dirtBottom = slab(ModBlocks.DIRT_SLAB, SlabType.BOTTOM, false);
        BlockState grassTop = snowySlab(ModBlocks.GRASS_SLAB, SlabType.TOP, false, false);
        BlockState grassBottom = snowySlab(ModBlocks.GRASS_SLAB, SlabType.BOTTOM, false, false);
        BlockState pathTop = slab(ModBlocks.PATH_SLAB, SlabType.TOP, false);
        BlockState pathBottom = slab(ModBlocks.PATH_SLAB, SlabType.BOTTOM, false);
        require(dirtTop.getValue(SlabBlock.TYPE) == SlabType.TOP
                && dirtBottom.getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                "Native slab orientation is unavailable");
        require(near(dirtBottom.getShape(world, ORIGIN).bounds().maxY, 0.5D)
                && near(dirtTop.getShape(world, ORIGIN).bounds().minY, 0.5D),
                "Native slab geometry changed");
        require(near(pathBottom.getShape(world, ORIGIN).bounds().maxY, 7.0D / 16.0D)
                && near(pathTop.getShape(world, ORIGIN).bounds().minY, 0.5D)
                && near(pathTop.getShape(world, ORIGIN).bounds().maxY, 15.0D / 16.0D),
                "Path slab geometry changed");
        require(near(TurfBlock.TURF_SHAPE.bounds().maxY, 1.0D / 16.0D),
                "Turf is not one pixel high");
        require(near(defaultState(ModBlocks.TURF).getCollisionShape(world, ORIGIN)
                        .bounds().maxY, 1.0D / 16.0D),
                "Turf does not have carpet collision");
        require(!defaultState(ModBlocks.TURF).isFaceSturdy(world, ORIGIN, Direction.NORTH),
                "Turf presents a solid horizontal face to fences");
        checks += 6;

        // Grass covering a full grass block must immediately dirtify the support.
        BlockPos grassCover = ORIGIN;
        world.setBlock(grassCover.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        world.setBlock(grassCover, grassBottom, 3);
        require(world.getBlockState(grassCover.below()).getBlock() == Blocks.DIRT,
                "Grass slab did not dirtify its grass support");
        checks++;

        // Snow is a native saved state and follows snow above or beside either soil slab.
        BlockPos snow = ORIGIN.east(2);
        world.setBlock(snow, dirtBottom, 3);
        world.setBlock(snow.east().below(), Blocks.DIRT.defaultBlockState(), 3);
        world.setBlock(snow.east(), Blocks.SNOW.defaultBlockState(), 3);
        ((Block) ModBlocks.DIRT_SLAB).tick(world.getBlockState(snow), world, snow,
                new Random(1L));
        require(world.getBlockState(snow).getValue(SnowyDirtBlock.SNOWY),
                "Dirt slab did not acquire its snow cap");
        world.removeBlock(snow.east(), false);
        ((Block) ModBlocks.DIRT_SLAB).tick(world.getBlockState(snow), world, snow,
                new Random(2L));
        require(!world.getBlockState(snow).getValue(SnowyDirtBlock.SNOWY),
                "Dirt slab retained a stale snow cap");
        checks += 2;

        // Covered, wet grass decays to wet dirt without changing orientation.
        BlockPos wetGrass = ORIGIN.east(4);
        world.setBlock(wetGrass, snowySlab(ModBlocks.GRASS_SLAB,
                SlabType.TOP, true, false), 3);
        ((Block) ModBlocks.GRASS_SLAB).tick(world.getBlockState(wetGrass), world, wetGrass,
                new Random(3L));
        require(world.getBlockState(wetGrass).getBlock() == ModBlocks.DIRT_SLAB
                && world.getBlockState(wetGrass).getValue(SlabBlock.TYPE) == SlabType.TOP
                && world.getBlockState(wetGrass).getValue(SlabBlock.WATERLOGGED),
                "Wet grass did not decay to matching wet dirt");
        checks++;

        // A path accepts water only by becoming matching wet dirt.
        BlockPos wetPath = ORIGIN.east(6);
        world.setBlock(wetPath, pathBottom, 3);
        require(((SlabBlock) ModBlocks.PATH_SLAB).placeLiquid(world, wetPath, pathBottom,
                Fluids.WATER.defaultFluidState()), "Path slab rejected water unexpectedly");
        require(world.getBlockState(wetPath).getBlock() == ModBlocks.DIRT_SLAB
                && world.getBlockState(wetPath).getValue(SlabBlock.WATERLOGGED)
                && world.getBlockState(wetPath).getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                "Waterlogged path did not become matching wet dirt");
        checks += 2;

        Player player = FakePlayerFactory.getMinecraft(world);
        player.getAbilities().mayBuild = true;

        // Matching slab items normalize rather than retaining DOUBLE.
        BlockPos normalize = ORIGIN.east(8);
        world.setBlock(normalize, dirtBottom, 3);
        ItemStack secondSlab = new ItemStack(ModBlocks.DIRT_SLAB);
        player.setItemInHand(InteractionHand.MAIN_HAND, secondSlab);
        InteractionResult combined = item(ModBlocks.DIRT_SLAB).useOn(
                useContext(player, normalize, Direction.UP));
        require(combined == InteractionResult.SUCCESS
                && world.getBlockState(normalize).getBlock() == Blocks.DIRT,
                "Two dirt slabs did not normalize to vanilla dirt");
        checks++;

        // Turf converts dry dirt slabs and leaves wet slabs untouched.
        BlockPos turfUse = ORIGIN.east(10);
        world.setBlock(turfUse, dirtTop, 3);
        ItemStack turfStack = new ItemStack(ModBlocks.TURF, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, turfStack);
        require(item(ModBlocks.TURF).useOn(
                useContext(player, turfUse, Direction.UP)) == InteractionResult.SUCCESS
                && world.getBlockState(turfUse).getBlock() == ModBlocks.GRASS_SLAB
                && world.getBlockState(turfUse).getValue(SlabBlock.TYPE) == SlabType.TOP,
                "Turf did not convert a dry dirt slab");
        world.setBlock(turfUse.east(), slab(ModBlocks.DIRT_SLAB,
                SlabType.BOTTOM, true), 3);
        int before = turfStack.getCount();
        require(item(ModBlocks.TURF).useOn(
                useContext(player, turfUse.east(), Direction.UP)) == InteractionResult.FAIL
                && turfStack.getCount() == before,
                "Turf changed or consumed itself on a wet dirt slab");
        checks += 2;

        // Vanilla shovels use the common Forge tool classification and preserve orientation.
        BlockPos flatten = ORIGIN.east(12);
        world.setBlock(flatten, grassTop, 3);
        world.removeBlock(flatten.above(), false);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        InteractionResult flattened = Items.IRON_SHOVEL.useOn(
                useContext(player, flatten, Direction.UP));
        require(flattened.consumesAction() &&
                world.getBlockState(flatten).getBlock() == ModBlocks.PATH_SLAB
                && world.getBlockState(flatten).getValue(SlabBlock.TYPE) == SlabType.TOP
                && shovel.getDamageValue() == 1,
                "Shovel flattening did not preserve the slab and tool contract");
        checks++;

        verifyTurfSupportAndSheep(world);
        checks += 4;
        verifyRecipes(world);
        checks += 8;
        verifyDrops(world);
        checks += 2;

        ModWorldState state = ModWorldState.get(world);
        require(state != null && ModWorldState.SCHEMA_VERSION == 1,
                "Schema 1 world state is unavailable");
        checks++;

        // Leave stable save sentinels for the reload phase.
        world.setBlock(ORIGIN.south(4), grassBottom, 3);
        world.setBlock(ORIGIN.south(5), pathTop, 3);
        world.setBlock(ORIGIN.south(6).below(), Blocks.DIRT.defaultBlockState(), 3);
        world.setBlock(ORIGIN.south(6), defaultState(ModBlocks.TURF), 3);
        return checks;
    }

    private static void verifyRegistries() {
        require(ForgeRegistries.BLOCKS.getValue(id("dirt_slab")) == ModBlocks.DIRT_SLAB,
                "Dirt slab registry identity changed");
        require(ForgeRegistries.BLOCKS.getValue(id("grass_slab")) == ModBlocks.GRASS_SLAB,
                "Grass slab registry identity changed");
        require(ForgeRegistries.BLOCKS.getValue(id("path_slab")) == ModBlocks.PATH_SLAB,
                "Path slab registry identity changed");
        require(ForgeRegistries.BLOCKS.getValue(id("turf")) == ModBlocks.TURF,
                "Turf registry identity changed");
        for (Block block : new Block[] {ModBlocks.DIRT_SLAB, ModBlocks.GRASS_SLAB,
                ModBlocks.PATH_SLAB, ModBlocks.TURF}) {
            require(ForgeRegistries.ITEMS.getValue(ForgeRegistries.BLOCKS.getKey(block))
                    == block.asItem(), "Block item identity changed for " + block);
        }
    }

    private static void verifyTurfSupportAndSheep(ServerLevel world) {
        BlockPos turf = ORIGIN.south(2);
        world.setBlock(turf.below(), Blocks.DIRT.defaultBlockState(), 3);
        world.setBlock(turf, defaultState(ModBlocks.TURF), 3);
        require(world.getBlockState(turf).canSurvive(world, turf),
                "Turf rejected full dirt support");
        world.setBlock(turf.below(), slab(ModBlocks.DIRT_SLAB,
                SlabType.BOTTOM, false), 3);
        require(!defaultState(ModBlocks.TURF).canSurvive(world, turf),
                "Turf accepted partial support");

        BlockPos coveredDirt = turf.east(2).below();
        world.setBlock(coveredDirt, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        world.setBlock(coveredDirt.above(), defaultState(ModBlocks.TURF), 3);
        require(world.getBlockState(coveredDirt).getBlock() == Blocks.DIRT,
                "Turf did not dirtify its grass support on placement");
        require(!GrassSpread.growTarget(world, coveredDirt),
                "Grass spread through turf to its supporting dirt");

        Sheep sheep = EntityType.SHEEP.create(world);
        EntityJoinWorldEvent join = new EntityJoinWorldEvent(sheep, world);
        CommonEvents.addTurfEatingTask(join);
        CommonEvents.addTurfEatingTask(join);
        int turfTasks = countTurfEatingTasks(sheep.goalSelector);
        require(turfTasks == 1, "Sheep received duplicate turf eating tasks");
        require(((Block) ModBlocks.TURF).getFlammability(defaultState(ModBlocks.TURF), world,
                turf, Direction.UP) > 0, "Turf is not flammable");
    }

    private static void verifyRecipes(ServerLevel world) {
        String[] names = {"dirt_slab", "grass_slab", "grass_block_from_seeds",
                "grass_slab_from_seeds", "turf"};
        for (String name : names) {
            require(world.getRecipeManager().byKey(id(name)).isPresent(),
                    "Recipe did not load: " + name);
        }
        Recipe<?> loadedRecipe = world.getRecipeManager().byKey(id("turf")).orElseThrow(
                () -> new IllegalStateException("Turf recipe did not load"));
        require(loadedRecipe instanceof CraftingRecipe,
                "Turf recipe cannot be used by a crafting container");
        CraftingRecipe recipe = (CraftingRecipe) loadedRecipe;
        require(!recipe.isSpecial() && recipe.getSerializer() == TurfCuttingRecipe.SERIALIZER,
                "Turf recipe is not visible or has the wrong serializer");

        CraftingContainer crafting = new CraftingContainer(new AbstractContainerMenu(null, 0) {
            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        }, 2, 2);
        crafting.setItem(0, new ItemStack(ModBlocks.GRASS_SLAB));
        ItemStack shovel = new ItemStack(Items.DIAMOND_SHOVEL);
        CompoundTag tag = new CompoundTag();
        tag.putString("probe", "retained");
        shovel.setTag(tag);
        shovel.setDamageValue(17);
        crafting.setItem(1, shovel);
        require(recipe.matches(crafting, world)
                && recipe.assemble(crafting).getItem() == item(ModBlocks.TURF),
                "Turf recipe did not match a grass slab and shovel");
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(crafting);
        require(remaining.get(0).getItem() == item(ModBlocks.DIRT_SLAB)
                && remaining.get(1).getItem() == Items.DIAMOND_SHOVEL
                && remaining.get(1).getCount() == 1
                && remaining.get(1).getDamageValue() == 17
                && remaining.get(1).hasTag()
                && "retained".equals(remaining.get(1).getTag().getString("probe")),
                "Turf recipe changed its shovel or returned the wrong soil: "
                        + remaining + ", damage=" + remaining.get(1).getDamageValue()
                        + ", tag=" + remaining.get(1).getTag());
    }

    private static void verifyDrops(ServerLevel world) {
        Player player = FakePlayerFactory.getMinecraft(world);
        List<ItemStack> grassDrops = Block.getDrops(snowySlab(ModBlocks.GRASS_SLAB,
                SlabType.BOTTOM, false, false), world, ORIGIN, null, player, ItemStack.EMPTY);
        require(grassDrops.size() == 1
                && grassDrops.get(0).getItem() == item(ModBlocks.DIRT_SLAB),
                "Grass slab normal drop changed");
        List<ItemStack> pathDrops = Block.getDrops(slab(ModBlocks.PATH_SLAB,
                SlabType.TOP, false), world, ORIGIN, null, player, ItemStack.EMPTY);
        require(pathDrops.size() == 1
                && pathDrops.get(0).getItem() == item(ModBlocks.DIRT_SLAB),
                "Path slab drop changed");
    }

    private static int verifyWorldGeneration(ServerLevel world) {
        int skySlabs = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int chunkZ = 24; chunkZ < 29; ++chunkZ) {
            for (int chunkX = 24; chunkX < 29; ++chunkX) {
                ChunkAccess chunk = world.getChunk(chunkX, chunkZ);
                int startX = chunkX << 4;
                int startZ = chunkZ << 4;
                for (int localZ = 0; localZ < 16; ++localZ) {
                    for (int localX = 0; localX < 16; ++localX) {
                        for (int y = 1; y < 255; ++y) {
                            if (chunk.getBlockState(pos.set(startX + localX, y,
                                    startZ + localZ)).getBlock() == ModBlocks.GRASS_SLAB) {
                                ++skySlabs;
                            }
                        }
                    }
                }
            }
        }
        require(skySlabs > 0, "Normal chunk generation produced no Sky grass slabs");
        return skySlabs;
    }

    private static void verifyReload(ServerLevel world) {
        world.getChunk(0, 0);
        require(world.getBlockState(ORIGIN.south(4)).getBlock() == ModBlocks.GRASS_SLAB
                && world.getBlockState(ORIGIN.south(4)).getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                "Grass slab changed across reload");
        require(world.getBlockState(ORIGIN.south(5)).getBlock() == ModBlocks.PATH_SLAB
                && world.getBlockState(ORIGIN.south(5)).getValue(SlabBlock.TYPE) == SlabType.TOP,
                "Path slab changed across reload");
        require(world.getBlockState(ORIGIN.south(6)).getBlock() == ModBlocks.TURF,
                "Turf changed across reload");
        require(ModWorldState.get(world) != null && ModWorldState.SCHEMA_VERSION == 1,
                "World state changed across reload");
    }

    private static void verifyForwardFixture(ServerLevel world, String minecraft,
            String modVersion, String jarSha256) throws IOException {
        world.getChunk(0, 0);
        Path sourceMarker = worldRoot(world.getServer()).resolve(
                "skysgrassslabs-forward-fixture.properties");
        Properties source = read(sourceMarker);
        require(minecraft.equals(source.getProperty("source_minecraft")),
                "Forward fixture source Minecraft version changed");
        require(modVersion.equals(source.getProperty("source_mod_version")),
                "Forward fixture source mod version changed");
        if (jarSha256 != null) {
            require(jarSha256.equals(source.getProperty("source_jar_sha256")),
                    "Forward fixture source jar changed");
        }
        Path legacyRegistry = worldRoot(world.getServer()).resolve(
                "data/skysgrassslabs_legacy_registry.dat");
        if ("1.13.2".equals(minecraft) || "1.14.4".equals(minecraft) ||
                "1.15.2".equals(minecraft) || "1.16.5".equals(minecraft)) {
            require(!Files.exists(legacyRegistry),
                    "The numeric legacy bridge ran on an already flattened world");
        } else {
            require(Files.isRegularFile(legacyRegistry),
                    "The preserved legacy registry sidecar is missing");
        }

        BlockPos origin = new BlockPos(8, 65, 8);
        verifyFixtureSlab(world, origin, ModBlocks.DIRT_SLAB, SlabType.TOP);
        verifyFixtureSlab(world, origin.east(), ModBlocks.DIRT_SLAB, SlabType.BOTTOM);
        verifyFixtureSlab(world, origin.east(2), ModBlocks.GRASS_SLAB, SlabType.TOP);
        verifyFixtureSlab(world, origin.east(3), ModBlocks.GRASS_SLAB, SlabType.BOTTOM);
        verifyFixtureSlab(world, origin.east(4), ModBlocks.PATH_SLAB, SlabType.TOP);
        verifyFixtureSlab(world, origin.east(5), ModBlocks.PATH_SLAB, SlabType.BOTTOM);
        require(world.getBlockState(origin.east(6)).getBlock() == ModBlocks.TURF,
                "Forward fixture turf was lost");

        BlockEntity entity = world.getBlockEntity(origin.south(2));
        require(entity instanceof Container, "Forward fixture chest was lost");
        Container chest = (Container) entity;
        verifyFixtureStack(chest.getItem(0), ModBlocks.DIRT_SLAB, 2);
        verifyFixtureStack(chest.getItem(1), ModBlocks.GRASS_SLAB, 3);
        require(chest.getItem(1).hasTag()
                && "retained".equals(chest.getItem(1).getTag().getString("fixture")),
                "Forward fixture stack NBT was lost");
        if ("1.13.2".equals(minecraft) || "1.14.4".equals(minecraft) ||
                "1.15.2".equals(minecraft) || "1.16.5".equals(minecraft)) {
            require(chest.getItem(1).getTag().getInt("source_data_version") == 1631,
                    "The flattened fixture's custom integer NBT was lost");
        }
        verifyFixtureStack(chest.getItem(2), ModBlocks.PATH_SLAB, 4);
        verifyFixtureStack(chest.getItem(3), ModBlocks.TURF, 5);

        List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class,
                new AABB(0.0D, world.getMinBuildHeight(), 0.0D,
                        16.0D, world.getMaxBuildHeight(), 16.0D));
        require(items.size() == 1, "Forward fixture entity stack count changed: " + items.size());
        verifyFixtureStack(items.get(0).getItem(), ModBlocks.GRASS_SLAB, 6);

        ModWorldState state = ModWorldState.get(world);
        require(state != null && ModWorldState.SCHEMA_VERSION == 1
                && state.migratedChunks() == 2
                && state.migratedGrassBlocks() == 8
                && state.migratedGrassBlocksTop() == 3
                && state.migratedGrassBlocksBottom() == 5
                && state.migratedDirtBlocks() == 18
                && state.migratedDirtBlocksTop() == 7
                && state.migratedDirtBlocksBottom() == 11
                && state.migratedGrassItems() == 13
                && state.migratedDirtItems() == 17
                && Long.valueOf(19L).equals(state.unsupported().get(
                        "fixture:unsupported_shape")),
                "Forward fixture world state changed");
    }

    private static void verifyFlattenedFixtureStates(ServerLevel world, String sourceVersion) {
        BlockPos origin = new BlockPos(8, 65, 8);
        BlockState snowyDirt = world.getBlockState(origin.south(4));
        require(snowyDirt.getBlock() == ModBlocks.DIRT_SLAB
                && snowyDirt.getValue(SlabBlock.TYPE) == SlabType.TOP
                && snowyDirt.getValue(SnowyDirtBlock.SNOWY)
                && !snowyDirt.getValue(SlabBlock.WATERLOGGED),
                "The " + sourceVersion + " snowy dirt slab state changed: " + snowyDirt);
        BlockState wetDirt = world.getBlockState(origin.south(5));
        require(wetDirt.getBlock() == ModBlocks.DIRT_SLAB
                && wetDirt.getValue(SlabBlock.TYPE) == SlabType.BOTTOM
                && wetDirt.getValue(SlabBlock.WATERLOGGED),
                "The " + sourceVersion + " waterlogged dirt slab state changed: " + wetDirt);
        BlockState snowyGrass = world.getBlockState(origin.south(6));
        require(snowyGrass.getBlock() == ModBlocks.GRASS_SLAB
                && snowyGrass.getValue(SlabBlock.TYPE) == SlabType.TOP
                && snowyGrass.getValue(SnowyDirtBlock.SNOWY)
                && !snowyGrass.getValue(SlabBlock.WATERLOGGED),
                "The " + sourceVersion + " snowy grass slab state changed: " + snowyGrass);
        BlockState wetGrass = world.getBlockState(origin.south(7));
        require(wetGrass.getBlock() == ModBlocks.GRASS_SLAB
                && wetGrass.getValue(SlabBlock.TYPE) == SlabType.BOTTOM
                && wetGrass.getValue(SlabBlock.WATERLOGGED),
                "The " + sourceVersion + " waterlogged grass slab state changed: " + wetGrass);
    }

    private static SylvesterCounts auditSylvester(Path worldDirectory) throws IOException {
        Map<Integer, String> legacyBlocks = savedBlockNames(worldDirectory);
        SylvesterCounts counts = new SylvesterCounts();
        for (String dimension : new String[] {"", "DIM-1", "DIM1"}) {
            File directory = dimension.isEmpty() ? worldDirectory.toFile()
                    : worldDirectory.resolve(dimension).toFile();
            auditRegionDirectory(new File(directory, "region"), dimension,
                    legacyBlocks, counts);
        }
        LOGGER.info("Sylvester audit: {} chunks, grass={}/{}, dirt={}/{}, items={}/{}/{}/{}",
                counts.chunks, counts.grassTop, counts.grassBottom, counts.dirtTop,
                counts.dirtBottom, counts.grassItems, counts.dirtItems, counts.pathItems,
                counts.turfItems);
        return counts;
    }

    private static void auditRegionDirectory(File regionDirectory, String dimension,
            Map<Integer, String> legacyBlocks, SylvesterCounts counts) throws IOException {
        File[] files = regionDirectory.listFiles();
        if (files == null) return;
        List<File> regions = new ArrayList<File>();
        for (File file : files) {
            if (file.getName().startsWith("r.") && file.getName().endsWith(".mca")) {
                regions.add(file);
            }
        }
        Collections.sort(regions, Comparator.comparing(File::getName));
        for (File file : regions) {
            try (RandomAccessFile region = new RandomAccessFile(file, "r")) {
                for (int index = 0; index < 1024; ++index) {
                    DataInputStream input = readRegionChunk(region, index, file);
                    if (input == null) continue;
                    try (DataInputStream chunk = input) {
                        CompoundTag root = NbtIo.read(chunk);
                        CompoundTag level = root.getCompound("Level");
                        countSavedBlocks(level, legacyBlocks, counts);
                        countStacks(level.getList("TileEntities", 10), counts);
                        countStacks(level.getList("Entities", 10), counts);
                        ++counts.chunks;
                    }
                }
            }
        }
    }

    private static DataInputStream readRegionChunk(RandomAccessFile region, int index,
            File source) throws IOException {
        region.seek(index * 4L);
        int location = region.readInt();
        int sector = location >>> 8;
        int sectors = location & 255;
        if (sector == 0 || sectors == 0) return null;

        long offset = sector * 4096L;
        if (offset + 5L > region.length()) {
            throw new IOException("Invalid chunk offset in " + source);
        }
        region.seek(offset);
        int length = region.readInt();
        if (length <= 1 || length > sectors * 4096 - 4 || offset + 4L + length > region.length()) {
            throw new IOException("Invalid chunk length in " + source);
        }
        int compression = region.readUnsignedByte();
        byte[] compressed = new byte[length - 1];
        region.readFully(compressed);
        InputStream bytes = new ByteArrayInputStream(compressed);
        if (compression == 1) {
            bytes = new GZIPInputStream(bytes);
        } else if (compression == 2) {
            bytes = new InflaterInputStream(bytes);
        } else if (compression != 3) {
            throw new IOException("Unsupported region compression " + compression + " in " + source);
        }
        return new DataInputStream(bytes);
    }

    private static Map<Integer, String> savedBlockNames(Path worldDirectory) throws IOException {
        CompoundTag root;
        try (FileInputStream input = new FileInputStream(
                worldDirectory.resolve("level.dat").toFile())) {
            root = NbtIo.readCompressed(input);
        }
        CompoundTag data = root.getCompound("Data");
        CompoundTag fml = data.getCompound("FML");
        if (!fml.contains("Registries", 10)) fml = root.getCompound("FML");
        CompoundTag registries = fml.getCompound("Registries");
        Map<Integer, String> names = new LinkedHashMap<Integer, String>();
        for (String registry : new String[] {"minecraft:blocks", "fml:blocks"}) {
            addSavedBlockNames(registries.getCompound(registry), names);
        }
        if (names.isEmpty()) {
            File sidecar = worldDirectory.resolve("data")
                    .resolve("skysgrassslabs_legacy_registry.dat").toFile();
            if (sidecar.isFile()) {
                try (FileInputStream input = new FileInputStream(sidecar)) {
                    addSavedBlockNames(NbtIo.readCompressed(input)
                            .getCompound("Blocks"), names);
                }
            }
        }
        return names;
    }

    private static void verifyLegacyHookCoexistence(Path worldDirectory) throws IOException {
        Path skySidecar = worldDirectory.resolve("data")
                .resolve("skysgrassslabs_legacy_registry.dat");
        Path mineralogySidecar = worldDirectory.resolve("data")
                .resolve("mineralogy_legacy_registry.dat");
        require(Files.isRegularFile(skySidecar),
                "Sky's Grass Slabs legacy registry sidecar is missing");
        require(Files.isRegularFile(mineralogySidecar),
                "Mineralogy legacy registry sidecar is missing");

        CompoundTag skySnapshot;
        CompoundTag mineralogySnapshot;
        try (FileInputStream input = new FileInputStream(skySidecar.toFile())) {
            skySnapshot = NbtIo.readCompressed(input).getCompound("Blocks");
        }
        try (FileInputStream input = new FileInputStream(mineralogySidecar.toFile())) {
            mineralogySnapshot = NbtIo.readCompressed(input).getCompound("Blocks");
        }
        require(skySnapshot.equals(mineralogySnapshot),
                "Legacy hooks did not preserve the same FML block snapshot");
        require(savedBlockNames(worldDirectory).containsValue("skysgrassslabs:grass_slab"),
                "The shared legacy registry snapshot lost Sky's grass slab ID");
    }

    private static void addSavedBlockNames(CompoundTag blockSnapshot,
            Map<Integer, String> names) {
        ListTag ids = blockSnapshot.getList("ids", 10);
        for (int index = 0; index < ids.size(); ++index) {
            CompoundTag entry = ids.getCompound(index);
            names.put(Integer.valueOf(entry.getInt("V")), entry.getString("K"));
        }
    }

    private static void countSavedBlocks(CompoundTag level,
            Map<Integer, String> legacyBlocks, SylvesterCounts counts) {
        ListTag sections = level.getList("Sections", 10);
        for (int sectionIndex = 0; sectionIndex < sections.size(); ++sectionIndex) {
            CompoundTag section = sections.getCompound(sectionIndex);
            if (section.contains("Palette", 9)) {
                countFlattenedSection(section, counts);
            } else {
                countLegacySection(section, legacyBlocks, counts);
            }
        }
    }

    private static void countLegacySection(CompoundTag section,
            Map<Integer, String> names, SylvesterCounts counts) {
        byte[] blocks = section.getByteArray("Blocks");
        if (blocks.length != 4096) return;
        byte[] metadata = section.getByteArray("Data");
        byte[] add = section.getByteArray("Add");
        for (int index = 0; index < blocks.length; ++index) {
            int high = nibble(add, index);
            String name = names.get(Integer.valueOf(blocks[index] & 255 | high << 8));
            countBlock(name, (nibble(metadata, index) & 1) == 0, counts);
        }
    }

    private static void countFlattenedSection(CompoundTag section, SylvesterCounts counts) {
        ListTag palette = section.getList("Palette", 10);
        if (palette.isEmpty()) return;
        String[] names = new String[palette.size()];
        boolean[] top = new boolean[palette.size()];
        for (int index = 0; index < palette.size(); ++index) {
            CompoundTag entry = palette.getCompound(index);
            names[index] = entry.getString("Name");
            CompoundTag properties = entry.getCompound("Properties");
            top[index] = "top".equals(properties.getString("type"));
        }
        long[] packed = section.getLongArray("BlockStates");
        if (palette.size() == 1 && packed.length == 0) {
            for (int index = 0; index < 4096; ++index) countBlock(names[0], top[0], counts);
            return;
        }
        int bits = 4;
        while ((1 << bits) < palette.size()) ++bits;
        for (int index = 0; index < 4096; ++index) {
            int paletteIndex = packedValue(packed, bits, index);
            if (paletteIndex >= 0 && paletteIndex < names.length) {
                countBlock(names[paletteIndex], top[paletteIndex], counts);
            }
        }
    }

    private static int packedValue(long[] packed, int bits, int index) {
        int valuesPerLong = 64 / bits;
        int paddedLength = (4096 + valuesPerLong - 1) / valuesPerLong;
        long mask = (1L << bits) - 1L;
        if (packed.length == paddedLength) {
            int longIndex = index / valuesPerLong;
            int offset = index % valuesPerLong * bits;
            return (int) (packed[longIndex] >>> offset & mask);
        }

        int compactLength = (4096 * bits + 63) / 64;
        if (packed.length != compactLength) {
            throw new IllegalArgumentException("Invalid block-state storage length "
                    + packed.length + " for " + bits + " bits");
        }
        int bitIndex = index * bits;
        int longIndex = bitIndex >>> 6;
        int offset = bitIndex & 63;
        long value = packed[longIndex] >>> offset;
        if (offset + bits > 64) {
            value |= packed[longIndex + 1] << (64 - offset);
        }
        return (int) (value & mask);
    }

    private static int nibble(byte[] values, int index) {
        if (values.length != 2048) return 0;
        return values[index >> 1] >> ((index & 1) * 4) & 15;
    }

    private static void countBlock(String name, boolean top, SylvesterCounts counts) {
        if ("skysgrassslabs:grass_slab".equals(name)) {
            if (top) ++counts.grassTop;
            else ++counts.grassBottom;
        } else if ("skysgrassslabs:dirt_slab".equals(name)) {
            if (top) ++counts.dirtTop;
            else ++counts.dirtBottom;
        }
    }

    private static void countStacks(Tag tag, SylvesterCounts counts) {
        if (tag instanceof CompoundTag) {
            CompoundTag compound = (CompoundTag) tag;
            if (compound.contains("id", 8) && compound.contains("Count", 99)) {
                int count = compound.getByte("Count") & 255;
                String id = compound.getString("id");
                if ("skysgrassslabs:grass_slab".equals(id)) counts.grassItems += count;
                else if ("skysgrassslabs:dirt_slab".equals(id)) counts.dirtItems += count;
                else if ("skysgrassslabs:path_slab".equals(id)) counts.pathItems += count;
                else if ("skysgrassslabs:turf".equals(id)) counts.turfItems += count;
            }
            for (String key : new ArrayList<String>(compound.getAllKeys())) {
                Tag child = compound .get(key);
                if (child != null) countStacks(child, counts);
            }
        } else if (tag instanceof ListTag) {
            ListTag list = (ListTag) tag;
            for (int index = 0; index < list.size(); ++index) {
                countStacks(list.get(index), counts);
            }
        }
    }

    private static void writeSylvesterEvidence(Properties evidence, SylvesterCounts counts) {
        evidence.setProperty("sylvester_chunks", Integer.toString(counts.chunks));
        evidence.setProperty("sylvester_grass_top", Long.toString(counts.grassTop));
        evidence.setProperty("sylvester_grass_bottom", Long.toString(counts.grassBottom));
        evidence.setProperty("sylvester_dirt_top", Long.toString(counts.dirtTop));
        evidence.setProperty("sylvester_dirt_bottom", Long.toString(counts.dirtBottom));
        evidence.setProperty("sylvester_grass_items", Long.toString(counts.grassItems));
        evidence.setProperty("sylvester_dirt_items", Long.toString(counts.dirtItems));
        evidence.setProperty("sylvester_path_items", Long.toString(counts.pathItems));
        evidence.setProperty("sylvester_turf_items", Long.toString(counts.turfItems));
    }

    private static void verifyFixtureSlab(Level world, BlockPos pos, Block expected,
            SlabType type) {
        BlockState state = world.getBlockState(pos);
        require(state.getBlock() == expected && state.getValue(SlabBlock.TYPE) == type
                && !state.getValue(SlabBlock.WATERLOGGED),
                "Forward fixture slab changed at " + pos + ": " + state);
    }

    private static void verifyFixtureStack(ItemStack stack, Block expected, int count) {
        require(stack.getItem() == expected.asItem() && stack.getCount() == count,
                "Forward fixture stack changed: " + stack);
    }

    private static BlockState slab(Block block, SlabType type, boolean waterlogged) {
        return defaultState(block) .setValue(SlabBlock.TYPE, type)
                 .setValue(SlabBlock.WATERLOGGED, waterlogged);
    }

    private static BlockState defaultState(Block block) {
        return block.defaultBlockState();
    }

    private static net.minecraft.world.item.Item item(Block block) {
        return block.asItem();
    }

    private static UseOnContext useContext(Player player, BlockPos pos, Direction face) {
        Vec3 hit = Vec3.atLowerCornerOf(pos).add(
                0.5D, face == Direction.UP ? 1.0D : 0.5D, 0.5D);
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(hit, face, pos, false));
    }

    @SuppressWarnings("unchecked")
    private static int countTurfEatingTasks(GoalSelector selector) {
        try {
            int count = 0;
            for (Field field : GoalSelector.class.getDeclaredFields()) {
                if (!Set.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                for (Object entry : (Set<Object>) field.get(selector)) {
                    if (entry instanceof WrappedGoal
                            && ((WrappedGoal) entry).getGoal() instanceof TurfEatingAI) {
                        ++count;
                    }
                }
            }
            return count;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not inspect sheep goals", exception);
        }
    }

    private static BlockState snowySlab(Block block, SlabType type,
            boolean waterlogged, boolean snowy) {
        return slab(block, type, waterlogged) .setValue(SnowyDirtBlock.SNOWY, snowy);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("skysgrassslabs", path);
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) < 0.000001D;
    }

    private static void clearProbe(Level world) {
        for (int x = ORIGIN.getX() - 1; x <= ORIGIN.getX() + 15; ++x) {
            for (int z = ORIGIN.getZ() - 1; z <= ORIGIN.getZ() + 8; ++z) {
                for (int y = ORIGIN.getY() - 2; y <= ORIGIN.getY() + 2; ++y) {
                    world.removeBlock(new BlockPos(x, y, z), false);
                }
            }
        }
    }

    private static Path worldRoot(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
    }

    private static Properties read(Path marker) throws IOException {
        Properties values = new Properties();
        if (Files.isRegularFile(marker)) {
            try (InputStream input = Files.newInputStream(marker)) {
                values.load(input);
            }
        }
        return values;
    }

    private static void write(Path marker, Properties values) throws IOException {
        try (OutputStream output = Files.newOutputStream(marker)) {
            values.store(output, "Sky's Grass Slabs Forge 1.17.1 integration evidence");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class SylvesterCounts {
        private int chunks;
        private long grassTop;
        private long grassBottom;
        private long dirtTop;
        private long dirtBottom;
        private long grassItems;
        private long dirtItems;
        private long pathItems;
        private long turfItems;
    }
}
