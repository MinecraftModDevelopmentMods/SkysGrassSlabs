package zone.moddev.mc.skysgrassslabs.integrationtest;

import com.mojang.authlib.GameProfile;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.block.PathSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.TurfBlock;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.entity.ai.TurfEatingAI;
import zone.moddev.mc.skysgrassslabs.event.CommonEvents;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.recipe.TurfCuttingRecipe;
import zone.moddev.mc.skysgrassslabs.world.GrassSlabSmoothingHandler;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Build-only runtime probe. This source set is never included in release artifacts. */
@Mod(modid = IntegrationTestMod.MOD_ID, name = "Sky's Grass Slabs Integration Test",
        version = "1", dependencies = "required-after:skysgrassslabs")
public final class IntegrationTestMod {
    public static final String MOD_ID = "skysgrassslabsintegrationtest";
    private static final String MARKER_NAME = "skysgrassslabs-integration.properties";
    private static final int MIGRATION_BATCH_SIZE = 256;

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer world = server.worldServerForDimension(0);
        File marker = new File(world.getSaveHandler().getWorldDirectory(), MARKER_NAME);
        String phase = System.getProperty("skysgrassslabs.integrationPhase", "fresh");
        try {
            Properties evidence = load(marker);
            if ("fresh".equals(phase)) {
                int gameplayChecks = verifyGameplay(server, world);
                int worldgenChecks = verifyWorldgen(world);
                evidence.setProperty("gameplay_checks", Integer.toString(gameplayChecks));
                evidence.setProperty("worldgen_checks", Integer.toString(worldgenChecks));
                if (Loader.isModLoaded(BuildingBricksCompat.MOD_ID)) {
                    seedLegacyCompatibilityFixture(world);
                    verifyLegacyCompatibilityHooks(server, world, false);
                    verifyLegacyBridgeRecipes(world);
                    int takeoverSlabs = verifyLegacyWorldgenTakeover(world);
                    evidence.setProperty("compat_fixture_seeded", "true");
                    evidence.setProperty("takeover_sky_slabs", Integer.toString(takeoverSlabs));
                    evidence.setProperty("takeover_complete", "true");
                }
                evidence.setProperty("fresh_complete", "true");
            } else if ("reload".equals(phase)) {
                require("true".equals(evidence.getProperty("fresh_complete")),
                        "Fresh integration evidence was not retained");
                require(ModWorldState.SCHEMA_VERSION == 1,
                        "World-state schema changed during reload");
                require(ModWorldState.get(world) != null, "World state did not reload");
                if ("true".equals(evidence.getProperty("compat_fixture_seeded"))) {
                    require(!SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs(),
                            "Default coexistence reload unexpectedly enabled replacement");
                    verifyLegacyCompatibilityFixture(world, false);
                    verifyLegacyCompatibilityHooks(server, world, false);
                    require(ModWorldState.get(world).migratedChunks() == 0,
                            "Disabled replacement marked or counted a chunk");
                    evidence.setProperty("compat_retention_complete", "true");
                }
                evidence.setProperty("reload_complete", "true");
            } else if ("compat-replacement".equals(phase)) {
                require("true".equals(evidence.getProperty("compat_retention_complete")),
                        "Default coexistence was not verified before forced replacement");
                require(SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs(),
                        "Compatibility replacement phase requires the force option");
                verifyLegacyCompatibilityFixture(world, true);
                verifyLegacyCompatibilityHooks(server, world, true);
                ModWorldState state = ModWorldState.get(world);
                require(state.migratedGrassBlocks() >= 3 && state.migratedDirtBlocks() >= 3,
                        "Forced replacement did not count loaded and newly placed slabs");
                require(state.migratedGrassItems() >= 4 && state.migratedDirtItems() >= 6,
                        "Forced replacement did not count container, entity and player items");
                evidence.setProperty("compat_replacement_complete", "true");
            } else if ("coexistence".equals(phase)) {
                require(Loader.isModLoaded(BuildingBricksCompat.MOD_ID),
                        "Default coexistence requires BuildingBricks");
                require(!SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs(),
                        "Default coexistence requires replacement to remain disabled");
                LegacyContentCounts counts = auditExistingLegacyContent(server, true);
                verifySylvesterContent(counts, "BuildingBricks");
                verifyLegacyContentSamples(server, counts, true);
                ModWorldState state = ModWorldState.get(world);
                require(state.migratedChunks() == 0 && state.migratedGrassBlocks() == 0 &&
                        state.migratedDirtBlocks() == 0 && state.migratedGrassItems() == 0 &&
                        state.migratedDirtItems() == 0,
                        "Disabled replacement changed migration counters");
                require(!migrationReport(world).exists(),
                        "Disabled replacement created a migration report");
                verifyLegacyBridgeRecipes(world);
                int takeoverSlabs = verifyLegacyWorldgenTakeover(world);
                writeLegacyContentEvidence(evidence, "retained", counts);
                evidence.setProperty("takeover_sky_slabs", Integer.toString(takeoverSlabs));
                evidence.setProperty("takeover_complete", "true");
                evidence.setProperty("coexistence_complete", "true");
            } else if ("missing-mapping".equals(phase)) {
                require(!Loader.isModLoaded(BuildingBricksCompat.MOD_ID),
                        "Missing mapping recovery requires BuildingBricks to be absent");
                LegacyContentCounts counts = auditExistingLegacyContent(server, false);
                verifySylvesterContent(counts, "Sky");
                verifyLegacyContentSamples(server, counts, false);
                ModWorldState state = ModWorldState.get(world);
                require(state.migratedChunks() == 0 && state.migratedGrassBlocks() == 0 &&
                        state.migratedDirtBlocks() == 0 && state.migratedGrassItems() == 0 &&
                        state.migratedDirtItems() == 0,
                        "Missing mapping recovery incorrectly counted forced migration");
                require(!migrationReport(world).exists(),
                        "Missing mapping recovery created a forced migration report");
                writeLegacyContentEvidence(evidence, "remapped", counts);
                evidence.setProperty("missing_mapping_complete", "true");
            } else if ("migration".equals(phase)) {
                require(SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs(),
                        "Sylvester migration requires compat.forceReplaceBuildingBricksSlabs=true");
                int chunks = migrateExistingChunks(server);
                ModWorldState state = ModWorldState.get(world);
                require(state.migratedGrassBlocks() == 1656276L,
                        "Unexpected Sylvester grass-slab total " + state.migratedGrassBlocks());
                require(state.migratedDirtBlocks() == 2968L,
                        "Unexpected Sylvester dirt-slab total " + state.migratedDirtBlocks());
                require(state.migratedGrassBlocks() == state.migratedGrassBlocksTop() +
                        state.migratedGrassBlocksBottom(),
                        "Grass-slab orientation totals do not add up");
                require(state.migratedDirtBlocks() == state.migratedDirtBlocksTop() +
                        state.migratedDirtBlocksBottom(),
                        "Dirt-slab orientation totals do not add up");
                writeMigrationEvidence(evidence, chunks, state);
                if ("true".equals(evidence.getProperty("coexistence_complete"))) {
                    evidence.setProperty("replacement_transition_verified", "true");
                }
                evidence.setProperty("migration_complete", "true");
            } else if ("migration-reload".equals(phase)) {
                require("true".equals(evidence.getProperty("migration_complete")),
                        "Migration evidence was not retained");
                require(SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs(),
                        "Sylvester migration reload requires forced replacement to remain enabled");
                ModWorldState before = ModWorldState.get(world);
                long grass = before.migratedGrassBlocks();
                long dirt = before.migratedDirtBlocks();
                long grassItems = before.migratedGrassItems();
                long dirtItems = before.migratedDirtItems();
                long unsupported = unsupportedTotal(before);
                int chunks = migrateExistingChunks(server);
                ModWorldState after = ModWorldState.get(world);
                require(grass == after.migratedGrassBlocks() &&
                        dirt == after.migratedDirtBlocks() &&
                        grassItems == after.migratedGrassItems() &&
                        dirtItems == after.migratedDirtItems() &&
                        unsupported == unsupportedTotal(after),
                        "Second Sylvester load performed additional conversions");
                evidence.setProperty("migration_reload_chunks", Integer.toString(chunks));
                evidence.setProperty("migration_reload_complete", "true");
            } else {
                throw new IllegalStateException("Unknown integration phase " + phase);
            }
            store(marker, evidence);
        } catch (Throwable failure) {
            SkysGrassSlabs.logger.error("Sky's Grass Slabs integration audit failed", failure);
            throw new RuntimeException(failure);
        } finally {
            server.initiateShutdown();
        }
    }

    private static int migrateExistingChunks(MinecraftServer server) throws Exception {
        int total = 0;
        for (int dimension : new int[] {0, -1, 1}) {
            WorldServer world = server.worldServerForDimension(dimension);
            if (world == null) continue;
            File dimensionDirectory = dimension == 0
                    ? world.getSaveHandler().getWorldDirectory()
                    : new File(world.getSaveHandler().getWorldDirectory(),
                            world.provider.getSaveFolder());
            List<int[]> chunks = existingChunks(new File(dimensionDirectory, "region"));
            total += chunks.size();
            migrateChunks(world, chunks, dimension);
        }
        return total;
    }

    private static LegacyContentCounts auditExistingLegacyContent(MinecraftServer server,
            boolean expectBuildingBricks) throws Exception {
        LegacyContentCounts counts = new LegacyContentCounts();
        Block grass = expectBuildingBricks ? legacyBlock(true) : ModBlocks.GRASS_SLAB;
        Block dirt = expectBuildingBricks ? legacyBlock(false) : ModBlocks.DIRT_SLAB;
        for (int dimension : new int[] {0, -1, 1}) {
            WorldServer world = server.worldServerForDimension(dimension);
            if (world == null) continue;
            File dimensionDirectory = dimension == 0
                    ? world.getSaveHandler().getWorldDirectory()
                    : new File(world.getSaveHandler().getWorldDirectory(),
                            world.provider.getSaveFolder());
            List<int[]> chunks = existingChunks(new File(dimensionDirectory, "region"));
            counts.chunks += chunks.size();
            auditSavedChunks(dimensionDirectory, chunks, dimension, grass, dirt, counts);
        }
        return counts;
    }

    private static List<int[]> existingChunks(File regionDirectory) throws Exception {
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
            RandomAccessFile input = new RandomAccessFile(file, "r");
            try {
                for (int index = 0; index < 1024; ++index) {
                    if (input.readInt() != 0) {
                        chunks.add(new int[] {regionX * 32 + (index & 31),
                                regionZ * 32 + (index >> 5)});
                    }
                }
            } finally {
                input.close();
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

    private static void migrateChunks(WorldServer world, List<int[]> coordinates,
            int dimension) {
        ChunkProviderServer provider = world.getChunkProvider();
        List<Chunk> batch = new ArrayList<Chunk>(MIGRATION_BATCH_SIZE);
        int processed = 0;
        for (int[] coordinate : coordinates) {
            Chunk chunk = loadChunk(provider, coordinate[0], coordinate[1]);
            require(chunk != null, "Could not load existing chunk " + coordinate[0] + "," +
                    coordinate[1] + " in dimension " + dimension);
            batch.add(chunk);
            if (batch.size() == MIGRATION_BATCH_SIZE) {
                flushAndUnload(provider, batch);
            }
            if (++processed % 1000 == 0) {
                SkysGrassSlabs.logger.info("Sylvester migration dimension {}: {}/{} chunks",
                        dimension, processed, coordinates.size());
            }
        }
        flushAndUnload(provider, batch);
        saveChunks(provider);
        SkysGrassSlabs.logger.info("Sylvester migration dimension {} complete: {} chunks",
                dimension, coordinates.size());
    }

    private static void auditSavedChunks(File dimensionDirectory, List<int[]> coordinates,
            int dimension, Block grass, Block dirt, LegacyContentCounts counts) throws Exception {
        int grassId = Block.getIdFromBlock(grass);
        int dirtId = Block.getIdFromBlock(dirt);
        int processed = 0;
        for (int[] coordinate : coordinates) {
            DataInputStream input = RegionFileCache.getChunkInputStream(dimensionDirectory,
                    coordinate[0], coordinate[1]);
            require(input != null, "Could not read existing chunk " + coordinate[0] + "," +
                    coordinate[1] + " in dimension " + dimension);
            NBTTagCompound serialized;
            try {
                serialized = CompressedStreamTools.read(input);
            } finally {
                input.close();
            }
            auditSavedBlocks(serialized, dimension, coordinate[0], coordinate[1],
                    grassId, dirtId, counts);
            NBTTagCompound level = serialized.getCompoundTag("Level");
            countStacks(level.getTagList("TileEntities", 10),
                    BuildingBricksCompat.GRASS_SLAB_ID.toString(),
                    BuildingBricksCompat.DIRT_SLAB_ID.toString(), counts,
                    dimension, coordinate[0], coordinate[1]);
            countStacks(level.getTagList("Entities", 10),
                    BuildingBricksCompat.GRASS_SLAB_ID.toString(),
                    BuildingBricksCompat.DIRT_SLAB_ID.toString(), counts,
                    dimension, coordinate[0], coordinate[1]);
            if (++processed % 1000 == 0) {
                SkysGrassSlabs.logger.info("Sylvester saved content audit dimension {}: {}/{} chunks",
                        dimension, processed, coordinates.size());
            }
        }
        RegionFileCache.clearRegionFileReferences();
        SkysGrassSlabs.logger.info("Sylvester saved content audit dimension {} complete: {} chunks",
                dimension, coordinates.size());
    }

    private static void auditSavedBlocks(NBTTagCompound serialized, int dimension, int chunkX,
            int chunkZ, int grassId, int dirtId, LegacyContentCounts counts) {
        NBTTagList sections = serialized.getCompoundTag("Level").getTagList("Sections", 10);
        for (int sectionIndex = 0; sectionIndex < sections.tagCount(); ++sectionIndex) {
            NBTTagCompound section = sections.getCompoundTagAt(sectionIndex);
            int ySection = section.getByte("Y") & 255;
            byte[] blocks = section.getByteArray("Blocks");
            if (blocks.length != 4096) continue;
            NibbleArray metadata = new NibbleArray(section.getByteArray("Data"));
            NibbleArray add = section.hasKey("Add", 7)
                    ? new NibbleArray(section.getByteArray("Add")) : null;
            for (int index = 0; index < blocks.length; ++index) {
                int numericId = (blocks[index] & 255) |
                        (add == null ? 0 : add.getFromIndex(index) << 8);
                if (numericId != grassId && numericId != dirtId) continue;
                int slabMetadata = metadata.getFromIndex(index) & 1;
                if (numericId == grassId) {
                    if (slabMetadata == 0) ++counts.grassTop;
                    else ++counts.grassBottom;
                    if (counts.grassSample == null) {
                        counts.grassSample = samplePosition(dimension, chunkX, chunkZ,
                                ySection, index, slabMetadata);
                    }
                } else {
                    if (slabMetadata == 0) ++counts.dirtTop;
                    else ++counts.dirtBottom;
                    if (counts.dirtSample == null) {
                        counts.dirtSample = samplePosition(dimension, chunkX, chunkZ,
                                ySection, index, slabMetadata);
                    }
                }
            }
        }
    }

    private static LegacyBlockSample samplePosition(int dimension, int chunkX, int chunkZ,
            int ySection, int index, int metadata) {
        int x = (chunkX << 4) + (index & 15);
        int y = (ySection << 4) + (index >> 8 & 15);
        int z = (chunkZ << 4) + (index >> 4 & 15);
        return new LegacyBlockSample(dimension, new BlockPos(x, y, z), metadata);
    }

    private static void auditItems(Chunk chunk, String grassId, String dirtId,
            LegacyContentCounts counts) {
        for (TileEntity tileEntity : chunk.getTileEntityMap().values()) {
            countStacks(tileEntity.writeToNBT(new NBTTagCompound()), grassId, dirtId, counts,
                    chunk.getWorld().provider.getDimension(), chunk.xPosition, chunk.zPosition);
        }
        for (ClassInheritanceMultiMap<Entity> list : chunk.getEntityLists()) {
            for (Entity entity : list) {
                countStacks(entity.serializeNBT(), grassId, dirtId, counts,
                        chunk.getWorld().provider.getDimension(), chunk.xPosition, chunk.zPosition);
            }
        }
    }

    private static void countStacks(NBTBase tag, String grassId,
            String dirtId, LegacyContentCounts counts, int dimension, int chunkX, int chunkZ) {
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) tag;
            if (compound.hasKey("id", 8) && compound.hasKey("Count", 99)) {
                String id = compound.getString("id");
                int count = compound.getByte("Count") & 255;
                if (grassId.equals(id)) {
                    counts.grassItems += count;
                    if (counts.itemSample == null) {
                        counts.itemSample = new LegacyChunkSample(dimension, chunkX, chunkZ);
                    }
                }
                if (dirtId.equals(id)) {
                    counts.dirtItems += count;
                    if (counts.itemSample == null) {
                        counts.itemSample = new LegacyChunkSample(dimension, chunkX, chunkZ);
                    }
                }
            }
            for (String key : new ArrayList<String>(compound.getKeySet())) {
                NBTBase child = compound.getTag(key);
                if (child != null) {
                    countStacks(child, grassId, dirtId, counts, dimension, chunkX, chunkZ);
                }
            }
        } else if (tag instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) tag;
            for (int index = 0; index < list.tagCount(); ++index) {
                countStacks(list.get(index), grassId, dirtId, counts,
                        dimension, chunkX, chunkZ);
            }
        }
    }

    private static Chunk loadChunk(ChunkProviderServer provider, int x, int z) {
        return provider.loadChunk(x, z);
    }

    private static void flushAndUnload(ChunkProviderServer provider, List<Chunk> chunks) {
        if (chunks.isEmpty()) return;
        saveChunks(provider);
        for (Chunk chunk : chunks) provider.unload(chunk);
        for (int remaining = chunks.size(); remaining > 0; remaining -= 100) {
            tickProvider(provider);
        }
        drainChunkWrites();
        chunks.clear();
    }

    private static void saveChunks(ChunkProviderServer provider) {
        provider.saveChunks(true);
    }

    private static void tickProvider(ChunkProviderServer provider) {
        provider.tick();
    }

    private static void drainChunkWrites() {
        try {
            ThreadedFileIOBase.getThreadedIOInstance().waitForFinish();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while draining chunk writes", exception);
        }
    }

    private static long unsupportedTotal(ModWorldState state) {
        long total = 0;
        for (Long count : state.unsupported().values()) total += count;
        return total;
    }

    private static void verifySylvesterContent(LegacyContentCounts counts, String owner) {
        require(counts.grassTop == 0 && counts.grassBottom == 1656276L,
                owner + " grass slab total or orientation changed: " +
                counts.grassTop + " top, " + counts.grassBottom + " bottom");
        require(counts.dirtTop == 12 && counts.dirtBottom == 2956L,
                owner + " dirt slab total or orientation changed: " +
                counts.dirtTop + " top, " + counts.dirtBottom + " bottom");
        require(counts.grassItems == 0 && counts.dirtItems == 7186L,
                owner + " supported item total changed: " + counts.grassItems +
                " grass, " + counts.dirtItems + " dirt");
    }

    private static void verifyLegacyContentSamples(MinecraftServer server,
            LegacyContentCounts counts, boolean expectBuildingBricks) {
        require(counts.grassSample != null && counts.dirtSample != null &&
                counts.itemSample != null,
                "Saved legacy content did not provide live verification samples");
        Block grass = expectBuildingBricks ? legacyBlock(true) : ModBlocks.GRASS_SLAB;
        Block dirt = expectBuildingBricks ? legacyBlock(false) : ModBlocks.DIRT_SLAB;
        verifyLegacyBlockSample(server, counts.grassSample, grass, "grass");
        verifyLegacyBlockSample(server, counts.dirtSample, dirt, "dirt");

        WorldServer world = server.worldServerForDimension(counts.itemSample.dimension);
        require(world != null, "Legacy item sample dimension was unavailable");
        Chunk chunk = loadChunk(world.getChunkProvider(), counts.itemSample.chunkX,
                counts.itemSample.chunkZ);
        require(chunk != null, "Legacy item sample chunk could not be loaded");
        LegacyContentCounts live = new LegacyContentCounts();
        auditItems(chunk, grass.getRegistryName().toString(), dirt.getRegistryName().toString(), live);
        require(live.grassItems + live.dirtItems > 0,
                "Saved legacy item did not load with the selected registry identity");
    }

    private static void verifyLegacyBlockSample(MinecraftServer server, LegacyBlockSample sample,
            Block expected, String type) {
        WorldServer world = server.worldServerForDimension(sample.dimension);
        require(world != null, "Legacy " + type + " sample dimension was unavailable");
        IBlockState state = world.getBlockState(sample.position);
        require(state.getBlock() == expected &&
                (expected.getMetaFromState(state) & 1) == sample.metadata,
                "Saved legacy " + type + " sample did not load with its identity and orientation");
    }

    private static void writeLegacyContentEvidence(Properties evidence, String prefix,
            LegacyContentCounts counts) {
        evidence.setProperty(prefix + "_chunks", Integer.toString(counts.chunks));
        evidence.setProperty(prefix + "_grass_blocks_top", Long.toString(counts.grassTop));
        evidence.setProperty(prefix + "_grass_blocks_bottom", Long.toString(counts.grassBottom));
        evidence.setProperty(prefix + "_dirt_blocks_top", Long.toString(counts.dirtTop));
        evidence.setProperty(prefix + "_dirt_blocks_bottom", Long.toString(counts.dirtBottom));
        evidence.setProperty(prefix + "_grass_items", Long.toString(counts.grassItems));
        evidence.setProperty(prefix + "_dirt_items", Long.toString(counts.dirtItems));
    }

    private static File migrationReport(WorldServer world) {
        return new File(new File(world.getSaveHandler().getWorldDirectory(), "serverconfig"),
                "skysgrassslabs-migration-report.txt");
    }

    private static void writeMigrationEvidence(Properties evidence, int chunks,
            ModWorldState state) {
        evidence.setProperty("migration_chunks", Integer.toString(chunks));
        evidence.setProperty("migrated_grass_blocks",
                Long.toString(state.migratedGrassBlocks()));
        evidence.setProperty("migrated_grass_blocks_top",
                Long.toString(state.migratedGrassBlocksTop()));
        evidence.setProperty("migrated_grass_blocks_bottom",
                Long.toString(state.migratedGrassBlocksBottom()));
        evidence.setProperty("migrated_dirt_blocks",
                Long.toString(state.migratedDirtBlocks()));
        evidence.setProperty("migrated_dirt_blocks_top",
                Long.toString(state.migratedDirtBlocksTop()));
        evidence.setProperty("migrated_dirt_blocks_bottom",
                Long.toString(state.migratedDirtBlocksBottom()));
        evidence.setProperty("migrated_grass_items",
                Long.toString(state.migratedGrassItems()));
        evidence.setProperty("migrated_dirt_items",
                Long.toString(state.migratedDirtItems()));
        evidence.setProperty("unsupported_entries",
                Integer.toString(state.unsupported().size()));
        evidence.setProperty("unsupported_total", Long.toString(unsupportedTotal(state)));
    }

    private static void seedLegacyCompatibilityFixture(WorldServer world) {
        BlockPos origin = legacyCompatibilityOrigin(world);
        setLegacyBlock(world, origin, true, 0);
        setLegacyBlock(world, origin.east(), true, 1);
        setLegacyBlock(world, origin.east(2), false, 0);
        setLegacyBlock(world, origin.east(3), false, 1);

        BlockPos chestPos = origin.east(5);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 3);
        IInventory chest = (IInventory) world.getTileEntity(chestPos);
        ItemStack grassStack = legacyStack(true, 2);
        NBTTagCompound retained = new NBTTagCompound();
        retained.setString("probe", "retained");
        grassStack.setTagCompound(retained);
        chest.setInventorySlotContents(0, grassStack);
        chest.setInventorySlotContents(1, legacyStack(false, 3));
        chest.markDirty();

        EntityItem dropped = new EntityItem(world, origin.getX() + 6.5D,
                origin.getY() + 0.5D, origin.getZ() + 0.5D, legacyStack(true, 2));
        require(world.spawnEntity(dropped), "Could not seed legacy dropped slabs");
    }

    private static void verifyLegacyCompatibilityFixture(WorldServer world, boolean replaced) {
        BlockPos origin = legacyCompatibilityOrigin(world);
        Block expectedGrass = replaced ? ModBlocks.GRASS_SLAB : legacyBlock(true);
        Block expectedDirt = replaced ? ModBlocks.DIRT_SLAB : legacyBlock(false);
        require(world.getBlockState(origin).getBlock() == expectedGrass &&
                expectedGrass.getMetaFromState(world.getBlockState(origin)) == 0,
                "Top legacy grass slab did not match the selected replacement mode");
        require(world.getBlockState(origin.east()).getBlock() == expectedGrass &&
                expectedGrass.getMetaFromState(world.getBlockState(origin.east())) == 1,
                "Bottom legacy grass slab did not match the selected replacement mode");
        require(world.getBlockState(origin.east(2)).getBlock() == expectedDirt &&
                expectedDirt.getMetaFromState(world.getBlockState(origin.east(2))) == 0,
                "Top legacy dirt slab did not match the selected replacement mode");
        require(world.getBlockState(origin.east(3)).getBlock() == expectedDirt &&
                expectedDirt.getMetaFromState(world.getBlockState(origin.east(3))) == 1,
                "Bottom legacy dirt slab did not match the selected replacement mode");

        IInventory chest = (IInventory) world.getTileEntity(origin.east(5));
        require(chest != null, "Legacy compatibility chest was not retained");
        Item expectedGrassItem = Item.getItemFromBlock(expectedGrass);
        Item expectedDirtItem = Item.getItemFromBlock(expectedDirt);
        require(chest.getStackInSlot(0) != null &&
                chest.getStackInSlot(0).getItem() == expectedGrassItem &&
                chest.getStackInSlot(0).stackSize == 2 &&
                "retained".equals(chest.getStackInSlot(0).getTagCompound().getString("probe")),
                "Legacy chest grass slabs or their NBT did not match the selected mode");
        require(chest.getStackInSlot(1) != null &&
                chest.getStackInSlot(1).getItem() == expectedDirtItem &&
                chest.getStackInSlot(1).stackSize == 3,
                "Legacy chest dirt slabs did not match the selected mode");

        List<EntityItem> drops = world.getEntitiesWithinAABB(EntityItem.class,
                new AxisAlignedBB(origin.east(6)).expandXyz(2.0D));
        require(drops.size() == 1 && drops.get(0).getEntityItem() != null &&
                drops.get(0).getEntityItem().getItem() == expectedGrassItem &&
                drops.get(0).getEntityItem().stackSize == 2,
                "Dropped legacy slabs did not match the selected mode");
    }

    private static void verifyLegacyCompatibilityHooks(MinecraftServer server,
            WorldServer world, boolean replaced) {
        Block expectedGrass = replaced ? ModBlocks.GRASS_SLAB : legacyBlock(true);
        Block expectedDirt = replaced ? ModBlocks.DIRT_SLAB : legacyBlock(false);
        EntityPlayerMP player = player(server, world);
        player.inventory.setInventorySlotContents(0, legacyStack(true, 2));
        player.getInventoryEnderChest().setInventorySlotContents(0, legacyStack(false, 3));
        FMLCommonHandler.instance().bus().post(new PlayerEvent.PlayerLoggedInEvent(player));
        require(player.inventory.getStackInSlot(0).getItem() ==
                Item.getItemFromBlock(expectedGrass),
                "Player inventory did not match the selected replacement mode");
        require(player.getInventoryEnderChest().getStackInSlot(0).getItem() ==
                Item.getItemFromBlock(expectedDirt),
                "Ender chest did not match the selected replacement mode");

        BlockPos origin = legacyCompatibilityOrigin(world).south(2);
        verifyLegacyPlacementEvent(world, player, origin, true, 0, expectedGrass);
        verifyLegacyPlacementEvent(world, player, origin.east(), false, 1, expectedDirt);
    }

    private static void verifyLegacyBridgeRecipes(World world) {
        InventoryCrafting seedGrid = craftingGrid(2, 2);
        ItemStack legacyDirt = legacyStack(false, 1);
        seedGrid.setInventorySlotContents(0, legacyDirt);
        seedGrid.setInventorySlotContents(1, new ItemStack(Items.WHEAT_SEEDS));
        boolean foundSeedBridge = false;
        int bridgeRecipes = 0;
        int grassOutputs = 0;
        for (IRecipe recipe : CraftingManager.getInstance().getRecipeList()) {
            if (recipe.getClass().getName().endsWith("BuildingBricksDirtSlabRecipe")) {
                ++bridgeRecipes;
            }
            ItemStack declaredOutput = recipe.getRecipeOutput();
            if (declaredOutput != null && declaredOutput.getItem() ==
                    Item.getItemFromBlock(ModBlocks.GRASS_SLAB)) {
                ++grassOutputs;
            }
            if (recipe.matches(seedGrid, world)) {
                ItemStack output = recipe.getCraftingResult(seedGrid);
                if (output != null && output.getItem() ==
                        Item.getItemFromBlock(ModBlocks.GRASS_SLAB)) {
                    foundSeedBridge = true;
                    break;
                }
            }
        }
        require(foundSeedBridge,
                "BuildingBricks dirt slab and seed bridge recipe was not available; found " +
                bridgeRecipes + " dynamic bridge recipes and " + grassOutputs +
                " recipes declaring a grass slab output");

        IRecipe turfRecipe = new TurfCuttingRecipe();
        InventoryCrafting turfGrid = craftingGrid(2, 2);
        turfGrid.setInventorySlotContents(0, legacyStack(true, 1));
        turfGrid.setInventorySlotContents(1, new ItemStack(Items.IRON_SHOVEL));
        require(turfRecipe.matches(turfGrid, world),
                "BuildingBricks grass slab turf bridge recipe was not available");
        ItemStack[] remaining = turfRecipe.getRemainingItems(turfGrid);
        require(remaining[0] != null && remaining[0].getItem() ==
                Item.getItemFromBlock(ModBlocks.DIRT_SLAB),
                "BuildingBricks turf recipe did not return a Sky dirt slab");
        require(remaining[1] != null && remaining[1].getItem() == Items.IRON_SHOVEL,
                "BuildingBricks turf recipe did not retain its shovel");
    }

    private static void verifyLegacyPlacementEvent(WorldServer world, EntityPlayerMP player,
            BlockPos pos, boolean grass, int metadata, Block expected) {
        Block legacy = legacyBlock(grass);
        setLegacyBlock(world, pos, grass, metadata);
        player.setHeldItem(EnumHand.MAIN_HAND, legacyStack(grass, 1));
        BlockSnapshot snapshot = BlockSnapshot.getBlockSnapshot(world, pos);
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.BlockEvent.PlaceEvent(
                snapshot, Blocks.DIRT.getDefaultState(), player, EnumHand.MAIN_HAND));
        require(world.getBlockState(pos).getBlock() == expected &&
                expected.getMetaFromState(world.getBlockState(pos)) == metadata,
                "Placed legacy slab did not match the selected replacement mode");
    }

    private static Block legacyBlock(boolean grass) {
        Block block = Block.REGISTRY.getObject(grass ? BuildingBricksCompat.GRASS_SLAB_ID
                : BuildingBricksCompat.DIRT_SLAB_ID);
        require(block != null && block != Blocks.AIR,
                "BuildingBricks did not register its supported slabs");
        return block;
    }

    private static ItemStack legacyStack(boolean grass, int count) {
        ItemStack stack = new ItemStack(legacyBlock(grass), count, 0);
        if (!grass) {
            try {
                Object material = legacyDirtMaterial();
                stack.getItem().getClass().getMethod("setMaterial", ItemStack.class,
                        material.getClass()).invoke(stack.getItem(), stack, material);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Could not initialize the build-only BuildingBricks dirt item", exception);
            }
        }
        return stack;
    }

    private static void setLegacyBlock(World world, BlockPos pos, boolean grass,
            int metadata) {
        Block legacy = legacyBlock(grass);
        IBlockState state = legacy.getStateFromMeta(metadata);
        if (!grass) {
            Object material = legacyDirtMaterial();
            try {
                int materialMetadata = (Integer) material.getClass().getClassLoader()
                        .loadClass("com.hea3ven.buildingbricks.core.materials.MaterialRegistry")
                        .getMethod("getMeta", material.getClass()).invoke(null, material);
                state = legacy.getStateForPlacement(world, pos, EnumFacing.UP, 0.5F,
                        metadata == 0 ? 0.75F : 0.25F, 0.5F, materialMetadata, null)
                        .withProperty(BlockSlab.HALF, metadata == 0
                                ? BlockSlab.EnumBlockHalf.TOP
                                : BlockSlab.EnumBlockHalf.BOTTOM);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Could not prepare the build-only BuildingBricks dirt state", exception);
            }
        }
        world.setBlockState(pos, state, 3);
        if (!grass && world.getTileEntity(pos) != null) {
            try {
                Class<?> materialType = Class.forName(
                        "com.hea3ven.buildingbricks.core.materials.Material");
                Object material = legacyDirtMaterial();
                world.getTileEntity(pos).getClass().getMethod("setMaterial", materialType)
                        .invoke(world.getTileEntity(pos), material);
                world.getTileEntity(pos).markDirty();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Could not initialize the build-only BuildingBricks dirt fixture", exception);
            }
        }
    }

    private static Object legacyDirtMaterial() {
        try {
            Class<?> registry = Class.forName(
                    "com.hea3ven.buildingbricks.core.materials.MaterialRegistry");
            Object material = registry.getMethod("get", String.class)
                    .invoke(null, "minecraft:dirt");
            require(material != null, "BuildingBricks dirt material was not registered");
            return material;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Could not resolve the build-only BuildingBricks dirt material", exception);
        }
    }

    private static BlockPos legacyCompatibilityOrigin(World world) {
        return new BlockPos((world.getSpawnPoint().getX() & ~15) + 2, 220,
                (world.getSpawnPoint().getZ() & ~15) + 2);
    }

    private static int verifyLegacyWorldgenTakeover(WorldServer world) {
        require(SkysGrassSlabsConfig.generateGrassSlabs(),
                "Sky world smoothing must be enabled for the takeover check");
        File legacyConfig = new File("config/BuildingBricks/general.cfg");
        Configuration configuration = new Configuration(legacyConfig);
        configuration.load();
        require(!configuration.get("compat.vanilla", "generateGrassSlabs", true).getBoolean(),
                "BuildingBricks world generation was not disabled before new terrain generation");

        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(
                UUID.fromString("11dc17f3-dd5a-4979-a817-13b9215c0d51"),
                "GrassSlabWorldgenProbe"));
        world.getPlayerChunkMap().setPlayerViewRadius(4);
        for (int attempt = 0; attempt < 8; ++attempt) {
            int regionX = 122 + attempt * 8;
            int regionZ = 0;
            File region = new File(new File(world.getSaveHandler().getWorldDirectory(), "region"),
                    "r." + regionX + "." + regionZ + ".mca");
            require(!region.exists(), "Takeover candidate region already exists: " + region.getName());
            int centerChunkX = regionX * 32 + 16;
            int centerChunkZ = regionZ * 32 + 16;
            player.setPosition(centerChunkX * 16 + 8, 80, centerChunkZ * 16 + 8);
            world.getPlayerChunkMap().addPlayer(player);
            try {
                for (int tick = 0; tick < 200; ++tick) {
                    world.getPlayerChunkMap().tick();
                    world.getChunkProvider().tick();
                }
                int[] slabs = countGeneratedSlabs(world, centerChunkX, centerChunkZ, 4);
                require(slabs[1] == 0,
                        "BuildingBricks generated grass slabs after its generator was disabled");
                if (slabs[0] > 0) return slabs[0];
            } finally {
                world.getPlayerChunkMap().removePlayer(player);
            }
        }
        throw new IllegalStateException(
                "No eligible Sky grass slope was found in eight virgin player loaded regions");
    }

    private static int[] countGeneratedSlabs(WorldServer world, int centerChunkX,
            int centerChunkZ, int radius) {
        int sky = 0;
        int legacy = 0;
        Block legacyGrass = legacyBlock(true);
        for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; ++chunkZ) {
            for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; ++chunkX) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
                if (chunk == null) continue;
                for (ExtendedBlockStorage section : chunk.getBlockStorageArray()) {
                    if (section == null) continue;
                    for (int y = 0; y < 16; ++y) {
                        for (int z = 0; z < 16; ++z) {
                            for (int x = 0; x < 16; ++x) {
                                Block block = section.get(x, y, z).getBlock();
                                if (block == ModBlocks.GRASS_SLAB) ++sky;
                                if (block == legacyGrass) ++legacy;
                            }
                        }
                    }
                }
            }
        }
        return new int[] {sky, legacy};
    }

    private static int verifyGameplay(MinecraftServer server, WorldServer world) {
        int checks = 0;
        require(SkysGrassSlabsConfig.generateGrassSlabs(),
                "World smoothing is not enabled by default");
        require(!SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs(),
                "Installed legacy slab replacement is not disabled by default");
        checks += 2;

        require("skysgrassslabs:dirt_slab".equals(blockId(ModBlocks.DIRT_SLAB)),
                "Dirt slab registry ID changed");
        require("skysgrassslabs:grass_slab".equals(blockId(ModBlocks.GRASS_SLAB)),
                "Grass slab registry ID changed");
        require("skysgrassslabs:path_slab".equals(blockId(ModBlocks.PATH_SLAB)),
                "Path slab registry ID changed");
        require("skysgrassslabs:turf".equals(blockId(ModBlocks.TURF)),
                "Turf registry ID changed");
        checks += 4;

        require(metadata(ModBlocks.DIRT_SLAB, defaultState(ModBlocks.DIRT_SLAB)
                .withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP)) == 0,
                "Top slab metadata is not zero");
        require(metadata(ModBlocks.DIRT_SLAB, defaultState(ModBlocks.DIRT_SLAB)
                .withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM)) == 1,
                "Bottom slab metadata is not one");
        require(stateFromMetadata(ModBlocks.DIRT_SLAB, 0).getValue(BlockSlab.HALF)
                == BlockSlab.EnumBlockHalf.TOP, "Metadata zero did not restore top");
        require(stateFromMetadata(ModBlocks.DIRT_SLAB, 1).getValue(BlockSlab.HALF)
                == BlockSlab.EnumBlockHalf.BOTTOM, "Metadata one did not restore bottom");
        checks += 4;

        require(close(PathSlabBlock.BOTTOM_PATH_AABB.maxY, 7.0D / 16.0D),
                "Bottom path geometry changed");
        require(close(PathSlabBlock.TOP_PATH_AABB.minY, 8.0D / 16.0D) &&
                close(PathSlabBlock.TOP_PATH_AABB.maxY, 15.0D / 16.0D),
                "Top path geometry changed");
        require(close(TurfBlock.TURF_AABB.maxY, 1.0D / 16.0D),
                "Turf geometry changed");
        checks += 3;

        BlockPos origin = new BlockPos((world.getSpawnPoint().getX() & ~15) + 6, 160,
                (world.getSpawnPoint().getZ() & ~15) + 6);
        world.getChunkFromBlockCoords(origin);
        EntityPlayerMP player = player(server, world);
        Item dirtItem = Item.getItemFromBlock(ModBlocks.DIRT_SLAB);
        ItemStack dirtStack = new ItemStack(ModBlocks.DIRT_SLAB);
        world.setBlockState(origin, defaultState(ModBlocks.DIRT_SLAB), 3);
        EnumActionResult combined = useItem(dirtItem, dirtStack, player, world, origin,
                EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        require(combined == EnumActionResult.SUCCESS && world.getBlockState(origin).getBlock() == Blocks.DIRT &&
                dirtStack.stackSize == 0, "Dirt slabs did not normalize to vanilla dirt");
        checks++;

        Item grassItem = Item.getItemFromBlock(ModBlocks.GRASS_SLAB);
        ItemStack grassStack = new ItemStack(ModBlocks.GRASS_SLAB);
        world.setBlockState(origin, defaultState(ModBlocks.GRASS_SLAB)
                .withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP), 3);
        combined = useItem(grassItem, grassStack, player, world, origin, EnumHand.MAIN_HAND,
                EnumFacing.DOWN, 0.5F, 0.0F, 0.5F);
        require(combined == EnumActionResult.SUCCESS && world.getBlockState(origin).getBlock() == Blocks.GRASS,
                "Grass slabs did not normalize to vanilla grass");
        checks++;

        Item pathItem = Item.getItemFromBlock(ModBlocks.PATH_SLAB);
        ItemStack pathStack = new ItemStack(ModBlocks.PATH_SLAB);
        world.setBlockState(origin, defaultState(ModBlocks.PATH_SLAB), 3);
        combined = useItem(pathItem, pathStack, player, world, origin, EnumHand.MAIN_HAND,
                EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        require(combined == EnumActionResult.SUCCESS && world.getBlockState(origin).getBlock() == Blocks.GRASS_PATH,
                "Path slabs did not normalize to vanilla grass path");
        checks++;

        Item turfItem = Item.getItemFromBlock(ModBlocks.TURF);
        for (BlockSlab.EnumBlockHalf half : BlockSlab.EnumBlockHalf.values()) {
            ItemStack turfStack = new ItemStack(ModBlocks.TURF);
            world.setBlockState(origin, defaultState(ModBlocks.DIRT_SLAB)
                    .withProperty(BlockSlab.HALF, half), 3);
            EnumActionResult result = useItem(turfItem, turfStack, player, world, origin,
                    EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
            require(result == EnumActionResult.SUCCESS && turfStack.stackSize == 0 &&
                    world.getBlockState(origin).getBlock() == ModBlocks.GRASS_SLAB &&
                    world.getBlockState(origin).getValue(BlockSlab.HALF) == half,
                    "Turf did not preserve dirt-slab orientation " + half);
            checks++;
        }

        BlockPos support = origin.add(0, 0, 2);
        world.setBlockState(support, Blocks.DIRT.getDefaultState(), 3);
        require(canPlace(ModBlocks.TURF, world, support.up()),
                "Turf rejected a full support block");
        world.setBlockState(support, defaultState(ModBlocks.DIRT_SLAB), 3);
        require(!canPlace(ModBlocks.TURF, world, support.up()),
                "Turf accepted a partial support block");
        checks += 2;

        BlockPos turfPos = support.up();
        world.setBlockState(support, Blocks.STONE.getDefaultState(), 3);
        world.setBlockState(turfPos, defaultState(ModBlocks.TURF), 3);
        tick(ModBlocks.TURF, world, turfPos, defaultState(ModBlocks.TURF), new Random(1));
        require(world.isAirBlock(turfPos), "Invalid turf support was not removed on its tick");
        checks++;

        world.setBlockState(support, Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(turfPos, defaultState(ModBlocks.TURF), 3);
        world.setBlockState(turfPos.up(), Blocks.STONE.getDefaultState(), 3);
        tick(ModBlocks.TURF, world, turfPos, defaultState(ModBlocks.TURF), new Random(2));
        require(world.getBlockState(turfPos).getBlock() == ModBlocks.TURF,
                "Covered turf incorrectly gained a dirt stage");
        world.setBlockToAir(turfPos.up());
        checks++;

        world.setBlockState(support, Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(turfPos, defaultState(ModBlocks.TURF), 3);
        world.setBlockState(support, Blocks.GRASS.getDefaultState(), 2);
        tick(ModBlocks.TURF, world, turfPos, world.getBlockState(turfPos), new Random(3));
        require(world.isAirBlock(turfPos),
                "Pre-existing turf above grass did not retain invalid-support removal");
        checks++;

        verifySpreading(world, origin.add(4, 0, 0));
        checks += 13;
        verifyGrassCoveringAndSnow(world, player, origin.add(10, 0, 0));
        checks += 12;
        verifyTurfEating(world, origin.add(10, 0, 6));
        checks += 7;
        verifyFlattening(world, player, origin.add(0, 0, 4));
        checks += 5;
        verifyRecipes(world);
        checks += 8;

        IPlantable sapling = (IPlantable) Blocks.SAPLING;
        IBlockState topGrass = defaultState(ModBlocks.GRASS_SLAB)
                .withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP);
        IBlockState bottomGrass = defaultState(ModBlocks.GRASS_SLAB);
        require(((Block) ModBlocks.GRASS_SLAB).canSustainPlant(topGrass, world, origin,
                EnumFacing.UP, sapling), "Top grass slab rejected a plant");
        require(!((Block) ModBlocks.GRASS_SLAB).canSustainPlant(bottomGrass, world, origin,
                EnumFacing.UP, sapling), "Bottom grass slab accepted a plant");
        require(((IGrowable) ModBlocks.GRASS_SLAB).canGrow(world, origin, topGrass, false) &&
                !((IGrowable) ModBlocks.GRASS_SLAB).canGrow(world, origin, bottomGrass, false),
                "Grass slab bonemeal orientation rule changed");
        checks += 3;

        require(canSilkHarvest(ModBlocks.GRASS_SLAB, world, origin, topGrass, player),
                "Grass slab cannot be Silk Touched");
        require(!canSilkHarvest(ModBlocks.PATH_SLAB, world, origin,
                defaultState(ModBlocks.PATH_SLAB), player),
                "Path slab unexpectedly Silk Touches itself");
        require(isOnlyDrop(drops(ModBlocks.GRASS_SLAB, world, origin, topGrass), ModBlocks.DIRT_SLAB) &&
                isOnlyDrop(drops(ModBlocks.PATH_SLAB, world, origin,
                        defaultState(ModBlocks.PATH_SLAB)), ModBlocks.DIRT_SLAB),
                "Grass or path slab ordinary drops changed");
        checks += 3;
        return checks;
    }

    private static void verifySpreading(World world, BlockPos origin) {
        clearBox(world, origin.add(-2, -2, -2), origin.add(3, 3, 3));
        world.setBlockState(origin, defaultState(ModBlocks.GRASS_SLAB), 3);
        BlockPos vanillaTarget = origin.east();
        world.setBlockState(vanillaTarget, Blocks.DIRT.getDefaultState(), 3);
        tick(ModBlocks.GRASS_SLAB, world, origin, world.getBlockState(origin),
                new SequenceRandom(2, 3, 1));
        require(world.getBlockState(vanillaTarget).getBlock() == Blocks.GRASS,
                "Grass slab did not spread to vanilla dirt");

        for (BlockSlab.EnumBlockHalf half : BlockSlab.EnumBlockHalf.values()) {
            world.setBlockState(vanillaTarget, defaultState(ModBlocks.DIRT_SLAB)
                    .withProperty(BlockSlab.HALF, half), 3);
            tick(ModBlocks.GRASS_SLAB, world, origin, world.getBlockState(origin),
                    new SequenceRandom(2, 3, 1));
            require(world.getBlockState(vanillaTarget).getBlock() == ModBlocks.GRASS_SLAB &&
                    world.getBlockState(vanillaTarget).getValue(BlockSlab.HALF) == half,
                    "Grass slab spread changed orientation " + half);
        }

        BlockPos slabTarget = origin.south();
        world.setBlockState(slabTarget, defaultState(ModBlocks.DIRT_SLAB), 3);
        world.setBlockState(slabTarget.west(), Blocks.GRASS.getDefaultState(), 3);
        tick(ModBlocks.DIRT_SLAB, world, slabTarget, world.getBlockState(slabTarget),
                new SequenceRandom(0, 1, 1));
        require(world.getBlockState(slabTarget).getBlock() == ModBlocks.GRASS_SLAB,
                "Vanilla grass did not spread to a dirt slab");

        BlockPos turfSource = origin.north();
        world.setBlockState(turfSource.down(), Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(turfSource, defaultState(ModBlocks.TURF), 3);
        BlockPos turfTarget = turfSource.east();
        world.setBlockState(turfTarget, Blocks.DIRT.getDefaultState(), 3);
        tick(ModBlocks.TURF, world, turfSource, world.getBlockState(turfSource),
                new SequenceRandom(2, 3, 1));
        require(world.getBlockState(turfTarget).getBlock() == Blocks.GRASS,
                "Turf did not spread to vanilla dirt");

        world.setBlockState(turfSource.down(), Blocks.DIRT.getDefaultState(), 3);
        tick(ModBlocks.TURF, world, turfSource, world.getBlockState(turfSource),
                new SequenceRandom(1, 2, 1));
        require(world.getBlockState(turfSource.down()).getBlock() == Blocks.DIRT,
                "Turf converted its own support");

        BlockPos grassSupport = origin.down();
        world.setBlockState(grassSupport, Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(origin, defaultState(ModBlocks.GRASS_SLAB), 3);
        tick(ModBlocks.GRASS_SLAB, world, origin, world.getBlockState(origin),
                new SequenceRandom(1, 2, 1));
        require(world.getBlockState(grassSupport).getBlock() == Blocks.DIRT,
                "Grass slab converted its own support");

        BlockPos coveredTarget = origin.add(2, 0, 0);
        world.setBlockState(coveredTarget, Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(coveredTarget.up(), defaultState(ModBlocks.TURF), 3);
        tick(ModBlocks.GRASS_SLAB, world, origin, world.getBlockState(origin),
                new SequenceRandom(0, 3, 1));
        require(world.getBlockState(coveredTarget).getBlock() == Blocks.DIRT,
                "Mod grass spread beneath turf");

        world.setBlockState(coveredTarget, Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(coveredTarget.up(), defaultState(ModBlocks.GRASS_SLAB), 3);
        tick(ModBlocks.GRASS_SLAB, world, origin, world.getBlockState(origin),
                new SequenceRandom(0, 3, 1));
        require(world.getBlockState(coveredTarget).getBlock() == Blocks.DIRT,
                "Mod grass spread beneath a grass slab");

        BlockPos vanillaSource = coveredTarget.west();
        world.setBlockState(vanillaSource, Blocks.GRASS.getDefaultState(), 3);
        world.setBlockState(coveredTarget, Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(coveredTarget.up(), defaultState(ModBlocks.TURF), 3);
        tick(Blocks.GRASS, world, vanillaSource, world.getBlockState(vanillaSource),
                new SequenceRandom(2, 3, 1));
        require(world.getBlockState(coveredTarget).getBlock() == Blocks.DIRT &&
                world.getBlockState(coveredTarget.up()).getBlock() == ModBlocks.TURF,
                "Vanilla grass persisted beneath turf after its neighbour update");

        world.setBlockState(coveredTarget, Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(coveredTarget.up(), defaultState(ModBlocks.GRASS_SLAB), 3);
        tick(Blocks.GRASS, world, vanillaSource, world.getBlockState(vanillaSource),
                new SequenceRandom(2, 3, 1));
        require(world.getBlockState(coveredTarget).getBlock() == Blocks.DIRT &&
                world.getBlockState(coveredTarget.up()).getBlock() == ModBlocks.GRASS_SLAB,
                "Vanilla grass persisted beneath a grass slab after its neighbour update");

        world.setBlockState(origin, defaultState(ModBlocks.GRASS_SLAB)
                .withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP), 3);
        world.setBlockState(origin.up(), Blocks.STONE.getDefaultState(), 3);
        tick(ModBlocks.GRASS_SLAB, world, origin, world.getBlockState(origin), new Random(4));
        require(world.getBlockState(origin).getBlock() == ModBlocks.DIRT_SLAB &&
                world.getBlockState(origin).getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP,
                "Covered grass slab did not decay while preserving orientation");

        world.setBlockState(origin, defaultState(ModBlocks.PATH_SLAB)
                .withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP), 3);
        neighborChanged(ModBlocks.PATH_SLAB, world.getBlockState(origin), world, origin, Blocks.STONE);
        require(world.getBlockState(origin).getBlock() == ModBlocks.DIRT_SLAB &&
                world.getBlockState(origin).getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP,
                "Covered path slab did not decay while preserving orientation");
    }

    private static void verifyGrassCoveringAndSnow(World world, EntityPlayerMP player,
            BlockPos origin) {
        clearBox(world, origin.add(-2, -2, -2), origin.add(10, 4, 4));
        Item grassItem = Item.getItemFromBlock(ModBlocks.GRASS_SLAB);

        BlockPos bottomSupport = origin;
        world.setBlockState(bottomSupport, Blocks.GRASS.getDefaultState(), 3);
        ItemStack bottomStack = new ItemStack(ModBlocks.GRASS_SLAB);
        EnumActionResult placed = useItem(grassItem, bottomStack, player, world, bottomSupport,
                EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        require(placed == EnumActionResult.SUCCESS &&
                world.getBlockState(bottomSupport.up()).getBlock() == ModBlocks.GRASS_SLAB &&
                world.getBlockState(bottomSupport.up()).getValue(BlockSlab.HALF) ==
                        BlockSlab.EnumBlockHalf.BOTTOM &&
                world.getBlockState(bottomSupport).getBlock() == Blocks.DIRT,
                "Bottom grass slab placement did not dirtify vanilla grass support");

        BlockPos topSupport = origin.east(3);
        BlockPos topTarget = topSupport.up();
        world.setBlockState(topSupport, Blocks.GRASS.getDefaultState(), 3);
        world.setBlockState(topTarget.west(), Blocks.STONE.getDefaultState(), 3);
        ItemStack topStack = new ItemStack(ModBlocks.GRASS_SLAB);
        placed = useItem(grassItem, topStack, player, world, topTarget.west(),
                EnumHand.MAIN_HAND, EnumFacing.EAST, 1.0F, 0.75F, 0.5F);
        require(placed == EnumActionResult.SUCCESS &&
                world.getBlockState(topTarget).getBlock() == ModBlocks.GRASS_SLAB &&
                world.getBlockState(topTarget).getValue(BlockSlab.HALF) ==
                        BlockSlab.EnumBlockHalf.TOP &&
                world.getBlockState(topSupport).getBlock() == Blocks.DIRT,
                "Top grass slab placement did not dirtify vanilla grass support");

        BlockPos repair = origin.east(6);
        world.setBlockState(repair, Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(repair.up(), defaultState(ModBlocks.GRASS_SLAB), 3);
        world.setBlockState(repair, Blocks.GRASS.getDefaultState(), 2);
        tick(ModBlocks.GRASS_SLAB, world, repair.up(), world.getBlockState(repair.up()),
                new Random(9));
        require(world.getBlockState(repair).getBlock() == Blocks.DIRT,
                "Grass slab random tick did not repair a grass support");
        world.setBlockState(repair, Blocks.GRASS.getDefaultState(), 2);
        neighborChanged(ModBlocks.GRASS_SLAB, world.getBlockState(repair.up()), world,
                repair.up(), Blocks.GRASS);
        require(world.getBlockState(repair).getBlock() == Blocks.DIRT,
                "Grass slab neighbour update did not repair a grass support");

        Block[] snowyBlocks = {ModBlocks.GRASS_SLAB, ModBlocks.DIRT_SLAB};
        for (int index = 0; index < snowyBlocks.length; ++index) {
            Block block = snowyBlocks[index];
            for (BlockSlab.EnumBlockHalf half : BlockSlab.EnumBlockHalf.values()) {
                BlockPos pos = origin.add(index * 3, 0, 3 + half.ordinal());
                IBlockState state = defaultState(block).withProperty(BlockSlab.HALF, half);
                world.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), 3);
                world.setBlockState(pos, state, 3);
                BlockPos snow = half == BlockSlab.EnumBlockHalf.TOP ? pos.east() : pos.up();
                world.setBlockState(snow, Blocks.SNOW.getDefaultState(), 2);
                IBlockState actual = actualState(block, state, world, pos);
                boolean snowy = block == ModBlocks.GRASS_SLAB
                        ? actual.getValue(BlockGrass.SNOWY)
                        : actual.getValue(BlockDirt.SNOWY);
                require(snowy && metadata(block, actual) ==
                        (half == BlockSlab.EnumBlockHalf.TOP ? 0 : 1),
                        "Visual snow state or metadata changed for " + blockId(block) + " " + half);
                world.setBlockToAir(snow);
                actual = actualState(block, state, world, pos);
                snowy = block == ModBlocks.GRASS_SLAB
                        ? actual.getValue(BlockGrass.SNOWY)
                        : actual.getValue(BlockDirt.SNOWY);
                require(!snowy, "Visual snow state persisted after snow removal for " +
                        blockId(block) + " " + half);
            }
        }
    }

    private static void verifyTurfEating(WorldServer world, BlockPos turfPos) {
        clearBox(world, turfPos.add(-2, -2, -2), turfPos.add(2, 3, 2));
        world.setBlockState(turfPos.down(), Blocks.DIRT.getDefaultState(), 3);
        world.setBlockState(turfPos, defaultState(ModBlocks.TURF), 3);
        EntitySheep sheep = new EntitySheep(world);
        sheep.setPosition(turfPos.getX() + 0.5D, turfPos.getY(), turfPos.getZ() + 0.5D);
        CommonEvents events = new CommonEvents();
        events.addTurfEatingTask(new EntityJoinWorldEvent(sheep, world));
        events.addTurfEatingTask(new EntityJoinWorldEvent(sheep, world));
        require(countTurfEatingTasks(sheep) == 1,
                "Sheep received a duplicate turf-eating task");

        TurfEatingAI task = turfEatingTask(sheep);
        sheep.setGrowingAge(0);
        sheep.setSheared(true);
        int drops = entityItems(world, turfPos);
        startAiTask(task);
        require(task.getEatingTimer() == 40,
                "Turf eating did not begin with the vanilla animation timer");
        runEatingTicks(task);
        require(world.isAirBlock(turfPos), "Sheep did not destroy turf");
        require(!sheep.getSheared(), "Adult sheep did not regrow wool after eating turf");
        require(entityItems(world, turfPos) == drops,
                "Sheep eating turf created an item drop");

        boolean mobGriefing = world.getGameRules().getBoolean("mobGriefing");
        try {
            world.getGameRules().setOrCreateGameRule("mobGriefing", "false");
            world.setBlockState(turfPos.down(), Blocks.DIRT.getDefaultState(), 3);
            world.setBlockState(turfPos, defaultState(ModBlocks.TURF), 3);
            sheep.setGrowingAge(-1000);
            int age = sheep.getGrowingAge();
            startAiTask(task);
            runEatingTicks(task);
            require(world.getBlockState(turfPos).getBlock() == ModBlocks.TURF,
                    "mobGriefing=false did not retain turf");
            require(sheep.getGrowingAge() > age,
                    "mobGriefing=false did not retain the vanilla child-growth bonus");
        } finally {
            world.getGameRules().setOrCreateGameRule("mobGriefing",
                    Boolean.toString(mobGriefing));
            sheep.setGrowingAge(0);
        }
    }

    private static int countTurfEatingTasks(EntitySheep sheep) {
        int count = 0;
        for (EntityAITasks.EntityAITaskEntry entry : sheep.tasks.taskEntries) {
            if (entry.action instanceof TurfEatingAI) ++count;
        }
        return count;
    }

    private static TurfEatingAI turfEatingTask(EntitySheep sheep) {
        for (EntityAITasks.EntityAITaskEntry entry : sheep.tasks.taskEntries) {
            if (entry.action instanceof TurfEatingAI) return (TurfEatingAI) entry.action;
        }
        throw new IllegalStateException("Sheep has no turf-eating task");
    }

    private static void runEatingTicks(TurfEatingAI task) {
        for (int tick = 0; tick < 36; ++tick) updateAiTask(task);
        require(task.getEatingTimer() == 4, "Turf eating timer did not reach action tick");
    }

    private static void startAiTask(EntityAIBase task) {
        task.startExecuting();
    }

    private static void updateAiTask(EntityAIBase task) {
        task.updateTask();
    }

    private static int entityItems(World world, BlockPos pos) {
        AxisAlignedBB area = new AxisAlignedBB(pos.add(-1, -1, -1), pos.add(2, 2, 2));
        return world.getEntitiesWithinAABB(EntityItem.class, area).size();
    }

    private static void verifyFlattening(World world, EntityPlayerMP player, BlockPos pos) {
        CommonEvents events = new CommonEvents();
        world.setBlockToAir(pos.up());
        IBlockState grassTop = defaultState(ModBlocks.GRASS_SLAB)
                .withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP);
        world.setBlockState(pos, grassTop, 3);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        shovel.setItemDamage(3);
        PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player,
                EnumHand.MAIN_HAND, shovel, pos, EnumFacing.UP, new Vec3d(0.5D, 1.0D, 0.5D));
        events.flattenSlab(event);
        require(event.isCanceled() && world.getBlockState(pos).getBlock() == ModBlocks.PATH_SLAB &&
                world.getBlockState(pos).getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP &&
                shovel.getItemDamage() == 4, "Vanilla shovel flattening contract changed");

        Item compatibleShovel = new Item() {
            @Override
            public Set<String> getToolClasses(ItemStack stack) {
                return Collections.singleton("shovel");
            }
        }.setMaxDamage(64);
        ItemStack compatible = new ItemStack(compatibleShovel);
        world.setBlockState(pos, defaultState(ModBlocks.DIRT_SLAB), 3);
        event = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, compatible,
                pos, EnumFacing.UP, new Vec3d(0.5D, 1.0D, 0.5D));
        events.flattenSlab(event);
        require(world.getBlockState(pos).getBlock() == ModBlocks.PATH_SLAB &&
                compatible.getItemDamage() == 1, "Forge-compatible shovel was not accepted");

        world.setBlockState(pos, defaultState(ModBlocks.GRASS_SLAB), 3);
        ItemStack downward = new ItemStack(Items.IRON_SHOVEL);
        event = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, downward,
                pos, EnumFacing.DOWN, new Vec3d(0.5D, 0.0D, 0.5D));
        events.flattenSlab(event);
        require(!event.isCanceled() && world.getBlockState(pos).getBlock() == ModBlocks.GRASS_SLAB &&
                downward.getItemDamage() == 0, "Downward shovel face was incorrectly flattened");

        world.setBlockState(pos.up(), Blocks.STONE.getDefaultState(), 3);
        event = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, downward,
                pos, EnumFacing.UP, new Vec3d(0.5D, 1.0D, 0.5D));
        events.flattenSlab(event);
        require(!event.isCanceled() && world.getBlockState(pos).getBlock() == ModBlocks.GRASS_SLAB,
                "Blocked shovel interaction was not refused");
        world.setBlockToAir(pos.up());

        require(isOnlyDrop(drops(ModBlocks.PATH_SLAB, world, pos,
                defaultState(ModBlocks.PATH_SLAB)), ModBlocks.DIRT_SLAB),
                "Path slab did not drop a dirt slab");
    }

    private static void verifyRecipes(World world) {
        IRecipe recipe = new TurfCuttingRecipe();
        InventoryCrafting grid = craftingGrid(2, 2);
        ItemStack shovel = new ItemStack(Items.DIAMOND_SHOVEL);
        shovel.setItemDamage(17);
        shovel.addEnchantment(Enchantments.UNBREAKING, 2);
        NBTTagCompound toolData = new NBTTagCompound();
        toolData.setString("probe", "retained");
        shovel.setTagCompound(toolData);
        grid.setInventorySlotContents(0, new ItemStack(ModBlocks.GRASS_SLAB));
        grid.setInventorySlotContents(1, shovel);
        require(recipe.matches(grid, world), "Grass slab turf recipe did not match in 2x2 crafting");
        require(recipe.getCraftingResult(grid).getItem() == Item.getItemFromBlock(ModBlocks.TURF),
                "Turf recipe output changed");
        ItemStack[] remaining = recipe.getRemainingItems(grid);
        require(remaining[0] != null && remaining[0].getItem() == Item.getItemFromBlock(ModBlocks.DIRT_SLAB),
                "Grass slab did not return a dirt slab");
        require(remaining[1] != null && remaining[1].getItem() == shovel.getItem() &&
                remaining[1].getItemDamage() == 17 && remaining[1].stackSize == 1 &&
                remaining[1].hasTagCompound() && "retained".equals(
                        remaining[1].getTagCompound().getString("probe")),
                "Turf recipe changed the shovel remainder");

        grid.clear();
        grid.setInventorySlotContents(0, new ItemStack(Blocks.GRASS));
        grid.setInventorySlotContents(3, new ItemStack(Items.WOODEN_SHOVEL));
        require(recipe.matches(grid, world), "Grass block turf recipe did not match");
        remaining = recipe.getRemainingItems(grid);
        require(remaining[0] != null && remaining[0].getItem() == Item.getItemFromBlock(Blocks.DIRT),
                "Grass block did not return vanilla dirt");

        grid.setInventorySlotContents(2, new ItemStack(Items.STICK));
        require(!recipe.matches(grid, world), "Turf recipe accepted an additional ingredient");
        grid.clear();
        grid.setInventorySlotContents(0, new ItemStack(Blocks.GRASS));
        grid.setInventorySlotContents(1, new ItemStack(Items.IRON_PICKAXE));
        require(!recipe.matches(grid, world), "Turf recipe accepted a non-shovel tool");
    }

    private static int verifyWorldgen(WorldServer world) {
        int checks = 0;
        int chunkX = (world.getSpawnPoint().getX() >> 4) + 4;
        int chunkZ = (world.getSpawnPoint().getZ() >> 4) + 4;
        world.getChunkFromChunkCoords(chunkX, chunkZ);
        GrassSlabSmoothingHandler handler = new GrassSlabSmoothingHandler();
        BlockPos chunkStart = new BlockPos(chunkX << 4, 0, chunkZ << 4);

        BlockPos lower = new BlockPos((chunkX << 4) + 5, 180, (chunkZ << 4) + 5);
        prepareGrassSurface(world, lower);
        prepareGrassSurface(world, lower.east().up());
        handler.beforeDecoration(new DecorateBiomeEvent.Pre(world, new Random(1), chunkStart));
        require(world.getBlockState(lower.up()).getBlock() == ModBlocks.GRASS_SLAB &&
                metadata(ModBlocks.GRASS_SLAB, world.getBlockState(lower.up())) == 1 &&
                world.getBlockState(lower).getBlock() == Blocks.DIRT,
                "Interior slope did not gain a bottom grass slab over dirt");
        checks++;
        handler.beforeDecoration(new DecorateBiomeEvent.Pre(world, new Random(2), chunkStart));
        require(world.getBlockState(lower.up()).getBlock() == ModBlocks.GRASS_SLAB,
                "Smoothing was not idempotent");
        checks++;

        BlockPos flat = new BlockPos((chunkX << 4) + 8, 184, (chunkZ << 4) + 8);
        prepareGrassSurface(world, flat);
        prepareGrassSurface(world, flat.east());
        handler.beforeDecoration(new DecorateBiomeEvent.Pre(world, new Random(3), chunkStart));
        require(world.isAirBlock(flat.up()), "Flat terrain was modified by smoothing");
        checks++;

        BlockPos occupied = new BlockPos((chunkX << 4) + 10, 188, (chunkZ << 4) + 10);
        prepareGrassSurface(world, occupied);
        prepareGrassSurface(world, occupied.east().up());
        world.setBlockState(occupied.up(), Blocks.CHEST.getDefaultState(), 3);
        handler.beforeDecoration(new DecorateBiomeEvent.Pre(world, new Random(4), chunkStart));
        require(world.getBlockState(occupied.up()).getBlock() == Blocks.CHEST,
                "Smoothing overwrote an occupied block or block entity");
        checks++;

        BlockPos replaceable = new BlockPos((chunkX << 4) + 6, 190, (chunkZ << 4) + 12);
        prepareGrassSurface(world, replaceable);
        prepareGrassSurface(world, replaceable.east().up());
        world.setBlockState(replaceable.up(), Blocks.TALLGRASS.getDefaultState(), 3);
        handler.beforeDecoration(new DecorateBiomeEvent.Pre(world, new Random(5), chunkStart));
        require(world.getBlockState(replaceable.up()).getBlock() == Blocks.TALLGRASS,
                "Smoothing overwrote replaceable provider decoration");
        checks++;

        BlockPos wet = new BlockPos((chunkX << 4) + 12, 192, (chunkZ << 4) + 12);
        prepareGrassSurface(world, wet);
        prepareGrassSurface(world, wet.east().up());
        world.setBlockState(wet.up(), Blocks.WATER.getDefaultState(), 3);
        handler.beforeDecoration(new DecorateBiomeEvent.Pre(world, new Random(6), chunkStart));
        require(world.getBlockState(wet.up()).getBlock() == Blocks.WATER,
                "Smoothing overwrote a fluid");
        checks++;

        int eastChunkX = chunkX + 1;
        world.getChunkFromChunkCoords(eastChunkX, chunkZ);
        BlockPos eastLower = new BlockPos((chunkX << 4) + 15, 196, (chunkZ << 4) + 7);
        prepareGrassSurface(world, eastLower);
        prepareGrassSurface(world, eastLower.east().up());
        handler.beforeDecoration(new DecorateBiomeEvent.Pre(world, new Random(7), chunkStart));
        require(world.getBlockState(eastLower.up()).getBlock() == ModBlocks.GRASS_SLAB,
                "Available east border was not smoothed by its owning chunk");
        checks++;

        world.getChunkFromChunkCoords(chunkX - 1, chunkZ);
        BlockPos westLower = new BlockPos(chunkX << 4, 200, (chunkZ << 4) + 11);
        prepareGrassSurface(world, westLower);
        prepareGrassSurface(world, westLower.west().up());
        handler.beforeDecoration(new DecorateBiomeEvent.Pre(world, new Random(8), chunkStart));
        require(world.isAirBlock(westLower.up()),
                "West border comparison escaped the owning-chunk rule");
        checks++;
        return checks;
    }

    private static void prepareGrassSurface(World world, BlockPos grass) {
        world.setBlockState(grass.down(), Blocks.STONE.getDefaultState(), 3);
        world.setBlockState(grass, Blocks.GRASS.getDefaultState(), 3);
        world.setBlockToAir(grass.up());
        world.setBlockToAir(grass.up(2));
    }

    private static EntityPlayerMP player(MinecraftServer server, WorldServer world) {
        PlayerInteractionManager manager = new PlayerInteractionManager(world);
        EntityPlayerMP player = new EntityPlayerMP(server, world,
                new GameProfile(UUID.fromString("c7c9288e-73d8-4f36-afc4-a377f1cac742"),
                        "GrassSlabProbe"), manager);
        player.capabilities.allowEdit = true;
        return player;
    }

    private static InventoryCrafting craftingGrid(int width, int height) {
        return new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, width, height);
    }

    private static boolean isOnlyDrop(List<ItemStack> drops, net.minecraft.block.Block expected) {
        return drops.size() == 1 && drops.get(0).getItem() == Item.getItemFromBlock(expected) &&
                drops.get(0).stackSize == 1;
    }

    // Keep inherited vanilla calls owned by vanilla types in bytecode so the
    // same probe can be reobfuscated for the packaged-runtime gate.
    private static String blockId(Block block) {
        return block.getRegistryName().toString();
    }

    private static IBlockState defaultState(Block block) {
        return block.getDefaultState();
    }

    private static int metadata(Block block, IBlockState state) {
        return block.getMetaFromState(state);
    }

    private static IBlockState stateFromMetadata(Block block, int metadata) {
        return block.getStateFromMeta(metadata);
    }

    private static IBlockState actualState(Block block, IBlockState state,
            World world, BlockPos pos) {
        return block.getActualState(state, world, pos);
    }

    private static boolean canPlace(Block block, World world, BlockPos pos) {
        return block.canPlaceBlockAt(world, pos);
    }

    private static void tick(Block block, World world, BlockPos pos,
            IBlockState state, Random random) {
        block.updateTick(world, pos, state, random);
    }

    private static void neighborChanged(Block block, IBlockState state, World world,
            BlockPos pos, Block changedBlock) {
        block.neighborChanged(state, world, pos, changedBlock);
    }

    private static boolean canSilkHarvest(Block block, World world, BlockPos pos,
            IBlockState state, EntityPlayer player) {
        return block.canSilkHarvest(world, pos, state, player);
    }

    private static List<ItemStack> drops(Block block, World world, BlockPos pos,
            IBlockState state) {
        return block.getDrops(world, pos, state, 0);
    }

    private static EnumActionResult useItem(Item item, ItemStack stack,
            EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        return item.onItemUse(stack, player, world, pos, hand, facing, hitX, hitY, hitZ);
    }

    private static void clearBox(World world, BlockPos from, BlockPos to) {
        for (int y = from.getY(); y <= to.getY(); ++y) {
            for (int z = from.getZ(); z <= to.getZ(); ++z) {
                for (int x = from.getX(); x <= to.getX(); ++x) {
                    world.setBlockToAir(new BlockPos(x, y, z));
                }
            }
        }
    }

    private static Properties load(File marker) throws Exception {
        Properties values = new Properties();
        if (marker.isFile()) {
            FileInputStream input = new FileInputStream(marker);
            try {
                values.load(input);
            } finally {
                input.close();
            }
        }
        return values;
    }

    private static void store(File marker, Properties values) throws Exception {
        FileOutputStream output = new FileOutputStream(marker);
        try {
            values.store(output, "Sky's Grass Slabs build-only integration evidence");
        } finally {
            output.close();
        }
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 0.0000001D;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class SequenceRandom extends Random {
        private final int[] values;
        private int index;

        private SequenceRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            int value = values[index++ % values.length];
            return Math.floorMod(value, bound);
        }
    }

    private static final class LegacyContentCounts {
        private int chunks;
        private long grassTop;
        private long grassBottom;
        private long dirtTop;
        private long dirtBottom;
        private long grassItems;
        private long dirtItems;
        private LegacyBlockSample grassSample;
        private LegacyBlockSample dirtSample;
        private LegacyChunkSample itemSample;
    }

    private static final class LegacyBlockSample {
        private final int dimension;
        private final BlockPos position;
        private final int metadata;

        private LegacyBlockSample(int dimension, BlockPos position, int metadata) {
            this.dimension = dimension;
            this.position = position;
            this.metadata = metadata;
        }
    }

    private static final class LegacyChunkSample {
        private final int dimension;
        private final int chunkX;
        private final int chunkZ;

        private LegacyChunkSample(int dimension, int chunkX, int chunkZ) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }
}
