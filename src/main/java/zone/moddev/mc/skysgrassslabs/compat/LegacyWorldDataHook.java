package zone.moddev.mc.skysgrassslabs.compat;

import com.mojang.datafixers.Dynamic;
import cpw.mods.modlauncher.api.INameMappingService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirtSnowy;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.fixes.BlockStateFlatteningMap;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.WorldPersistenceHooks;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import sun.misc.Unsafe;

/** Bridges Forge 1.10-1.12 numeric registry snapshots into 1.13 block states. */
public final class LegacyWorldDataHook implements WorldPersistenceHooks.WorldPersistenceHook {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final LegacyWorldDataHook INSTANCE = new LegacyWorldDataHook();
    private static final ResourceLocation BLOCK_REGISTRY =
            new ResourceLocation("minecraft", "blocks");
    private static final Map<String, NBTTagCompound> LEGACY_WORLD_DATA =
            new ConcurrentHashMap<String, NBTTagCompound>();
    private static final BitSet SUPPORTED_BLOCK_IDS = new BitSet();
    private static final Set<Long> LEGACY_CHUNKS = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> SKY_IDS = new LinkedHashSet<ResourceLocation>();
    private static final Set<ResourceLocation> SUPPORTED_BUILDINGBRICKS_IDS =
            new LinkedHashSet<ResourceLocation>();
    private static final String PRESERVE_CHUNK_MARKER = "SkysGrassSlabsLegacyPreserveChunk";
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
            WorldPersistenceHooks.addHook(INSTANCE);
            registered = true;
        }
    }

    public static synchronized void prepareLegacyWorld(File levelDat) {
        legacyWorldActive = false;
        LEGACY_CHUNKS.clear();
        if (!levelDat.isFile()) return;
        try (FileInputStream input = new FileInputStream(levelDat)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(input);
            if (root.contains("FML", 10)) {
                prepareLegacyData(levelDat.getParentFile(), root.getCompound("FML"));
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not inspect '{}' for legacy Sky's Grass Slabs registry data",
                    levelDat, exception);
        }
    }

    /** Called before Minecraft applies its vanilla block-state flattening. */
    public static synchronized void prepareLegacyChunk(NBTTagCompound root) {
        if (!legacyWorldActive || root == null || !root.contains("Level", 10)) return;
        NBTTagCompound level = root.getCompound("Level");
        if (!containsSupportedBlock(level)) return;
        level.setBoolean("TerrainPopulated", true);
        level.setBoolean("LightPopulated", true);
        level.setBoolean(PRESERVE_CHUNK_MARKER, true);
    }

    /** Called after data fixing but before the chunk is deserialized. */
    public static NBTTagCompound finalizeLegacyChunk(NBTTagCompound root) {
        if (root == null || !root.contains("Level", 10)) return root;
        NBTTagCompound level = root.getCompound("Level");
        if (level.getBoolean(PRESERVE_CHUNK_MARKER)) {
            level.setString("Status", "postprocessed");
            level.removeTag(PRESERVE_CHUNK_MARKER);
        }
        LegacyMigrationHandler.migrateStacksInNbt(root, null);
        return root;
    }

    public static boolean isLegacyChunk(int chunkX, int chunkZ) {
        return legacyWorldActive && LEGACY_CHUNKS.contains(chunkKey(chunkX, chunkZ));
    }

    @Override
    public String getModId() {
        return "FML";
    }

    @Override
    public NBTTagCompound getDataForWriting(SaveHandler handler, WorldInfo info) {
        NBTTagCompound legacy = LEGACY_WORLD_DATA.get(worldKey(handler));
        return legacy == null ? new NBTTagCompound() : legacy.copy();
    }

    @Override
    public void readData(SaveHandler handler, WorldInfo info, NBTTagCompound tag) {
        prepareLegacyData(handler.getWorldDirectory(), tag);
    }

    private static synchronized void prepareLegacyData(File worldDirectory,
            NBTTagCompound tag) {
        if (!tag.contains("Registries", 10)) return;
        NBTTagCompound registries = tag.getCompound("Registries");
        if (!registries.contains(BLOCK_REGISTRY.toString(), 10)) {
            throw new IllegalStateException("Legacy world has no saved block registry snapshot");
        }
        String key = worldKey(worldDirectory);
        boolean first = !LEGACY_WORLD_DATA.containsKey(key);
        int mapped = installLegacyBlockStates(registries.getCompound(BLOCK_REGISTRY.toString()));
        if (mapped == 0) return;
        if (first) LEGACY_WORLD_DATA.put(key, tag.copy());
        legacyWorldActive = true;
        int indexed = indexLegacyChunks(worldDirectory);
        if (first) {
            LOGGER.info("Prepared {} legacy slab states and indexed {} existing Overworld chunks " +
                    "from '{}' for safe 1.13 flattening", mapped, indexed, worldDirectory);
        }
    }

    private static int installLegacyBlockStates(NBTTagCompound blockSnapshot) {
        SUPPORTED_BLOCK_IDS.clear();
        Map<ResourceLocation, Integer> supported = new LinkedHashMap<ResourceLocation, Integer>();
        Set<ResourceLocation> unsupported = new LinkedHashSet<ResourceLocation>();
        NBTTagList savedIds = blockSnapshot.getList("ids", 10);
        int highestStateId = 0;
        for (int index = 0; index < savedIds.size(); ++index) {
            NBTTagCompound savedId = savedIds.getCompound(index);
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
            LOGGER.error("Unsupported BuildingBricks content remains outside Sky's Grass Slabs " +
                    "migration scope and may be removed during upgrade: {}", unsupported);
        }
        if (supported.isEmpty()) return 0;
        Dynamic<?>[] table = expandFlatteningTable(highestStateId + 1);
        int mapped = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : supported.entrySet()) {
            SUPPORTED_BLOCK_IDS.set(entry.getValue());
            for (int meta = 0; meta < 16; ++meta) {
                IBlockState state = legacyState(entry.getKey(), meta);
                int stateId = (entry.getValue() << 4) | meta;
                table[stateId] = BlockStateFlatteningMap.makeDynamic(
                        NBTUtil.writeBlockState(state).toString());
                ++mapped;
            }
        }
        return mapped;
    }

    static IBlockState legacyState(ResourceLocation id, int metadata) {
        if (id.equals(new ResourceLocation(SkysGrassSlabs.MOD_ID, "turf"))) {
            return ModBlocks.TURF.getDefaultState();
        }
        boolean grass = id.equals(new ResourceLocation(SkysGrassSlabs.MOD_ID, "grass_slab")) ||
                id.equals(BuildingBricksCompat.GRASS_SLAB_ID) ||
                id.equals(BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID);
        boolean dirt = id.equals(new ResourceLocation(SkysGrassSlabs.MOD_ID, "dirt_slab")) ||
                id.equals(BuildingBricksCompat.DIRT_SLAB_ID);
        IBlockState state = grass ? ModBlocks.GRASS_SLAB.getDefaultState()
                : dirt ? ModBlocks.DIRT_SLAB.getDefaultState()
                : ModBlocks.PATH_SLAB.getDefaultState();
        state = state.with(BlockSlab.TYPE,
                (metadata & 1) == 0 ? SlabType.TOP : SlabType.BOTTOM)
                .with(BlockSlab.WATERLOGGED, Boolean.FALSE);
        if (state.has(BlockDirtSnowy.SNOWY)) {
            state = state.with(BlockDirtSnowy.SNOWY, Boolean.FALSE);
        }
        return state;
    }

    private static boolean containsSupportedBlock(NBTTagCompound level) {
        NBTTagList sections = level.getList("Sections", 10);
        for (int sectionIndex = 0; sectionIndex < sections.size(); ++sectionIndex) {
            NBTTagCompound section = sections.getCompound(sectionIndex);
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
            Field field;
            try {
                field = BlockStateFlatteningMap.class.getDeclaredField("ID_TO_FIXED_NBT");
            } catch (NoSuchFieldException ignored) {
                String fieldName = ObfuscationReflectionHelper.remapName(
                        INameMappingService.Domain.FIELD, "field_199200_b");
                field = BlockStateFlatteningMap.class.getDeclaredField(fieldName);
            }
            field.setAccessible(true);
            Dynamic<?>[] current = (Dynamic<?>[]) field.get(null);
            if (current.length >= requiredLength) return current;
            Dynamic<?>[] expanded = Arrays.copyOf(current, requiredLength);
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            unsafe.putObjectVolatile(unsafe.staticFieldBase(field),
                    unsafe.staticFieldOffset(field), expanded);
            return expanded;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Could not expand Minecraft's legacy block-state flattening table", exception);
        }
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (long) chunkX << 32 | chunkZ & 0xFFFFFFFFL;
    }

    private static String worldKey(SaveHandler handler) {
        return worldKey(handler.getWorldDirectory());
    }

    private static String worldKey(File directory) {
        try {
            return directory.getCanonicalPath();
        } catch (IOException exception) {
            return directory.getAbsolutePath();
        }
    }

    private LegacyWorldDataHook() {
    }
}
