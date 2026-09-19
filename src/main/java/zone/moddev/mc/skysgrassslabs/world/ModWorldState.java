package zone.moddev.mc.skysgrassslabs.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.util.datafix.DataFixTypes;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;

/** Persistent schema and aggregate historical-slab migration totals. */
public final class ModWorldState extends SavedData {
    public static final String DATA_NAME = "skysgrassslabs_world_state";
    public static final int SCHEMA_VERSION = 1;
    public static final int MIGRATION_VERSION = 1;
    private static final SavedDataType<ModWorldState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SkysGrassSlabs.MOD_ID, DATA_NAME), ModWorldState::new,
            CompoundTag.CODEC.xmap(ModWorldState::new, ModWorldState::saveTag),
            DataFixTypes.SAVED_DATA_MAP_DATA);

    private long migratedChunks;
    private long migratedGrassBlocks;
    private long migratedGrassBlocksTop;
    private long migratedGrassBlocksBottom;
    private long migratedDirtBlocks;
    private long migratedDirtBlocksTop;
    private long migratedDirtBlocksBottom;
    private long migratedGrassItems;
    private long migratedDirtItems;
    private long grassSlabsMigratedChunks;
    private long grassSlabsMigratedGrassSlabBlocks;
    private long grassSlabsMigratedDirtSlabBlocks;
    private long grassSlabsMigratedPathSlabBlocks;
    private long grassSlabsMigratedTurfBlocks;
    private long grassSlabsMigratedGrassSlabItems;
    private long grassSlabsMigratedDirtSlabItems;
    private long grassSlabsMigratedPathSlabItems;
    private long grassSlabsMigratedTurfItems;
    private long grassSlabsRetainedGrassCarpets;
    private final Map<String, Long> unsupported = new TreeMap<>();

    public ModWorldState() {
    }

    private ModWorldState(CompoundTag tag) {
        load(tag);
    }

    public static ModWorldState get(Level level) {
        if (!level.isClientSide() && level.dimension() != Level.OVERWORLD && level.getServer() != null) {
            ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                level = overworld;
            }
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return new ModWorldState();
        }
        relocateLegacyState(serverLevel);
        return serverLevel.getDataStorage().computeIfAbsent(TYPE);
    }

    private static synchronized void relocateLegacyState(ServerLevel level) {
        Path worldRoot = level.getServer().getWorldPath(LevelResource.ROOT);
        relocateLegacyState(worldRoot);
    }

    static synchronized void relocateLegacyState(Path worldRoot) {
        Path oldFile = worldRoot.resolve("data").resolve(DATA_NAME + ".dat");
        Path newFile = worldRoot.resolve("dimensions").resolve("minecraft")
                .resolve("overworld").resolve("data").resolve(SkysGrassSlabs.MOD_ID)
                .resolve(DATA_NAME + ".dat");
        if (Files.exists(newFile) || !Files.isRegularFile(oldFile)) {
            return;
        }

        Path temporary = newFile.resolveSibling(newFile.getFileName() + ".migration.tmp");
        try {
            Files.createDirectories(newFile.getParent());
            Files.copy(oldFile, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, newFile, StandardCopyOption.ATOMIC_MOVE);
            SkysGrassSlabs.LOGGER.info("Preserved Sky's Grass Slabs world state in the "
                    + "namespaced data directory");
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw new IllegalStateException("Could not preserve existing Sky's Grass Slabs "
                    + "world state before loading this Minecraft version", exception);
        }
    }

    public void recordChunk() {
        ++migratedChunks;
        setDirty();
    }

    public void recordGrassBlocks(long count) {
        if (count > 0) {
            migratedGrassBlocks += count;
            setDirty();
        }
    }

    public void recordGrassBlocks(long count, int metadata) {
        if (count <= 0) {
            return;
        }
        recordGrassBlocks(count);
        if ((metadata & 1) == 0) {
            migratedGrassBlocksTop += count;
        } else {
            migratedGrassBlocksBottom += count;
        }
    }

    public void recordDirtBlocks(long count) {
        if (count > 0) {
            migratedDirtBlocks += count;
            setDirty();
        }
    }

    public void recordDirtBlocks(long count, int metadata) {
        if (count <= 0) {
            return;
        }
        recordDirtBlocks(count);
        if ((metadata & 1) == 0) {
            migratedDirtBlocksTop += count;
        } else {
            migratedDirtBlocksBottom += count;
        }
    }

    public void recordGrassItems(long count) {
        if (count > 0) {
            migratedGrassItems += count;
            setDirty();
        }
    }

    public void recordDirtItems(long count) {
        if (count > 0) {
            migratedDirtItems += count;
            setDirty();
        }
    }

    public void recordGrassSlabsChunk() {
        ++grassSlabsMigratedChunks;
        setDirty();
    }

    public void recordGrassSlabsBlock(String kind, long count) {
        if (count <= 0L) {
            return;
        }
        switch (kind) {
            case "grass_slab" -> grassSlabsMigratedGrassSlabBlocks += count;
            case "dirt_slab" -> grassSlabsMigratedDirtSlabBlocks += count;
            case "path_slab" -> grassSlabsMigratedPathSlabBlocks += count;
            case "turf" -> grassSlabsMigratedTurfBlocks += count;
            default -> throw new IllegalArgumentException("Unknown migrated block kind: " + kind);
        }
        setDirty();
    }

    public void recordGrassSlabsItem(String kind, long count) {
        if (count <= 0L) {
            return;
        }
        switch (kind) {
            case "grass_slab" -> grassSlabsMigratedGrassSlabItems += count;
            case "dirt_slab" -> grassSlabsMigratedDirtSlabItems += count;
            case "path_slab" -> grassSlabsMigratedPathSlabItems += count;
            case "turf" -> grassSlabsMigratedTurfItems += count;
            default -> throw new IllegalArgumentException("Unknown migrated item kind: " + kind);
        }
        setDirty();
    }

    public void recordRetainedGrassCarpets(long count) {
        if (count > 0L) {
            grassSlabsRetainedGrassCarpets += count;
            setDirty();
        }
    }

    public void recordUnsupported(String id, long count) {
        if (count > 0) {
            unsupported.merge(id, count, Long::sum);
            setDirty();
        }
    }

    public int schemaVersion() {
        return SCHEMA_VERSION;
    }

    public long migratedChunks() {
        return migratedChunks;
    }

    public long migratedGrassBlocks() {
        return migratedGrassBlocks;
    }

    public long migratedDirtBlocks() {
        return migratedDirtBlocks;
    }

    public long migratedGrassBlocksTop() {
        return migratedGrassBlocksTop;
    }

    public long migratedGrassBlocksBottom() {
        return migratedGrassBlocksBottom;
    }

    public long migratedDirtBlocksTop() {
        return migratedDirtBlocksTop;
    }

    public long migratedDirtBlocksBottom() {
        return migratedDirtBlocksBottom;
    }

    public long migratedGrassItems() {
        return migratedGrassItems;
    }

    public long migratedDirtItems() {
        return migratedDirtItems;
    }

    public long grassSlabsMigratedChunks() {
        return grassSlabsMigratedChunks;
    }

    public long grassSlabsMigratedGrassSlabBlocks() {
        return grassSlabsMigratedGrassSlabBlocks;
    }

    public long grassSlabsMigratedDirtSlabBlocks() {
        return grassSlabsMigratedDirtSlabBlocks;
    }

    public long grassSlabsMigratedPathSlabBlocks() {
        return grassSlabsMigratedPathSlabBlocks;
    }

    public long grassSlabsMigratedTurfBlocks() {
        return grassSlabsMigratedTurfBlocks;
    }

    public long grassSlabsMigratedGrassSlabItems() {
        return grassSlabsMigratedGrassSlabItems;
    }

    public long grassSlabsMigratedDirtSlabItems() {
        return grassSlabsMigratedDirtSlabItems;
    }

    public long grassSlabsMigratedPathSlabItems() {
        return grassSlabsMigratedPathSlabItems;
    }

    public long grassSlabsMigratedTurfItems() {
        return grassSlabsMigratedTurfItems;
    }

    public long grassSlabsRetainedGrassCarpets() {
        return grassSlabsRetainedGrassCarpets;
    }

    public Map<String, Long> unsupported() {
        return Collections.unmodifiableMap(unsupported);
    }

    private void load(CompoundTag tag) {
        migratedChunks = tag.getLongOr("migrated_chunks", 0L);
        migratedGrassBlocks = tag.getLongOr("migrated_grass_blocks", 0L);
        migratedGrassBlocksTop = tag.getLongOr("migrated_grass_blocks_top", 0L);
        migratedGrassBlocksBottom = tag.getLongOr("migrated_grass_blocks_bottom", 0L);
        migratedDirtBlocks = tag.getLongOr("migrated_dirt_blocks", 0L);
        migratedDirtBlocksTop = tag.getLongOr("migrated_dirt_blocks_top", 0L);
        migratedDirtBlocksBottom = tag.getLongOr("migrated_dirt_blocks_bottom", 0L);
        migratedGrassItems = tag.getLongOr("migrated_grass_items", 0L);
        migratedDirtItems = tag.getLongOr("migrated_dirt_items", 0L);
        grassSlabsMigratedChunks = tag.getLongOr("grassslabs_migrated_chunks", 0L);
        grassSlabsMigratedGrassSlabBlocks =
                tag.getLongOr("grassslabs_migrated_grass_slab_blocks", 0L);
        grassSlabsMigratedDirtSlabBlocks =
                tag.getLongOr("grassslabs_migrated_dirt_slab_blocks", 0L);
        grassSlabsMigratedPathSlabBlocks =
                tag.getLongOr("grassslabs_migrated_path_slab_blocks", 0L);
        grassSlabsMigratedTurfBlocks = tag.getLongOr(
                "grassslabs_migrated_turf_blocks", 0L);
        grassSlabsMigratedGrassSlabItems =
                tag.getLongOr("grassslabs_migrated_grass_slab_items", 0L);
        grassSlabsMigratedDirtSlabItems =
                tag.getLongOr("grassslabs_migrated_dirt_slab_items", 0L);
        grassSlabsMigratedPathSlabItems =
                tag.getLongOr("grassslabs_migrated_path_slab_items", 0L);
        grassSlabsMigratedTurfItems = tag.getLongOr("grassslabs_migrated_turf_items", 0L);
        grassSlabsRetainedGrassCarpets =
                tag.getLongOr("grassslabs_retained_grass_carpet_blocks", 0L);
        unsupported.clear();
        ListTag list = tag.getListOrEmpty("unsupported");
        for (int index = 0; index < list.size(); ++index) {
            CompoundTag entry = list.getCompoundOrEmpty(index);
            unsupported.put(entry.getStringOr("id", ""), entry.getLongOr("count", 0L));
        }
    }

    private CompoundTag saveTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema_version", SCHEMA_VERSION);
        tag.putInt("buildingbricks_migration_version", MIGRATION_VERSION);
        tag.putInt("grassslabs_migration_version", MIGRATION_VERSION);
        tag.putLong("migrated_chunks", migratedChunks);
        tag.putLong("migrated_grass_blocks", migratedGrassBlocks);
        tag.putLong("migrated_grass_blocks_top", migratedGrassBlocksTop);
        tag.putLong("migrated_grass_blocks_bottom", migratedGrassBlocksBottom);
        tag.putLong("migrated_dirt_blocks", migratedDirtBlocks);
        tag.putLong("migrated_dirt_blocks_top", migratedDirtBlocksTop);
        tag.putLong("migrated_dirt_blocks_bottom", migratedDirtBlocksBottom);
        tag.putLong("migrated_grass_items", migratedGrassItems);
        tag.putLong("migrated_dirt_items", migratedDirtItems);
        tag.putLong("grassslabs_migrated_chunks", grassSlabsMigratedChunks);
        tag.putLong("grassslabs_migrated_grass_slab_blocks",
                grassSlabsMigratedGrassSlabBlocks);
        tag.putLong("grassslabs_migrated_dirt_slab_blocks",
                grassSlabsMigratedDirtSlabBlocks);
        tag.putLong("grassslabs_migrated_path_slab_blocks",
                grassSlabsMigratedPathSlabBlocks);
        tag.putLong("grassslabs_migrated_turf_blocks", grassSlabsMigratedTurfBlocks);
        tag.putLong("grassslabs_migrated_grass_slab_items",
                grassSlabsMigratedGrassSlabItems);
        tag.putLong("grassslabs_migrated_dirt_slab_items",
                grassSlabsMigratedDirtSlabItems);
        tag.putLong("grassslabs_migrated_path_slab_items",
                grassSlabsMigratedPathSlabItems);
        tag.putLong("grassslabs_migrated_turf_items", grassSlabsMigratedTurfItems);
        tag.putLong("grassslabs_retained_grass_carpet_blocks",
                grassSlabsRetainedGrassCarpets);
        ListTag list = new ListTag();
        unsupported.forEach((id, count) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            entry.putLong("count", count);
            list.add(entry);
        });
        tag.put("unsupported", list);
        return tag;
    }
}
