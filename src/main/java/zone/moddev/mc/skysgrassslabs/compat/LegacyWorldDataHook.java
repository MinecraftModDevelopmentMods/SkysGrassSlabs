package zone.moddev.mc.skysgrassslabs.compat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Bridges supported pre-flattening slab states before vanilla chunk data fixing. */
public final class LegacyWorldDataHook {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BitSet SUPPORTED_BLOCK_IDS = new BitSet();
    private static final Set<Long> LEGACY_CHUNKS = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> SKY_IDS = new LinkedHashSet<>();
    private static final Set<ResourceLocation> HISTORICAL_IDS = new LinkedHashSet<>();
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
            MinecraftForge.EVENT_BUS.addListener(LegacyWorldDataHook::onServerAboutToStart);
            registered = true;
        }
    }

    /** Called from Forge's raw additional-level-data reader before legacy FML data is discarded. */
    public static void captureLegacyLevelData(LevelStorageSource.LevelStorageAccess access,
            LevelStorageSource.LevelDirectory levelDirectory) {
        if (access == null || levelDirectory == null) {
            return;
        }
        CompoundTag root;
        try {
            root = access.getDataTagRaw(false);
        } catch (IOException primaryFailure) {
            try {
                root = access.getDataTagRaw(true);
            } catch (IOException fallbackFailure) {
                LOGGER.warn("Could not inspect primary or fallback level data in '{}' for "
                        + "legacy Sky's Grass Slabs mappings", levelDirectory.path(),
                        fallbackFailure);
                return;
            }
        }
        Path levelPath = levelDirectory.path();
        if (root.contains("FML", Tag.TAG_COMPOUND)) {
            prepareLegacyWorld(levelPath.toFile(), root.getCompound("FML"));
        } else if (root.contains("fml", Tag.TAG_COMPOUND)) {
            prepareLegacyWorld(levelPath.toFile(), root.getCompound("fml"));
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
            if (root.contains("FML", Tag.TAG_COMPOUND)) {
                CompoundTag registries = root.getCompound("FML").getCompound("Registries");
                if (registries.contains("minecraft:blocks", Tag.TAG_COMPOUND)) {
                    CompoundTag blocks = registries.getCompound("minecraft:blocks");
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
        if (fmlData.contains("Registries", Tag.TAG_COMPOUND)) {
            CompoundTag registries = fmlData.getCompound("Registries");
            if (registries.contains("minecraft:blocks", Tag.TAG_COMPOUND)) {
                CompoundTag blocks = registries.getCompound("minecraft:blocks");
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
                    NbtAccounter.unlimitedHeap()).getCompound("Blocks"));
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
        if (!legacyWorldActive || root == null || !root.contains("Level", Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag level = root.getCompound("Level");
        if (!containsSupportedBlock(level)) {
            return;
        }
        LEGACY_CHUNKS.add(chunkKey(level.getInt("xPos"), level.getInt("zPos")));
        level.putBoolean("TerrainPopulated", true);
        level.putBoolean("LightPopulated", true);
        level.putBoolean(PRESERVE_CHUNK_MARKER, true);
    }

    /** Called by the chunk-loader coremod after vanilla data fixing. */
    public static CompoundTag finalizeLegacyChunk(CompoundTag root) {
        if (root == null) {
            return null;
        }
        CompoundTag level = root.contains("Level", Tag.TAG_COMPOUND)
                ? root.getCompound("Level") : root;
        if (level.getBoolean(PRESERVE_CHUNK_MARKER)
                || LEGACY_CHUNKS.contains(chunkKey(level.getInt("xPos"), level.getInt("zPos")))) {
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
        Map<ResourceLocation, Integer> supported = new LinkedHashMap<>();
        Set<ResourceLocation> unsupported = new LinkedHashSet<>();
        ListTag savedIds = blockSnapshot.getList("ids", Tag.TAG_COMPOUND);
        for (int index = 0; index < savedIds.size(); ++index) {
            CompoundTag savedId = savedIds.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(savedId.getString("K"));
            if (id == null) {
                continue;
            }
            int numericId = savedId.getInt("V");
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
        Method registerState = findLegacyStateRegistrationMethod();
        int mapped = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : supported.entrySet()) {
            SUPPORTED_BLOCK_IDS.set(entry.getValue());
            for (int metadata = 0; metadata < 16; ++metadata) {
                int stateId = entry.getValue() << 4 | metadata;
                try {
                    registerState.invoke(null, stateId,
                            NbtUtils.writeBlockState(legacyState(entry.getKey(), metadata)).toString(),
                            new String[0]);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Could not register legacy block state "
                            + entry.getKey() + ":" + metadata, exception);
                }
                ++mapped;
            }
        }
        return mapped;
    }

    static BlockState legacyState(ResourceLocation id, int metadata) {
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
        return state.hasProperty(SnowyDirtBlock.SNOWY)
                ? state.setValue(SnowyDirtBlock.SNOWY, false) : state;
    }

    private static boolean containsSupportedBlock(CompoundTag level) {
        ListTag sections = level.getList("Sections", Tag.TAG_COMPOUND);
        for (int sectionIndex = 0; sectionIndex < sections.size(); ++sectionIndex) {
            CompoundTag section = sections.getCompound(sectionIndex);
            byte[] blocks = section.getByteArray("Blocks");
            if (blocks.length != 4096) {
                continue;
            }
            byte[] add = section.getByteArray("Add");
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
        File regionDirectory = new File(worldDirectory, "region");
        File[] regionFiles = regionDirectory.listFiles((directory, name) ->
                name.startsWith("r.") && (name.endsWith(".mca") || name.endsWith(".mcr")));
        if (regionFiles == null) {
            return 0;
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
        return LEGACY_CHUNKS.size();
    }

    private static Method findLegacyStateRegistrationMethod() {
        for (Method method : BlockStateData.class.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                    && method.getReturnType() == void.class && parameters.length == 3
                    && parameters[0] == int.class && parameters[1] == String.class
                    && parameters[2] == String[].class) {
                return method;
            }
        }
        throw new IllegalStateException("Could not find the public Forge 52 legacy block-state "
                + "registration method; the coremod was not applied");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SkysGrassSlabs.MOD_ID, path);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (long) chunkX << 32 | chunkZ & 0xFFFFFFFFL;
    }

    private LegacyWorldDataHook() {
    }
}
