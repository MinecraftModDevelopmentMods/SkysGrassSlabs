package zone.moddev.mc.skysgrassslabs.compat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.mixin.BlockStateDataAccessor;

/** Bridges supported pre-flattening slab states before vanilla chunk data fixing. */
public final class LegacyWorldDataHook {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BitSet SUPPORTED_BLOCK_IDS = new BitSet();
    private static final Set<Long> LEGACY_CHUNKS = ConcurrentHashMap.newKeySet();
    private static final Set<Identifier> SKY_IDS = new LinkedHashSet<>();
    private static final Set<Identifier> HISTORICAL_IDS = new LinkedHashSet<>();
    private static final String PRESERVE_CHUNK_MARKER =
            "SkysGrassSlabsLegacyPreserveChunk";
    private static final String SIDECAR_NAME = "skysgrassslabs_legacy_registry.dat";
    private static volatile boolean legacyWorldActive;
    private static boolean registered;

    static {
        SKY_IDS.add(id("dirt_slab"));
        SKY_IDS.add(id("grass_slab"));
        SKY_IDS.add(id("path_slab"));
        SKY_IDS.add(id("turf"));
        HISTORICAL_IDS.add(BuildingBricksCompat.GRASS_SLAB_ID);
        HISTORICAL_IDS.add(BuildingBricksCompat.DIRT_SLAB_ID);
        HISTORICAL_IDS.add(BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID);
    }

    public static synchronized void register() {
        if (!registered) {
            NeoForge.EVENT_BUS.addListener(LegacyWorldDataHook::onServerAboutToStart);
            registered = true;
        }
    }

