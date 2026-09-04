package zone.moddev.mc.skysgrassslabs.world;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

public final class ModWorldState extends WorldSavedData {
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
    private final Map<String, Long> unsupported = new TreeMap<String, Long>();

    public ModWorldState(String name) {
        super(name);
    }

    public static ModWorldState get(World world) {
        if (!world.isRemote && world.getDimension().getType() != DimensionType.OVERWORLD &&
                world.getServer() != null) {
            ServerWorld overworld = world.getServer().getWorld(DimensionType.OVERWORLD);
            if (overworld != null) world = overworld;
        }
        if (!(world instanceof ServerWorld)) {
            return new ModWorldState(DATA_NAME);
        }
        ModWorldState state = ((ServerWorld) world).getSavedData().getOrCreate(
                () -> new ModWorldState(DATA_NAME), DATA_NAME);
        return state;
    }

    public void recordChunk() {
        ++migratedChunks;
        markDirty();
    }

    public void recordGrassBlocks(long count) {
        migratedGrassBlocks += count;
        markDirty();
    }

    public void recordGrassBlocks(long count, int metadata) {
        if (count <= 0) return;
        recordGrassBlocks(count);
        if ((metadata & 1) == 0) migratedGrassBlocksTop += count;
        else migratedGrassBlocksBottom += count;
    }

    public void recordDirtBlocks(long count) {
        migratedDirtBlocks += count;
        markDirty();
    }

    public void recordDirtBlocks(long count, int metadata) {
        if (count <= 0) return;
        recordDirtBlocks(count);
        if ((metadata & 1) == 0) migratedDirtBlocksTop += count;
        else migratedDirtBlocksBottom += count;
    }

    public void recordGrassItems(long count) {
        migratedGrassItems += count;
        markDirty();
    }

    public void recordDirtItems(long count) {
        migratedDirtItems += count;
        markDirty();
    }

    public void recordUnsupported(String id, long count) {
        if (count <= 0) return;
        unsupported.put(id, unsupported.containsKey(id) ? unsupported.get(id) + count : count);
        markDirty();
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

    @Override
    public void read(CompoundNBT nbt) {
        migratedChunks = nbt.getLong("migrated_chunks");
        migratedGrassBlocks = nbt.getLong("migrated_grass_blocks");
        migratedGrassBlocksTop = nbt.getLong("migrated_grass_blocks_top");
        migratedGrassBlocksBottom = nbt.getLong("migrated_grass_blocks_bottom");
        migratedDirtBlocks = nbt.getLong("migrated_dirt_blocks");
        migratedDirtBlocksTop = nbt.getLong("migrated_dirt_blocks_top");
        migratedDirtBlocksBottom = nbt.getLong("migrated_dirt_blocks_bottom");
        migratedGrassItems = nbt.getLong("migrated_grass_items");
        migratedDirtItems = nbt.getLong("migrated_dirt_items");
        unsupported.clear();
        ListNBT list = nbt.getList("unsupported", 10);
        for (int index = 0; index < list.size(); ++index) {
            CompoundNBT entry = list.getCompound(index);
            unsupported.put(entry.getString("id"), entry.getLong("count"));
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        nbt.putInt("schema_version", SCHEMA_VERSION);
        nbt.putInt("buildingbricks_migration_version", MIGRATION_VERSION);
        nbt.putLong("migrated_chunks", migratedChunks);
        nbt.putLong("migrated_grass_blocks", migratedGrassBlocks);
        nbt.putLong("migrated_grass_blocks_top", migratedGrassBlocksTop);
        nbt.putLong("migrated_grass_blocks_bottom", migratedGrassBlocksBottom);
        nbt.putLong("migrated_dirt_blocks", migratedDirtBlocks);
        nbt.putLong("migrated_dirt_blocks_top", migratedDirtBlocksTop);
        nbt.putLong("migrated_dirt_blocks_bottom", migratedDirtBlocksBottom);
        nbt.putLong("migrated_grass_items", migratedGrassItems);
        nbt.putLong("migrated_dirt_items", migratedDirtItems);
        ListNBT list = new ListNBT();
        for (Map.Entry<String, Long> value : unsupported.entrySet()) {
            CompoundNBT entry = new CompoundNBT();
            entry.putString("id", value.getKey());
            entry.putLong("count", value.getValue());
            list.add(entry);
        }
        nbt.put("unsupported", list);
        return nbt;
    }
}
