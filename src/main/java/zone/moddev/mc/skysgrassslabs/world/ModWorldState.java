package zone.moddev.mc.skysgrassslabs.world;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
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
        if (!world.isRemote && world.provider.getDimension() != 0 &&
                world.getMinecraftServer() != null) {
            World overworld = world.getMinecraftServer().getWorld(0);
            if (overworld != null) world = overworld;
        }
        MapStorage storage = world.getMapStorage();
        ModWorldState state = (ModWorldState) storage.getOrLoadData(ModWorldState.class, DATA_NAME);
        if (state == null) {
            state = new ModWorldState(DATA_NAME);
            storage.setData(DATA_NAME, state);
            state.markDirty();
        }
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
    public void readFromNBT(NBTTagCompound nbt) {
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
        NBTTagList list = nbt.getTagList("unsupported", 10);
        for (int index = 0; index < list.tagCount(); ++index) {
            NBTTagCompound entry = list.getCompoundTagAt(index);
            unsupported.put(entry.getString("id"), entry.getLong("count"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("schema_version", SCHEMA_VERSION);
        nbt.setInteger("buildingbricks_migration_version", MIGRATION_VERSION);
        nbt.setLong("migrated_chunks", migratedChunks);
        nbt.setLong("migrated_grass_blocks", migratedGrassBlocks);
        nbt.setLong("migrated_grass_blocks_top", migratedGrassBlocksTop);
        nbt.setLong("migrated_grass_blocks_bottom", migratedGrassBlocksBottom);
        nbt.setLong("migrated_dirt_blocks", migratedDirtBlocks);
        nbt.setLong("migrated_dirt_blocks_top", migratedDirtBlocksTop);
        nbt.setLong("migrated_dirt_blocks_bottom", migratedDirtBlocksBottom);
        nbt.setLong("migrated_grass_items", migratedGrassItems);
        nbt.setLong("migrated_dirt_items", migratedDirtItems);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Long> value : unsupported.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("id", value.getKey());
            entry.setLong("count", value.getValue());
            list.appendTag(entry);
        }
        nbt.setTag("unsupported", list);
        return nbt;
    }
}