    /** Called from NeoForge's raw additional-level-data reader before legacy FML data is discarded. */
    public static void captureLegacyLevelData(CompoundTag root,
            LevelStorageSource.LevelDirectory levelDirectory) {
        if (root == null || levelDirectory == null) {
            return;
        }
        Path levelPath = levelDirectory.path();
        if (root.contains("FML")) {
            prepareLegacyWorld(levelPath.toFile(), root.getCompoundOrEmpty("FML"));
        } else if (root.contains("fml")) {
            prepareLegacyWorld(levelPath.toFile(), root.getCompoundOrEmpty("fml"));
        } else {
            prepareLegacyWorld(levelPath.resolve("level.dat").toFile());
        }
    }

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        File levelDat = event.getServer().getWorldPath(LevelResource.LEVEL_DATA_FILE).toFile();
        prepareLegacyWorld(levelDat);
    }

    static synchronized void prepareLegacyWorld(File levelDat) {
        legacyWorldActive = false;
        LEGACY_CHUNKS.clear();
        if (!levelDat.isFile()) {
            return;
        }
        try (FileInputStream input = new FileInputStream(levelDat)) {
            CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            if (root.contains("FML")) {
                CompoundTag registries = root.getCompoundOrEmpty("FML")
                        .getCompoundOrEmpty("Registries");
                if (registries.contains("minecraft:blocks")) {
                    CompoundTag blocks = registries.getCompoundOrEmpty("minecraft:blocks");
                    install(levelDat.getParentFile(), blocks);
                    writeSidecar(levelDat.getParentFile(), blocks);
                    return;
                }
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not inspect '{}' for legacy Sky's Grass Slabs registry data",
                    levelDat, exception);
            return;
        }
        readSidecar(levelDat.getParentFile());
    }

    private static synchronized void prepareLegacyWorld(File worldDirectory, CompoundTag fmlData) {
        legacyWorldActive = false;
        LEGACY_CHUNKS.clear();
        if (fmlData.contains("Registries")) {
            CompoundTag registries = fmlData.getCompoundOrEmpty("Registries");
            if (registries.contains("minecraft:blocks")) {
                CompoundTag blocks = registries.getCompoundOrEmpty("minecraft:blocks");
                install(worldDirectory, blocks);
                writeSidecar(worldDirectory, blocks);
                return;
            }
        }
        readSidecar(worldDirectory);
    }

    private static void readSidecar(File worldDirectory) {
        File sidecar = sidecar(worldDirectory);
        if (!sidecar.isFile()) {
            return;
        }
        try (FileInputStream input = new FileInputStream(sidecar)) {
            install(worldDirectory, NbtIo.readCompressed(input,
                    NbtAccounter.unlimitedHeap()).getCompoundOrEmpty("Blocks"));
        } catch (IOException exception) {
            LOGGER.warn("Could not read legacy Sky's Grass Slabs registry sidecar '{}'",
                    sidecar, exception);
        }
    }

    private static void install(File worldDirectory, CompoundTag blockSnapshot) {
        int mapped = installLegacyBlockStates(blockSnapshot);
        legacyWorldActive = mapped > 0;
        if (legacyWorldActive) {
            int indexed = indexLegacyChunks(worldDirectory);
            LOGGER.info("Prepared {} legacy slab states and protected {} existing Overworld "
                    + "chunks from '{}'", mapped, indexed, worldDirectory);
        }
    }

    private static void writeSidecar(File worldDirectory, CompoundTag blockSnapshot) {
        File sidecar = sidecar(worldDirectory);
        if (sidecar.isFile()) {
            return;
        }
        File parent = sidecar.getParentFile();
        File temporary = new File(parent, SIDECAR_NAME + ".tmp");
        try {
            Files.createDirectories(parent.toPath());
            CompoundTag root = new CompoundTag();
            root.put("Blocks", blockSnapshot.copy());
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                NbtIo.writeCompressed(root, output);
            }
            try {
                Files.move(temporary.toPath(), sidecar.toPath(),
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary.toPath(), sidecar.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not preserve legacy Sky's Grass Slabs registry data in '{}'",
                    sidecar, exception);
        }
    }

    private static File sidecar(File worldDirectory) {
        return new File(new File(worldDirectory, "data"), SIDECAR_NAME);
    }

    /** Called by the chunk-loader coremod immediately before vanilla data fixing. */
    public static void prepareLegacyChunk(CompoundTag root) {
        if (!legacyWorldActive || root == null || !root.contains("Level")) {
            return;
        }
        CompoundTag level = root.getCompoundOrEmpty("Level");
        if (!containsSupportedBlock(level)) {
            return;
        }
        LEGACY_CHUNKS.add(chunkKey(level.getIntOr("xPos", 0), level.getIntOr("zPos", 0)));
        level.putBoolean("TerrainPopulated", true);
        level.putBoolean("LightPopulated", true);
        level.putBoolean(PRESERVE_CHUNK_MARKER, true);
    }

    /** Called by the chunk-loader coremod after vanilla data fixing. */
    public static CompoundTag finalizeLegacyChunk(CompoundTag root) {
        if (root == null) {
            return null;
        }
        CompoundTag level = root.contains("Level")
                ? root.getCompoundOrEmpty("Level") : root;
        if (level.getBooleanOr(PRESERVE_CHUNK_MARKER, false)
                || LEGACY_CHUNKS.contains(chunkKey(level.getIntOr("xPos", 0),
                        level.getIntOr("zPos", 0)))) {
            level.putString("Status", "full");
            level.remove(PRESERVE_CHUNK_MARKER);
        }
        LegacyMigrationHandler.migrateStacksInNbt(root, null);
        return root;
    }

    public static boolean isLegacyChunk(int chunkX, int chunkZ) {
        return legacyWorldActive && LEGACY_CHUNKS.contains(chunkKey(chunkX, chunkZ));
    }

    private static int installLegacyBlockStates(CompoundTag blockSnapshot) {
        SUPPORTED_BLOCK_IDS.clear();
        Map<Identifier, Integer> supported = new LinkedHashMap<>();
        Set<Identifier> unsupported = new LinkedHashSet<>();
        ListTag savedIds = blockSnapshot.getListOrEmpty("ids");
        for (int index = 0; index < savedIds.size(); ++index) {
            CompoundTag savedId = savedIds.getCompoundOrEmpty(index);
            Identifier id = Identifier.tryParse(savedId.getStringOr("K", ""));
            if (id == null) {
                continue;
            }
            int numericId = savedId.getIntOr("V", -1);
            if (numericId < 0) {
                continue;
            }
            if (SKY_IDS.contains(id) || HISTORICAL_IDS.contains(id)) {
                supported.put(id, numericId);
            } else if (BuildingBricksCompat.MOD_ID.equals(id.getNamespace())
                    || "buildingbrickscompatvanilla".equals(id.getNamespace())) {
                unsupported.add(id);
            }
        }
        if (!unsupported.isEmpty()) {
            LOGGER.warn("Unsupported historical shapes remain outside Sky's Grass Slabs "
                    + "migration scope and may be removed during upgrade: {}", unsupported);
        }
        if (supported.isEmpty()) {
            return 0;
        }
        Dynamic<?>[] legacyStates = BlockStateDataAccessor.skysgrassslabs$getLegacyStateMap();
        int mapped = 0;
        for (Map.Entry<Identifier, Integer> entry : supported.entrySet()) {
            SUPPORTED_BLOCK_IDS.set(entry.getValue());
            for (int metadata = 0; metadata < 16; ++metadata) {
                int stateId = entry.getValue() << 4 | metadata;
                if (stateId >= legacyStates.length) {
                    throw new IllegalStateException("Legacy block-state table was not expanded; "
                            + "cannot register " + entry.getKey() + ":" + metadata);
                }
                legacyStates[stateId] = new Dynamic<>(NbtOps.INSTANCE,
                        NbtUtils.writeBlockState(legacyState(entry.getKey(), metadata)));
                ++mapped;
            }
        }
        return mapped;
    }

    static BlockState legacyState(Identifier id, int metadata) {
        if (id.equals(id("turf"))) {
            return ModBlocks.TURF.get().defaultBlockState();
        }
        boolean grass = id.equals(id("grass_slab"))
                || id.equals(BuildingBricksCompat.GRASS_SLAB_ID)
                || id.equals(BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID);
        boolean dirt = id.equals(id("dirt_slab"))
                || id.equals(BuildingBricksCompat.DIRT_SLAB_ID);
        BlockState state = (grass ? ModBlocks.GRASS_SLAB.get()
                : dirt ? ModBlocks.DIRT_SLAB.get() : ModBlocks.PATH_SLAB.get()).defaultBlockState();
        state = state.setValue(SlabBlock.TYPE,
                (metadata & 1) == 0 ? SlabType.TOP : SlabType.BOTTOM)
                .setValue(SlabBlock.WATERLOGGED, false);
        return state.hasProperty(SnowyBlock.SNOWY)
                ? state.setValue(SnowyBlock.SNOWY, false) : state;
    }

    private static boolean containsSupportedBlock(CompoundTag level) {
        ListTag sections = level.getListOrEmpty("Sections");
        for (int sectionIndex = 0; sectionIndex < sections.size(); ++sectionIndex) {
            CompoundTag section = sections.getCompoundOrEmpty(sectionIndex);
            byte[] blocks = section.getByteArray("Blocks").orElseGet(() -> new byte[0]);
            if (blocks.length != 4096) {
                continue;
            }
            byte[] add = section.getByteArray("Add").orElseGet(() -> new byte[0]);
            for (int blockIndex = 0; blockIndex < blocks.length; ++blockIndex) {
                int highBits = add.length == 2048
                        ? add[blockIndex >> 1] >> ((blockIndex & 1) * 4) & 15 : 0;
                int blockId = blocks[blockIndex] & 255 | highBits << 8;
                if (SUPPORTED_BLOCK_IDS.get(blockId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int indexLegacyChunks(File worldDirectory) {
        LEGACY_CHUNKS.clear();
        indexLegacyRegionDirectory(new File(worldDirectory, "region"));
        indexLegacyRegionDirectory(worldDirectory.toPath().resolve("dimensions")
                .resolve("minecraft").resolve("overworld").resolve("region").toFile());
        return LEGACY_CHUNKS.size();
    }

    private static void indexLegacyRegionDirectory(File regionDirectory) {
        File[] regionFiles = regionDirectory.listFiles((directory, name) ->
                name.startsWith("r.") && (name.endsWith(".mca") || name.endsWith(".mcr")));
        if (regionFiles == null) {
            return;
        }
        byte[] locations = new byte[4096];
        for (File regionFile : regionFiles) {
            String[] parts = regionFile.getName().split("\\.");
            if (parts.length != 4) {
                continue;
            }
            int regionX;
            int regionZ;
            try {
                regionX = Integer.parseInt(parts[1]);
                regionZ = Integer.parseInt(parts[2]);
            } catch (NumberFormatException exception) {
                continue;
            }
            try (InputStream input = Files.newInputStream(regionFile.toPath())) {
                int read = 0;
                while (read < locations.length) {
                    int count = input.read(locations, read, locations.length - read);
                    if (count < 0) {
                        break;
                    }
                    read += count;
                }
                for (int index = 0; index < read / 4; ++index) {
                    int offset = index * 4;
                    if ((locations[offset] | locations[offset + 1]
                            | locations[offset + 2] | locations[offset + 3]) != 0) {
                        LEGACY_CHUNKS.add(chunkKey(regionX * 32 + (index & 31),
                                regionZ * 32 + (index >> 5)));
                    }
                }
            } catch (IOException exception) {
                LOGGER.warn("Could not inspect legacy chunk locations in '{}'", regionFile,
                        exception);
            }
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SkysGrassSlabs.MOD_ID, path);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (long) chunkX << 32 | chunkZ & 0xFFFFFFFFL;
    }

    private LegacyWorldDataHook() {
    }
}
