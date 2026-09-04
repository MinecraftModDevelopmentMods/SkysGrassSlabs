package zone.moddev.mc.skysgrassslabs.compat;

import com.mojang.datafixers.Dynamic;
import cpw.mods.modlauncher.api.INameMappingService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.fixes.BlockStateFlatteningMap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Bridges supported pre-flattening slab states before vanilla chunk data fixing. */
public final class LegacyWorldDataHook {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BitSet SUPPORTED_BLOCK_IDS = new BitSet();
    private static final Set<Long> LEGACY_CHUNKS = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> SKY_IDS = new LinkedHashSet<ResourceLocation>();
    private static final Set<ResourceLocation> SUPPORTED_BUILDINGBRICKS_IDS =
            new LinkedHashSet<ResourceLocation>();
    private static final String PRESERVE_CHUNK_MARKER = "SkysGrassSlabsLegacyPreserveChunk";
    private static final String SIDECAR_NAME = "skysgrassslabs_legacy_registry.dat";
    private static volatile boolean legacyWorldActive;
    private static boolean registered;

    static {
        SKY_IDS.add(new ResourceLocation(SkysGrassSlabs.MOD_ID, "dirt_slab"));
        SKY_IDS.add(new ResourceLocation(SkysGrassSlabs.MOD_ID, "grass_slab"));
        SKY_IDS.add(new ResourceLocation(SkysGrassSlabs.MOD_ID, "path_slab"));
        SKY_IDS.add(new ResourceLocation(SkysGrassSlabs.MOD_ID, "turf"));
        SUPPORTED_BUILDINGBRICKS_IDS.add(BuildingBricksCompat.GRASS_SLAB_ID);
        SUPPORTED_BUILDINGBRICKS_IDS.add(BuildingBricksCompat.DIRT_SLAB_ID);
        SUPPORTED_BUILDINGBRICKS_IDS.add(BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID);
    }

    public static synchronized void register() {
        if (!registered) {
            MinecraftForge.EVENT_BUS.addListener(LegacyWorldDataHook::onServerAboutToStart);
            registered = true;
        }
    }

