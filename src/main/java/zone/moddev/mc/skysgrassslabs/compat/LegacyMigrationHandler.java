package zone.moddev.mc.skysgrassslabs.compat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

public final class LegacyMigrationHandler {
    private static final String CHUNK_MARKER = "skysgrassslabs";
    private static final String CHUNK_MIGRATION_VERSION = "buildingbricks_migration_version";

    private final Map<ChunkKey, Integer> chunkMarkersToSave =
            new HashMap<ChunkKey, Integer>();

    @SubscribeEvent
    public void loadChunk(ChunkDataEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote) return;
        NBTTagCompound marker = event.getData().getCompoundTag(CHUNK_MARKER);
        int markerVersion = marker.getInteger(CHUNK_MIGRATION_VERSION);
        Chunk chunk = event.getChunk();
        ChunkKey chunkKey = ChunkKey.of(world, chunk);
        if (BuildingBricksCompat.hasLegacyAliases()) {
            boolean changed = migrateBlocks(chunk, event.getData(), null);
            changed |= migrateChunkInventories(chunk, null);
            if (changed) chunk.setModified(true);
            return;
        }
        if (shouldPreserveChunkMarker(markerVersion)) {
            chunkMarkersToSave.put(chunkKey, markerVersion);
            return;
        }
        if (!shouldMigrateChunk(BuildingBricksCompat.shouldReplaceSlabs(), markerVersion)) return;
        BuildingBricksCompat.resolveBlocks();
        if (BuildingBricksCompat.grassSlab() == null || BuildingBricksCompat.dirtSlab() == null) {
            SkysGrassSlabs.logger.error("BuildingBricks is loaded but its grass or dirt slab is not registered");
            return;
        }

