package zone.moddev.mc.skysgrassslabs.world;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent schema and aggregate historical-slab migration totals. */
public final class ModWorldState extends SavedData {
    public static final String DATA_NAME = "skysgrassslabs_world_state";
    public static final int SCHEMA_VERSION = 1;
    public static final int MIGRATION_VERSION = 1;

    private long migratedChunks;
    private long migratedGrassBlocks;
    private long migratedGrassBlocksTop;
    private long migratedGrassBlocksBottom;
    private long migratedDirtBlocks;
    private long migratedDirtBlocksTop;
    private long migratedDirtBlocksBottom;
    private long migratedGrassItems;
    private long migratedDirtItems;
    private final Map<String, Long> unsupported = new TreeMap<>();

    public ModWorldState() {
    }

    private ModWorldState(CompoundTag tag) {
        load(tag);
    }

    public static ModWorldState get(Level level) {
        if (!level.isClientSide && level.dimension() != Level.OVERWORLD && level.getServer() != null) {
            ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                level = overworld;
            }
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return new ModWorldState();
        }
        return serverLevel.getDataStorage().computeIfAbsent(
                ModWorldState::new, ModWorldState::new, DATA_NAME);
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

    public Map<String, Long> unsupported() {
        return Collections.unmodifiableMap(unsupported);
    }

    private void load(CompoundTag tag) {
        migratedChunks = tag.getLong("migrated_chunks");
        migratedGrassBlocks = tag.getLong("migrated_grass_blocks");
        migratedGrassBlocksTop = tag.getLong("migrated_grass_blocks_top");
        migratedGrassBlocksBottom = tag.getLong("migrated_grass_blocks_bottom");
        migratedDirtBlocks = tag.getLong("migrated_dirt_blocks");
        migratedDirtBlocksTop = tag.getLong("migrated_dirt_blocks_top");
        migratedDirtBlocksBottom = tag.getLong("migrated_dirt_blocks_bottom");
        migratedGrassItems = tag.getLong("migrated_grass_items");
        migratedDirtItems = tag.getLong("migrated_dirt_items");
        unsupported.clear();
        ListTag list = tag.getList("unsupported", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); ++index) {
            CompoundTag entry = list.getCompound(index);
            unsupported.put(entry.getString("id"), entry.getLong("count"));
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("schema_version", SCHEMA_VERSION);
        tag.putInt("buildingbricks_migration_version", MIGRATION_VERSION);
        tag.putLong("migrated_chunks", migratedChunks);
        tag.putLong("migrated_grass_blocks", migratedGrassBlocks);
        tag.putLong("migrated_grass_blocks_top", migratedGrassBlocksTop);
        tag.putLong("migrated_grass_blocks_bottom", migratedGrassBlocksBottom);
        tag.putLong("migrated_dirt_blocks", migratedDirtBlocks);
        tag.putLong("migrated_dirt_blocks_top", migratedDirtBlocksTop);
        tag.putLong("migrated_dirt_blocks_bottom", migratedDirtBlocksBottom);
        tag.putLong("migrated_grass_items", migratedGrassItems);
        tag.putLong("migrated_dirt_items", migratedDirtItems);
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