    public static void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        File levelDat = event.getServer().getActiveAnvilConverter()
                .getFile(event.getServer().getFolderName(), "level.dat");
        prepareLegacyWorld(levelDat);
    }

    static synchronized void prepareLegacyWorld(File levelDat) {
        legacyWorldActive = false;
        LEGACY_CHUNKS.clear();
        if (!levelDat.isFile()) return;

        try (FileInputStream input = new FileInputStream(levelDat)) {
            CompoundNBT root = CompressedStreamTools.readCompressed(input);
            if (root.contains("FML", 10)) {
                CompoundNBT registries = root.getCompound("FML").getCompound("Registries");
                if (registries.contains("minecraft:blocks", 10)) {
                    CompoundNBT blocks = registries.getCompound("minecraft:blocks");
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

        File sidecar = sidecar(levelDat.getParentFile());
        if (sidecar.isFile()) {
            try (FileInputStream input = new FileInputStream(sidecar)) {
                install(levelDat.getParentFile(),
                        CompressedStreamTools.readCompressed(input).getCompound("Blocks"));
            } catch (IOException exception) {
                LOGGER.warn("Could not read legacy Sky's Grass Slabs registry sidecar '{}'",
                        sidecar, exception);
            }
        }
    }

    private static void install(File worldDirectory, CompoundNBT blockSnapshot) {
        int mapped = installLegacyBlockStates(blockSnapshot);
        legacyWorldActive = mapped > 0;
        if (legacyWorldActive) {
            int indexed = indexLegacyChunks(worldDirectory);
            LOGGER.info("Prepared {} legacy slab states and indexed {} existing Overworld " +
                    "chunks from '{}'", mapped, indexed, worldDirectory);
        }
    }

    private static void writeSidecar(File worldDirectory, CompoundNBT blockSnapshot) {
        File sidecar = sidecar(worldDirectory);
        if (sidecar.isFile()) return;
        File parent = sidecar.getParentFile();
        File temporary = new File(parent, SIDECAR_NAME + ".tmp");
        try {
            Files.createDirectories(parent.toPath());
            CompoundNBT root = new CompoundNBT();
            root.put("Blocks", blockSnapshot.copy());
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                CompressedStreamTools.writeCompressed(root, output);
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
    public static void prepareLegacyChunk(CompoundNBT root) {
        if (!legacyWorldActive || root == null || !root.contains("Level", 10)) return;
        CompoundNBT level = root.getCompound("Level");
        if (!containsSupportedBlock(level)) return;
        level.putBoolean("TerrainPopulated", true);
        level.putBoolean("LightPopulated", true);
        level.putBoolean(PRESERVE_CHUNK_MARKER, true);
    }

    /** Called by the chunk-loader coremod after vanilla data fixing. */
    public static CompoundNBT finalizeLegacyChunk(CompoundNBT root) {
        if (root == null || !root.contains("Level", 10)) return root;
        CompoundNBT level = root.getCompound("Level");
        if (level.getBoolean(PRESERVE_CHUNK_MARKER)) {
            level.putString("Status", "full");
            level.remove(PRESERVE_CHUNK_MARKER);
        }
        LegacyMigrationHandler.migrateStacksInNbt(root, null);
        return root;
    }

    public static boolean isLegacyChunk(int chunkX, int chunkZ) {
        return legacyWorldActive && LEGACY_CHUNKS.contains(chunkKey(chunkX, chunkZ));
    }

    private static int installLegacyBlockStates(CompoundNBT blockSnapshot) {
        SUPPORTED_BLOCK_IDS.clear();
        Map<ResourceLocation, Integer> supported = new LinkedHashMap<ResourceLocation, Integer>();
        Set<ResourceLocation> unsupported = new LinkedHashSet<ResourceLocation>();
        ListNBT savedIds = blockSnapshot.getList("ids", 10);
        int highestStateId = 0;
        for (int index = 0; index < savedIds.size(); ++index) {
            CompoundNBT savedId = savedIds.getCompound(index);
            ResourceLocation id;
            try {
                id = new ResourceLocation(savedId.getString("K"));
            } catch (RuntimeException exception) {
                continue;
            }
            int numericId = savedId.getInt("V");
            if (SKY_IDS.contains(id) || SUPPORTED_BUILDINGBRICKS_IDS.contains(id)) {
                supported.put(id, numericId);
                highestStateId = Math.max(highestStateId, (numericId << 4) | 15);
            } else if (BuildingBricksCompat.MOD_ID.equals(id.getNamespace()) ||
                    "buildingbrickscompatvanilla".equals(id.getNamespace())) {
                unsupported.add(id);
            }
        }
        if (!unsupported.isEmpty()) {
            LOGGER.warn("Unsupported BuildingBricks content remains outside Sky's Grass Slabs " +
                    "migration scope and may be removed during upgrade: {}", unsupported);
        }
        if (supported.isEmpty()) return 0;

        Dynamic<?>[] table = expandFlatteningTable(highestStateId + 1);
        int mapped = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : supported.entrySet()) {
            SUPPORTED_BLOCK_IDS.set(entry.getValue());
            for (int metadata = 0; metadata < 16; ++metadata) {
                BlockState state = legacyState(entry.getKey(), metadata);
                int stateId = (entry.getValue() << 4) | metadata;
                table[stateId] = BlockStateFlatteningMap.makeDynamic(
                        NBTUtil.writeBlockState(state).toString());
                ++mapped;
            }
        }
        return mapped;
    }

    static BlockState legacyState(ResourceLocation id, int metadata) {
        if (id.equals(new ResourceLocation(SkysGrassSlabs.MOD_ID, "turf"))) {
            return ModBlocks.TURF.getDefaultState();
        }
        boolean grass = id.equals(new ResourceLocation(SkysGrassSlabs.MOD_ID, "grass_slab")) ||
                id.equals(BuildingBricksCompat.GRASS_SLAB_ID) ||
                id.equals(BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID);
        boolean dirt = id.equals(new ResourceLocation(SkysGrassSlabs.MOD_ID, "dirt_slab")) ||
                id.equals(BuildingBricksCompat.DIRT_SLAB_ID);
        BlockState state = grass ? ModBlocks.GRASS_SLAB.getDefaultState()
                : dirt ? ModBlocks.DIRT_SLAB.getDefaultState()
                : ModBlocks.PATH_SLAB.getDefaultState();
        state = state.with(SlabBlock.TYPE,
                (metadata & 1) == 0 ? SlabType.TOP : SlabType.BOTTOM)
                .with(SlabBlock.WATERLOGGED, Boolean.FALSE);
        if (state.has(SnowyDirtBlock.SNOWY)) {
            state = state.with(SnowyDirtBlock.SNOWY, Boolean.FALSE);
        }
        return state;
    }

    private static boolean containsSupportedBlock(CompoundNBT level) {
        ListNBT sections = level.getList("Sections", 10);
        for (int sectionIndex = 0; sectionIndex < sections.size(); ++sectionIndex) {
            CompoundNBT section = sections.getCompound(sectionIndex);
            byte[] blocks = section.getByteArray("Blocks");
            if (blocks.length != 4096) continue;
            byte[] add = section.getByteArray("Add");
            for (int blockIndex = 0; blockIndex < blocks.length; ++blockIndex) {
                int highBits = add.length == 2048
                        ? add[blockIndex >> 1] >> ((blockIndex & 1) * 4) & 15 : 0;
                int blockId = blocks[blockIndex] & 255 | highBits << 8;
                if (SUPPORTED_BLOCK_IDS.get(blockId)) return true;
            }
        }
        return false;
    }

    private static int indexLegacyChunks(File worldDirectory) {
        LEGACY_CHUNKS.clear();
        File regionDirectory = new File(worldDirectory, "region");
        File[] regionFiles = regionDirectory.listFiles((directory, name) ->
                name.startsWith("r.") && (name.endsWith(".mca") || name.endsWith(".mcr")));
        if (regionFiles == null) return 0;
        byte[] locations = new byte[4096];
        for (File regionFile : regionFiles) {
            String[] parts = regionFile.getName().split("\\.");
            if (parts.length != 4) continue;
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
                    if (count < 0) break;
                    read += count;
                }
                for (int index = 0; index < read / 4; ++index) {
                    int offset = index * 4;
                    if ((locations[offset] | locations[offset + 1] |
                            locations[offset + 2] | locations[offset + 3]) != 0) {
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

    @SuppressWarnings("unchecked")
    static Dynamic<?>[] expandFlatteningTable(int requiredLength) {
        try {
            Field valuesField;
            try {
                valuesField = BlockStateFlatteningMap.class.getDeclaredField("ID_TO_FIXED_NBT");
            } catch (NoSuchFieldException ignored) {
                String fieldName = ObfuscationReflectionHelper.remapName(
                        INameMappingService.Domain.FIELD, "field_199200_b");
                valuesField = BlockStateFlatteningMap.class.getDeclaredField(fieldName);
            }
            valuesField.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(valuesField, valuesField.getModifiers() & ~Modifier.FINAL);
            Dynamic<?>[] current = (Dynamic<?>[]) valuesField.get(null);
            if (current.length >= requiredLength) return current;
            Dynamic<?>[] expanded = Arrays.copyOf(current, requiredLength);
            valuesField.set(null, expanded);
            return expanded;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Could not expand Minecraft's legacy block-state flattening table", exception);
        }
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (long) chunkX << 32 | chunkZ & 0xFFFFFFFFL;
    }

    private LegacyWorldDataHook() {
    }
}