        ModWorldState state = ModWorldState.get(world);
        migrateBlocks(chunk, event.getData(), state);
        migrateChunkInventories(chunk, state);
        state.recordChunk();
        chunk.setModified(true);
        chunkMarkersToSave.put(chunkKey, ModWorldState.MIGRATION_VERSION);
    }

    @SubscribeEvent
    public void saveChunk(ChunkDataEvent.Save event) {
        Integer markerVersion = chunkMarkersToSave.get(
                ChunkKey.of(event.getWorld(), event.getChunk()));
        if (markerVersion == null) return;
        NBTTagCompound marker = event.getData().hasKey(CHUNK_MARKER, 10)
                ? event.getData().getCompoundTag(CHUNK_MARKER) : new NBTTagCompound();
        marker.setInteger(CHUNK_MIGRATION_VERSION, markerVersion.intValue());
        event.getData().setTag(CHUNK_MARKER, marker);
    }

    @SubscribeEvent
    public void convertPlacedBlock(BlockEvent.PlaceEvent event) {
        World world = event.getWorld();
        if (world.isRemote || (!BuildingBricksCompat.shouldReplaceSlabs() &&
                !BuildingBricksCompat.hasLegacyAliases())) {
            return;
        }
        IBlockState placed = event.getPlacedBlock();
        IBlockState replacement = replacementFor(placed);
        if (replacement != null && world.setBlockState(event.getPos(), replacement, 3)) {
            ModWorldState state = ModWorldState.get(world);
            int metadata = placed.getBlock().getMetaFromState(placed) & 1;
            if (placed.getBlock() == BuildingBricksCompat.grassSlab()) {
                state.recordGrassBlocks(1, metadata);
            }
            if (placed.getBlock() == BuildingBricksCompat.dirtSlab()) {
                state.recordDirtBlocks(1, metadata);
            }
        }
    }

    @SubscribeEvent
    public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayer) || (!BuildingBricksCompat.shouldReplaceSlabs() &&
                !BuildingBricksCompat.hasLegacyAliases())) {
            return;
        }
        EntityPlayer player = event.player;
        ModWorldState state = BuildingBricksCompat.hasLegacyAliases() ? null
                : ModWorldState.get(player.getEntityWorld());
        migrateInventory(player.inventory, state);
        migrateInventory(player.getInventoryEnderChest(), state);
    }

    @SubscribeEvent
    public void saveWorld(WorldEvent.Save event) {
        World world = event.getWorld();
        if (world.isRemote || world.provider.getDimension() != 0 ||
                !BuildingBricksCompat.shouldReplaceSlabs()) {
            return;
        }
        writeReport(world, ModWorldState.get(world));
    }

    public static void remapMissingBlocks(RegistryEvent.MissingMappings<Block> event) {
        if (BuildingBricksCompat.hasLegacyAliases()) return;
        for (RegistryEvent.MissingMappings.Mapping<Block> mapping : event.getAllMappings()) {
            LegacySlabKind kind = legacySlabKind(mapping.key);
            if (kind == null) continue;
            mapping.remap(kind == LegacySlabKind.GRASS
                    ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB);
        }
    }

    public static void remapMissingItems(RegistryEvent.MissingMappings<Item> event) {
        if (BuildingBricksCompat.hasLegacyAliases()) return;
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            LegacySlabKind kind = legacySlabKind(mapping.key);
            if (kind == null) continue;
            mapping.remap(Item.getItemFromBlock(kind == LegacySlabKind.GRASS
                    ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB));
        }
    }

    static LegacySlabKind legacySlabKind(ResourceLocation id) {
        if (id.equals(BuildingBricksCompat.GRASS_SLAB_ID) ||
                id.equals(BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID)) {
            return LegacySlabKind.GRASS;
        }
        return id.equals(BuildingBricksCompat.DIRT_SLAB_ID) ? LegacySlabKind.DIRT : null;
    }

    static boolean shouldMigrateChunk(boolean replacementEnabled, int markerVersion) {
        return replacementEnabled && markerVersion < ModWorldState.MIGRATION_VERSION;
    }

    static boolean shouldPreserveChunkMarker(int markerVersion) {
        return markerVersion >= ModWorldState.MIGRATION_VERSION;
    }

    private static boolean migrateBlocks(Chunk chunk, NBTTagCompound chunkData,
            ModWorldState worldState) {
        long grassTop = 0;
        long grassBottom = 0;
        long dirtTop = 0;
        long dirtBottom = 0;
        Map<String, Long> unsupported = new TreeMap<String, Long>();
        ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        NBTTagList serializedSections = chunkData.getCompoundTag("Level")
                .getTagList("Sections", 10);
        Map<Integer, String> buildingBricksIds =
                BuildingBricksCompat.buildingBricksBlockIdsByNumericId();
        int grassId = Block.getIdFromBlock(BuildingBricksCompat.grassSlab());
        int historicalGrassId = BuildingBricksCompat.historicalGrassSlab() == null ? -1
                : Block.getIdFromBlock(BuildingBricksCompat.historicalGrassSlab());
        int dirtId = Block.getIdFromBlock(BuildingBricksCompat.dirtSlab());
        for (int sectionIndex = 0; sectionIndex < serializedSections.tagCount(); ++sectionIndex) {
            NBTTagCompound serializedSection = serializedSections.getCompoundTagAt(sectionIndex);
            int ySection = serializedSection.getByte("Y") & 255;
            if (ySection >= storage.length || storage[ySection] == null) continue;
            byte[] blocks = serializedSection.getByteArray("Blocks");
            if (blocks.length != 4096) continue;
            NibbleArray metadata = new NibbleArray(serializedSection.getByteArray("Data"));
            NibbleArray add = serializedSection.hasKey("Add", 7)
                    ? new NibbleArray(serializedSection.getByteArray("Add")) : null;
            ExtendedBlockStorage section = storage[ySection];
            for (int index = 0; index < blocks.length; ++index) {
                int numericId = (blocks[index] & 255) |
                        (add == null ? 0 : add.getFromIndex(index) << 8);
                if (numericId == grassId || numericId == historicalGrassId || numericId == dirtId) {
                    int slabMetadata = metadata.getFromIndex(index) & 1;
                    section.set(index & 15, index >> 8 & 15, index >> 4 & 15,
                            skyStateFor(numericId == grassId || numericId == historicalGrassId,
                                    slabMetadata));
                    if (numericId == grassId || numericId == historicalGrassId) {
                        if (slabMetadata == 0) ++grassTop;
                        else ++grassBottom;
                    } else {
                        if (slabMetadata == 0) ++dirtTop;
                        else ++dirtBottom;
                    }
                } else {
                    String id = buildingBricksIds.get(numericId);
                    if (id != null) {
                        unsupported.put(id, unsupported.containsKey(id)
                                ? unsupported.get(id) + 1 : 1);
                    }
                }
            }
        }
        if (worldState != null) {
            worldState.recordGrassBlocks(grassTop, 0);
            worldState.recordGrassBlocks(grassBottom, 1);
            worldState.recordDirtBlocks(dirtTop, 0);
            worldState.recordDirtBlocks(dirtBottom, 1);
            for (Map.Entry<String, Long> entry : unsupported.entrySet()) {
                worldState.recordUnsupported("block:" + entry.getKey(), entry.getValue());
            }
        }
        return grassTop + grassBottom + dirtTop + dirtBottom > 0;
    }

    private static IBlockState replacementFor(IBlockState oldState) {
        Block oldBlock = oldState.getBlock();
        if (oldBlock != BuildingBricksCompat.grassSlab() &&
                oldBlock != BuildingBricksCompat.historicalGrassSlab() &&
                oldBlock != BuildingBricksCompat.dirtSlab()) {
            return null;
        }
        int metadata = oldBlock.getMetaFromState(oldState) & 1;
        return skyStateFor(oldBlock == BuildingBricksCompat.grassSlab() ||
                oldBlock == BuildingBricksCompat.historicalGrassSlab(), metadata);
    }

    static IBlockState skyStateFor(boolean grass, int metadata) {
        return (grass ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB)
                .getStateFromMeta(metadata & 1);
    }

    private static boolean migrateChunkInventories(Chunk chunk, ModWorldState state) {
        boolean changed = false;
        for (TileEntity tileEntity : chunk.getTileEntityMap().values()) {
            NBTTagCompound serialized = tileEntity.writeToNBT(new NBTTagCompound());
            if (migrateStacksInNbt(serialized, state)) {
                tileEntity.readFromNBT(serialized);
                changed = true;
            }
        }
        for (ClassInheritanceMultiMap<Entity> list : chunk.getEntityLists()) {
            for (Entity entity : list) {
                NBTTagCompound serialized = entity.serializeNBT();
                if (migrateStacksInNbt(serialized, state)) {
                    entity.deserializeNBT(serialized);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static void migrateInventory(IInventory inventory, ModWorldState state) {
        for (int slot = 0; slot < inventory.getSizeInventory(); ++slot) {
            ItemStack stack = inventory.getStackInSlot(slot);
            ItemStack migrated = migrateStack(stack, state);
            if (migrated != null) inventory.setInventorySlotContents(slot, migrated);
        }
        inventory.markDirty();
    }

    /**
     * Migrates serialized stacks without calling a tile inventory API. In 1.10 a loot chest's
     * accessor generates its loot and marks the tile dirty; doing that from ChunkDataEvent.Load
     * recursively loads the same chunk. NBT traversal also covers item-handler capabilities and
     * nested container formats while leaving unopened loot tables untouched.
     */
    private static boolean migrateStacksInNbt(NBTBase tag, ModWorldState state) {
        boolean changed = false;
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) tag;
            if (compound.hasKey("id", 8) && compound.hasKey("Count", 99)) {
                String id = compound.getString("id");
                boolean grass = BuildingBricksCompat.GRASS_SLAB_ID.toString().equals(id) ||
                        BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID.toString().equals(id);
                boolean dirt = BuildingBricksCompat.DIRT_SLAB_ID.toString().equals(id);
                if (grass || dirt) {
                    compound.setString("id", (grass ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB)
                            .getRegistryName().toString());
                    compound.setShort("Damage", (short) 0);
                    int count = compound.getByte("Count") & 255;
                    if (state != null && grass) state.recordGrassItems(count);
                    if (state != null && dirt) state.recordDirtItems(count);
                    changed = true;
                }
            }
            for (String key : new ArrayList<String>(compound.getKeySet())) {
                NBTBase child = compound.getTag(key);
                if (child != null) changed |= migrateStacksInNbt(child, state);
            }
        } else if (tag instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) tag;
            for (int index = 0; index < list.tagCount(); ++index) {
                changed |= migrateStacksInNbt(list.get(index), state);
            }
        }
        return changed;
    }

    private static ItemStack migrateStack(ItemStack stack, ModWorldState state) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Item oldItem = stack.getItem();
        Item grassItem = BuildingBricksCompat.grassSlab() == null ? null
                : Item.getItemFromBlock(BuildingBricksCompat.grassSlab());
        Item historicalGrassItem = BuildingBricksCompat.historicalGrassSlab() == null ? null
                : Item.getItemFromBlock(BuildingBricksCompat.historicalGrassSlab());
        Item dirtItem = BuildingBricksCompat.dirtSlab() == null ? null
                : Item.getItemFromBlock(BuildingBricksCompat.dirtSlab());
        if (oldItem == grassItem || oldItem == historicalGrassItem || oldItem == dirtItem) {
            boolean grass = oldItem == grassItem || oldItem == historicalGrassItem;
            ItemStack migrated = new ItemStack(grass
                    ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB, stack.getCount(), 0);
            if (stack.hasTagCompound()) migrated.setTagCompound(stack.getTagCompound().copy());
            if (state != null && grass) state.recordGrassItems(stack.getCount());
            if (state != null && !grass) state.recordDirtItems(stack.getCount());
            return migrated;
        }
        return null;
    }

    private static void writeReport(World world, ModWorldState state) {
        File directory = new File(world.getSaveHandler().getWorldDirectory(), "serverconfig");
        File report = new File(directory, "skysgrassslabs-migration-report.txt");
        File temporary = new File(directory, "skysgrassslabs-migration-report.txt.tmp");
        List<String> lines = migrationReportLines(state);
        try {
            Files.createDirectories(directory.toPath());
            Files.write(temporary.toPath(), lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporary.toPath(), report.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary.toPath(), report.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            SkysGrassSlabs.logger.error("Could not write BuildingBricks migration report", exception);
        }
    }

    static List<String> migrationReportLines(ModWorldState state) {
        List<String> lines = new ArrayList<String>();
        lines.add("Sky's Grass Slabs " + SkysGrassSlabs.VERSION
                + " BuildingBricks Migration Report");
        lines.add("schema_version=1");
        lines.add("migration_version=1");
        lines.add("force_replacement_enabled=true");
        lines.add("migrated_chunks=" + state.migratedChunks());
        lines.add("migrated_grass_blocks=" + state.migratedGrassBlocks());
        lines.add("migrated_grass_blocks_top=" + state.migratedGrassBlocksTop());
        lines.add("migrated_grass_blocks_bottom=" + state.migratedGrassBlocksBottom());
        lines.add("migrated_dirt_blocks=" + state.migratedDirtBlocks());
        lines.add("migrated_dirt_blocks_top=" + state.migratedDirtBlocksTop());
        lines.add("migrated_dirt_blocks_bottom=" + state.migratedDirtBlocksBottom());
        lines.add("migrated_grass_items=" + state.migratedGrassItems());
        lines.add("migrated_dirt_items=" + state.migratedDirtItems());
        lines.add("unsupported_entries=" + state.unsupported().size());
        for (Map.Entry<String, Long> entry : state.unsupported().entrySet()) {
            lines.add("unsupported." + entry.getKey() + "=" + entry.getValue());
        }
        return lines;
    }

    enum LegacySlabKind {
        GRASS,
        DIRT
    }

    /** Stable identity retained between Forge's separate chunk load and save events. */
    private static final class ChunkKey {
        private final int dimension;
        private final int x;
        private final int z;

        private ChunkKey(int dimension, int x, int z) {
            this.dimension = dimension;
            this.x = x;
            this.z = z;
        }

        private static ChunkKey of(World world, Chunk chunk) {
            return new ChunkKey(world.provider.getDimension(), chunk.x, chunk.z);
        }

        @Override
        public int hashCode() {
            int result = dimension;
            result = 31 * result + x;
            return 31 * result + z;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value) return true;
            if (!(value instanceof ChunkKey)) return false;
            ChunkKey other = (ChunkKey) value;
            return dimension == other.dimension && x == other.x && z == other.z;
        }
    }
}
