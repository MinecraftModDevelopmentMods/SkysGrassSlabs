package zone.moddev.mc.skysgrassslabs.integrationtest;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirtSnowy;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Fluids;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.INBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.properties.SlabType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BitArray;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
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

    public IntegrationTestMod() {
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
    }

    private void serverStarted(FMLServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        WorldServer world = server.getWorld(DimensionType.OVERWORLD);
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
                evidence.setProperty(phase.replace('-', '_') + "_complete", "true");
            } else if (phase.startsWith("upgrade-111-")) {
                verifyForwardFixture(world, "1.11.2", "1.0.1.111021",
                        "56D8B8C1FA7F2289C9F9A3BCF2BEB2D15F0F880373D0647BB9BDBFA7E1D5FE54");
                evidence.setProperty(phase.replace('-', '_') + "_complete", "true");
            } else if (phase.startsWith("upgrade-112-")) {
                verifyForwardFixture(world, "1.12.2", "1.0.1.112021",
                        "C6E83E66AFB35AE47661FB560F81A458B95FB50D87E940CE682B7C91DB034543");
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
                    require(counts.dirtItems == 7186L,
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
                    require(counts.dirtItems > 0L && counts.dirtItems <= Long.parseLong(
                                    evidence.getProperty("sylvester_dirt_items")),
                            "Sylvester dirt slab items increased or disappeared on reload: " +
                                    counts.dirtItems);
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
            server.initiateShutdown();
        }
    }

    private static int verifyGameplay(WorldServer world) {
        int checks = 0;
        verifyRegistries();
        checks += 8;

        IBlockState dirtTop = slab(ModBlocks.DIRT_SLAB, SlabType.TOP, false);
        IBlockState dirtBottom = slab(ModBlocks.DIRT_SLAB, SlabType.BOTTOM, false);
        IBlockState grassTop = snowySlab(ModBlocks.GRASS_SLAB, SlabType.TOP, false, false);
        IBlockState grassBottom = snowySlab(ModBlocks.GRASS_SLAB, SlabType.BOTTOM, false, false);
        IBlockState pathTop = slab(ModBlocks.PATH_SLAB, SlabType.TOP, false);
        IBlockState pathBottom = slab(ModBlocks.PATH_SLAB, SlabType.BOTTOM, false);
        require(dirtTop.get(BlockSlab.TYPE) == SlabType.TOP
                && dirtBottom.get(BlockSlab.TYPE) == SlabType.BOTTOM,
                "Native slab orientation is unavailable");
        require(near(dirtBottom.getShape(world, ORIGIN).getBoundingBox().maxY, 0.5D)
                && near(dirtTop.getShape(world, ORIGIN).getBoundingBox().minY, 0.5D),
                "Native slab geometry changed");
        require(near(pathBottom.getShape(world, ORIGIN).getBoundingBox().maxY, 7.0D / 16.0D)
                && near(pathTop.getShape(world, ORIGIN).getBoundingBox().minY, 0.5D)
                && near(pathTop.getShape(world, ORIGIN).getBoundingBox().maxY, 15.0D / 16.0D),
                "Path slab geometry changed");
        require(near(TurfBlock.TURF_SHAPE.getBoundingBox().maxY, 1.0D / 16.0D),
                "Turf is not one pixel high");
        require(near(defaultState(ModBlocks.TURF).getCollisionShape(world, ORIGIN)
                        .getBoundingBox().maxY, 1.0D / 16.0D),
                "Turf does not have carpet collision");
        require(((Block) ModBlocks.TURF).getBlockFaceShape(world, defaultState(ModBlocks.TURF),
                ORIGIN, EnumFacing.NORTH) == BlockFaceShape.UNDEFINED,
                "Turf presents a solid horizontal face to fences");
        checks += 6;

        // Grass covering a full grass block must immediately dirtify the support.
        BlockPos grassCover = ORIGIN;
        world.setBlockState(grassCover.down(), Blocks.GRASS_BLOCK.getDefaultState(), 3);
        world.setBlockState(grassCover, grassBottom, 3);
        require(world.getBlockState(grassCover.down()).getBlock() == Blocks.DIRT,
                "Grass slab did not dirtify its grass support");
        checks++;

        // Snow is a native saved state and follows snow above or beside either soil slab.
        BlockPos snow = ORIGIN.east(2);
        world.setBlockState(snow, dirtBottom, 3);
        world.setBlockState(snow.east().down(), Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(snow.east(), Blocks.SNOW.getDefaultState(), 3);
        ((Block) ModBlocks.DIRT_SLAB).tick(world.getBlockState(snow), world, snow,
                new Random(1L));
        require(world.getBlockState(snow).get(BlockDirtSnowy.SNOWY),
                "Dirt slab did not acquire its snow cap");
        world.removeBlock(snow.east());
        ((Block) ModBlocks.DIRT_SLAB).tick(world.getBlockState(snow), world, snow,
                new Random(2L));
        require(!world.getBlockState(snow).get(BlockDirtSnowy.SNOWY),
                "Dirt slab retained a stale snow cap");
        checks += 2;

        // Covered, wet grass decays to wet dirt without changing orientation.
        BlockPos wetGrass = ORIGIN.east(4);
        world.setBlockState(wetGrass, snowySlab(ModBlocks.GRASS_SLAB,
                SlabType.TOP, true, false), 3);
        ((Block) ModBlocks.GRASS_SLAB).tick(world.getBlockState(wetGrass), world, wetGrass,
                new Random(3L));
        require(world.getBlockState(wetGrass).getBlock() == ModBlocks.DIRT_SLAB
                && world.getBlockState(wetGrass).get(BlockSlab.TYPE) == SlabType.TOP
                && world.getBlockState(wetGrass).get(BlockSlab.WATERLOGGED),
                "Wet grass did not decay to matching wet dirt");
        checks++;

        // A path accepts water only by becoming matching wet dirt.
        BlockPos wetPath = ORIGIN.east(6);
        world.setBlockState(wetPath, pathBottom, 3);
        require(((BlockSlab) ModBlocks.PATH_SLAB).receiveFluid(world, wetPath, pathBottom,
                Fluids.WATER.getStillFluidState(false)), "Path slab rejected water unexpectedly");
        require(world.getBlockState(wetPath).getBlock() == ModBlocks.DIRT_SLAB
                && world.getBlockState(wetPath).get(BlockSlab.WATERLOGGED)
                && world.getBlockState(wetPath).get(BlockSlab.TYPE) == SlabType.BOTTOM,
                "Waterlogged path did not become matching wet dirt");
        checks += 2;

        EntityPlayer player = FakePlayerFactory.getMinecraft(world);
        player.abilities.allowEdit = true;

        // Matching slab items normalize rather than retaining DOUBLE.
        BlockPos normalize = ORIGIN.east(8);
        world.setBlockState(normalize, dirtBottom, 3);
        ItemStack secondSlab = new ItemStack(ModBlocks.DIRT_SLAB);
        player.setHeldItem(EnumHand.MAIN_HAND, secondSlab);
        EnumActionResult combined = item(ModBlocks.DIRT_SLAB).onItemUse(new ItemUseContext(
                player, secondSlab, normalize, EnumFacing.UP, 0.5F, 0.5F, 0.5F));
        require(combined == EnumActionResult.SUCCESS
                && world.getBlockState(normalize).getBlock() == Blocks.DIRT,
                "Two dirt slabs did not normalize to vanilla dirt");
        checks++;

        // Turf converts dry dirt slabs and leaves wet slabs untouched.
        BlockPos turfUse = ORIGIN.east(10);
        world.setBlockState(turfUse, dirtTop, 3);
        ItemStack turfStack = new ItemStack(ModBlocks.TURF, 2);
        player.setHeldItem(EnumHand.MAIN_HAND, turfStack);
        require(item(ModBlocks.TURF).onItemUse(new ItemUseContext(player, turfStack,
                turfUse, EnumFacing.UP, 0.5F, 1.0F, 0.5F)) == EnumActionResult.SUCCESS
                && world.getBlockState(turfUse).getBlock() == ModBlocks.GRASS_SLAB
                && world.getBlockState(turfUse).get(BlockSlab.TYPE) == SlabType.TOP,
                "Turf did not convert a dry dirt slab");
        world.setBlockState(turfUse.east(), slab(ModBlocks.DIRT_SLAB,
                SlabType.BOTTOM, true), 3);
        int before = turfStack.getCount();
        require(item(ModBlocks.TURF).onItemUse(new ItemUseContext(player, turfStack,
                turfUse.east(), EnumFacing.UP, 0.5F, 0.5F, 0.5F)) == EnumActionResult.FAIL
                && turfStack.getCount() == before,
                "Turf changed or consumed itself on a wet dirt slab");
        checks += 2;

        // Vanilla shovels use the common Forge tool classification and preserve orientation.
        BlockPos flatten = ORIGIN.east(12);
        world.setBlockState(flatten, grassTop, 3);
        world.removeBlock(flatten.up());
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        player.setHeldItem(EnumHand.MAIN_HAND, shovel);
        PlayerInteractEvent.RightClickBlock flattenEvent = new PlayerInteractEvent.RightClickBlock(
                player, EnumHand.MAIN_HAND, flatten, EnumFacing.UP,
                new Vec3d(flatten).add(0.5D, 1.0D, 0.5D));
        CommonEvents.flattenSlab(flattenEvent);
        require(flattenEvent.isCanceled()
                && world.getBlockState(flatten).getBlock() == ModBlocks.PATH_SLAB
                && world.getBlockState(flatten).get(BlockSlab.TYPE) == SlabType.TOP
                && shovel.getDamage() == 1,
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
        world.setBlockState(ORIGIN.south(4), grassBottom, 3);
        world.setBlockState(ORIGIN.south(5), pathTop, 3);
        world.setBlockState(ORIGIN.south(6).down(), Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(ORIGIN.south(6), defaultState(ModBlocks.TURF), 3);
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

    private static void verifyTurfSupportAndSheep(WorldServer world) {
        BlockPos turf = ORIGIN.south(2);
        world.setBlockState(turf.down(), Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(turf, defaultState(ModBlocks.TURF), 3);
        require(world.getBlockState(turf).isValidPosition(world, turf),
                "Turf rejected full dirt support");
        world.setBlockState(turf.down(), slab(ModBlocks.DIRT_SLAB,
                SlabType.BOTTOM, false), 3);
        require(!defaultState(ModBlocks.TURF).isValidPosition(world, turf),
                "Turf accepted partial support");

        BlockPos coveredDirt = turf.east(2).down();
        world.setBlockState(coveredDirt, Blocks.GRASS_BLOCK.getDefaultState(), 3);
        world.setBlockState(coveredDirt.up(), defaultState(ModBlocks.TURF), 3);
        require(world.getBlockState(coveredDirt).getBlock() == Blocks.DIRT,
                "Turf did not dirtify its grass support on placement");
        require(!GrassSpread.growTarget(world, coveredDirt),
                "Grass spread through turf to its supporting dirt");

        EntitySheep sheep = new EntitySheep(world);
        EntityJoinWorldEvent join = new EntityJoinWorldEvent(sheep, world);
        CommonEvents.addTurfEatingTask(join);
        CommonEvents.addTurfEatingTask(join);
        int turfTasks = 0;
        for (EntityAITasks.EntityAITaskEntry entry : sheep.tasks.taskEntries) {
            if (entry.action instanceof TurfEatingAI) ++turfTasks;
        }
        require(turfTasks == 1, "Sheep received duplicate turf eating tasks");
        require(((Block) ModBlocks.TURF).getFlammability(defaultState(ModBlocks.TURF), world,
                turf, EnumFacing.UP) > 0, "Turf is not flammable");
    }

    private static void verifyRecipes(WorldServer world) {
        String[] names = {"dirt_slab", "grass_slab", "grass_block_from_seeds",
                "grass_slab_from_seeds", "turf"};
        for (String name : names) {
            require(world.getRecipeManager().getRecipe(id(name)) != null,
                    "Recipe did not load: " + name);
        }
        IRecipe recipe = world.getRecipeManager().getRecipe(id("turf"));
        require(!recipe.isDynamic() && recipe.getSerializer() == TurfCuttingRecipe.SERIALIZER,
                "Turf recipe is not visible or has the wrong serializer");

        InventoryCrafting crafting = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 2, 2);
        crafting.setInventorySlotContents(0, new ItemStack(ModBlocks.GRASS_SLAB));
        ItemStack shovel = new ItemStack(Items.DIAMOND_SHOVEL);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("probe", "retained");
        shovel.setTag(tag);
        shovel.setDamage(17);
        crafting.setInventorySlotContents(1, shovel);
        require(recipe.matches(crafting, world)
                && recipe.getCraftingResult(crafting).getItem() == item(ModBlocks.TURF),
                "Turf recipe did not match a grass slab and shovel");
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(crafting);
        require(remaining.get(0).getItem() == item(ModBlocks.DIRT_SLAB)
                && remaining.get(1).getItem() == Items.DIAMOND_SHOVEL
                && remaining.get(1).getCount() == 1
                && remaining.get(1).getDamage() == 17
                && remaining.get(1).hasTag()
                && "retained".equals(remaining.get(1).getTag().getString("probe")),
                "Turf recipe changed its shovel or returned the wrong soil: "
                        + remaining + ", damage=" + remaining.get(1).getDamage()
                        + ", tag=" + remaining.get(1).getTag());
    }

    private static void verifyDrops(WorldServer world) {
        NonNullList<ItemStack> grassDrops = NonNullList.create();
        ((Block) ModBlocks.GRASS_SLAB).getDrops(snowySlab(ModBlocks.GRASS_SLAB,
                SlabType.BOTTOM, false, false), grassDrops, world, ORIGIN, 0);
        require(grassDrops.size() == 1
                && grassDrops.get(0).getItem() == item(ModBlocks.DIRT_SLAB),
                "Grass slab normal drop changed");
        NonNullList<ItemStack> pathDrops = NonNullList.create();
        ((Block) ModBlocks.PATH_SLAB).getDrops(slab(ModBlocks.PATH_SLAB,
                SlabType.TOP, false), pathDrops, world, ORIGIN, 0);
        require(pathDrops.size() == 1
                && pathDrops.get(0).getItem() == item(ModBlocks.DIRT_SLAB),
                "Path slab drop changed");
    }

    private static int verifyWorldGeneration(WorldServer world) {
        int skySlabs = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int chunkZ = 24; chunkZ < 29; ++chunkZ) {
            for (int chunkX = 24; chunkX < 29; ++chunkX) {
                IChunk chunk = world.getChunk(chunkX, chunkZ);
                int startX = chunkX << 4;
                int startZ = chunkZ << 4;
                for (int localZ = 0; localZ < 16; ++localZ) {
                    for (int localX = 0; localX < 16; ++localX) {
                        for (int y = 1; y < 255; ++y) {
                            if (chunk.getBlockState(pos.setPos(startX + localX, y,
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

    private static void verifyReload(WorldServer world) {
        world.getChunk(0, 0);
        require(world.getBlockState(ORIGIN.south(4)).getBlock() == ModBlocks.GRASS_SLAB
                && world.getBlockState(ORIGIN.south(4)).get(BlockSlab.TYPE) == SlabType.BOTTOM,
                "Grass slab changed across reload");
        require(world.getBlockState(ORIGIN.south(5)).getBlock() == ModBlocks.PATH_SLAB
                && world.getBlockState(ORIGIN.south(5)).get(BlockSlab.TYPE) == SlabType.TOP,
                "Path slab changed across reload");
        require(world.getBlockState(ORIGIN.south(6)).getBlock() == ModBlocks.TURF,
                "Turf changed across reload");
        require(ModWorldState.get(world) != null && ModWorldState.SCHEMA_VERSION == 1,
                "World state changed across reload");
    }

    private static void verifyForwardFixture(WorldServer world, String minecraft,
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

        BlockPos origin = new BlockPos(8, 65, 8);
        verifyFixtureSlab(world, origin, ModBlocks.DIRT_SLAB, SlabType.TOP);
        verifyFixtureSlab(world, origin.east(), ModBlocks.DIRT_SLAB, SlabType.BOTTOM);
        verifyFixtureSlab(world, origin.east(2), ModBlocks.GRASS_SLAB, SlabType.TOP);
        verifyFixtureSlab(world, origin.east(3), ModBlocks.GRASS_SLAB, SlabType.BOTTOM);
        verifyFixtureSlab(world, origin.east(4), ModBlocks.PATH_SLAB, SlabType.TOP);
        verifyFixtureSlab(world, origin.east(5), ModBlocks.PATH_SLAB, SlabType.BOTTOM);
        require(world.getBlockState(origin.east(6)).getBlock() == ModBlocks.TURF,
                "Forward fixture turf was lost");

        TileEntity entity = world.getTileEntity(origin.south(2));
        require(entity instanceof IInventory, "Forward fixture chest was lost");
        IInventory chest = (IInventory) entity;
        verifyFixtureStack(chest.getStackInSlot(0), ModBlocks.DIRT_SLAB, 2);
        verifyFixtureStack(chest.getStackInSlot(1), ModBlocks.GRASS_SLAB, 3);
        require(chest.getStackInSlot(1).hasTag()
                && "retained".equals(chest.getStackInSlot(1).getTag().getString("fixture")),
                "Forward fixture stack NBT was lost");
        verifyFixtureStack(chest.getStackInSlot(2), ModBlocks.PATH_SLAB, 4);
        verifyFixtureStack(chest.getStackInSlot(3), ModBlocks.TURF, 5);

        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class,
                new AxisAlignedBB(origin.add(2, 0, 0), origin.add(7, 4, 5)));
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

    private static SylvesterCounts auditSylvester(Path worldDirectory) throws IOException {
        Map<Integer, String> legacyBlocks = savedBlockNames(worldDirectory);
        SylvesterCounts counts = new SylvesterCounts();
        for (String dimension : new String[] {"", "DIM-1", "DIM1"}) {
            File directory = dimension.isEmpty() ? worldDirectory.toFile()
                    : worldDirectory.resolve(dimension).toFile();
            for (int[] chunk : existingChunks(new File(directory, "region"))) {
                DataInputStream input = RegionFileCache.getChunkInputStream(
                        directory, chunk[0], chunk[1]);
                if (input == null) {
                    throw new IOException("Could not read Sylvester chunk " + chunk[0] + "," +
                            chunk[1] + " in " + dimension);
                }
                NBTTagCompound root;
                try {
                    root = CompressedStreamTools.read(input);
                } finally {
                    input.close();
                }
                NBTTagCompound level = root.getCompound("Level");
                countSavedBlocks(level, legacyBlocks, counts);
                countStacks(level.getList("TileEntities", 10), counts);
                countStacks(level.getList("Entities", 10), counts);
                ++counts.chunks;
            }
        }
        RegionFileCache.clearRegionFileReferences();
        LOGGER.info("Sylvester audit: {} chunks, grass={}/{}, dirt={}/{}, items={}/{}/{}/{}",
                counts.chunks, counts.grassTop, counts.grassBottom, counts.dirtTop,
                counts.dirtBottom, counts.grassItems, counts.dirtItems, counts.pathItems,
                counts.turfItems);
        return counts;
    }

    private static List<int[]> existingChunks(File regionDirectory) throws IOException {
        List<int[]> chunks = new ArrayList<int[]>();
        File[] files = regionDirectory.listFiles();
        if (files == null) return chunks;
        for (File file : files) {
            String name = file.getName();
            if (!name.startsWith("r.") || !name.endsWith(".mca")) continue;
            String[] coordinates = name.substring(2, name.length() - 4).split("\\.");
            if (coordinates.length != 2) continue;
            int regionX = Integer.parseInt(coordinates[0]);
            int regionZ = Integer.parseInt(coordinates[1]);
            try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
                for (int index = 0; index < 1024; ++index) {
                    if (input.readInt() != 0) {
                        chunks.add(new int[] {regionX * 32 + (index & 31),
                                regionZ * 32 + (index >> 5)});
                    }
                }
            }
        }
        Collections.sort(chunks, new Comparator<int[]>() {
            @Override
            public int compare(int[] left, int[] right) {
                int x = Integer.compare(left[0], right[0]);
                return x == 0 ? Integer.compare(left[1], right[1]) : x;
            }
        });
        return chunks;
    }

    private static Map<Integer, String> savedBlockNames(Path worldDirectory) throws IOException {
        NBTTagCompound root;
        try (FileInputStream input = new FileInputStream(
                worldDirectory.resolve("level.dat").toFile())) {
            root = CompressedStreamTools.readCompressed(input);
        }
        NBTTagCompound data = root.getCompound("Data");
        NBTTagCompound fml = data.getCompound("FML");
        if (!fml.contains("Registries", 10)) fml = root.getCompound("FML");
        NBTTagCompound registries = fml.getCompound("Registries");
        Map<Integer, String> names = new LinkedHashMap<Integer, String>();
        for (String registry : new String[] {"minecraft:blocks", "fml:blocks"}) {
            NBTTagList ids = registries.getCompound(registry).getList("ids", 10);
            for (int index = 0; index < ids.size(); ++index) {
                NBTTagCompound entry = ids.getCompound(index);
                names.put(Integer.valueOf(entry.getInt("V")), entry.getString("K"));
            }
        }
        return names;
    }

    private static void countSavedBlocks(NBTTagCompound level,
            Map<Integer, String> legacyBlocks, SylvesterCounts counts) {
        NBTTagList sections = level.getList("Sections", 10);
        for (int sectionIndex = 0; sectionIndex < sections.size(); ++sectionIndex) {
            NBTTagCompound section = sections.getCompound(sectionIndex);
            if (section.contains("Palette", 9)) {
                countFlattenedSection(section, counts);
            } else {
                countLegacySection(section, legacyBlocks, counts);
            }
        }
    }

    private static void countLegacySection(NBTTagCompound section,
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

    private static void countFlattenedSection(NBTTagCompound section, SylvesterCounts counts) {
        NBTTagList palette = section.getList("Palette", 10);
        if (palette.isEmpty()) return;
        String[] names = new String[palette.size()];
        boolean[] top = new boolean[palette.size()];
        for (int index = 0; index < palette.size(); ++index) {
            NBTTagCompound entry = palette.getCompound(index);
            names[index] = entry.getString("Name");
            NBTTagCompound properties = entry.getCompound("Properties");
            top[index] = "top".equals(properties.getString("type"));
        }
        long[] packed = section.getLongArray("BlockStates");
        if (palette.size() == 1 && packed.length == 0) {
            for (int index = 0; index < 4096; ++index) countBlock(names[0], top[0], counts);
            return;
        }
        int bits = 4;
        while ((1 << bits) < palette.size()) ++bits;
        BitArray values = new BitArray(bits, 4096, packed);
        for (int index = 0; index < 4096; ++index) {
            int paletteIndex = values.getAt(index);
            if (paletteIndex >= 0 && paletteIndex < names.length) {
                countBlock(names[paletteIndex], top[paletteIndex], counts);
            }
        }
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

    private static void countStacks(INBTBase tag, SylvesterCounts counts) {
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) tag;
            if (compound.contains("id", 8) && compound.contains("Count", 99)) {
                int count = compound.getByte("Count") & 255;
                String id = compound.getString("id");
                if ("skysgrassslabs:grass_slab".equals(id)) counts.grassItems += count;
                else if ("skysgrassslabs:dirt_slab".equals(id)) counts.dirtItems += count;
                else if ("skysgrassslabs:path_slab".equals(id)) counts.pathItems += count;
                else if ("skysgrassslabs:turf".equals(id)) counts.turfItems += count;
            }
            for (String key : new ArrayList<String>(compound.keySet())) {
                INBTBase child = compound.getTag(key);
                if (child != null) countStacks(child, counts);
            }
        } else if (tag instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) tag;
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

    private static void verifyFixtureSlab(World world, BlockPos pos, Block expected,
            SlabType type) {
        IBlockState state = world.getBlockState(pos);
        require(state.getBlock() == expected && state.get(BlockSlab.TYPE) == type
                && !state.get(BlockSlab.WATERLOGGED),
                "Forward fixture slab changed at " + pos + ": " + state);
    }

    private static void verifyFixtureStack(ItemStack stack, Block expected, int count) {
        require(stack.getItem() == expected.asItem() && stack.getCount() == count,
                "Forward fixture stack changed: " + stack);
    }

    private static IBlockState slab(Block block, SlabType type, boolean waterlogged) {
        return defaultState(block).with(BlockSlab.TYPE, type)
                .with(BlockSlab.WATERLOGGED, waterlogged);
    }

    private static IBlockState defaultState(Block block) {
        return block.getDefaultState();
    }

    private static net.minecraft.item.Item item(Block block) {
        return block.asItem();
    }

    private static IBlockState snowySlab(Block block, SlabType type,
            boolean waterlogged, boolean snowy) {
        return slab(block, type, waterlogged).with(BlockDirtSnowy.SNOWY, snowy);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("skysgrassslabs", path);
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) < 0.000001D;
    }

    private static void clearProbe(World world) {
        for (int x = ORIGIN.getX() - 1; x <= ORIGIN.getX() + 15; ++x) {
            for (int z = ORIGIN.getZ() - 1; z <= ORIGIN.getZ() + 8; ++z) {
                for (int y = ORIGIN.getY() - 2; y <= ORIGIN.getY() + 2; ++y) {
                    world.removeBlock(new BlockPos(x, y, z));
                }
            }
        }
    }

    private static Path worldRoot(MinecraftServer server) {
        return server.getActiveAnvilConverter().getFile(server.getFolderName(), "level.dat")
                .toPath().toAbsolutePath().normalize().getParent();
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
            values.store(output, "Sky's Grass Slabs Forge 1.13.2 integration evidence");
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
