package zone.moddev.mc.skysgrassslabs.compat;

import java.util.ArrayList;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Converts the hidden legacy item holders as their owning containers load. */
public final class LegacyMigrationHandler {
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(LegacyMigrationHandler::loadChunk);
        MinecraftForge.EVENT_BUS.addListener(LegacyMigrationHandler::playerLogin);
    }

    public static void loadChunk(ChunkDataEvent.Load event) {
        if (!(event.getWorld() instanceof World) || !(event.getChunk() instanceof Chunk)) return;
        World world = (World) event.getWorld();
        if (world.isRemote || !BuildingBricksCompat.hasLegacyAliases()) return;
        ModWorldState state = ModWorldState.get(world);
        boolean changed = migrateStacksInNbt(event.getData(), state);
        Chunk chunk = (Chunk) event.getChunk();
        changed |= migrateChunkInventories(chunk, state);
        if (changed) chunk.setModified(true);
    }

    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!BuildingBricksCompat.hasLegacyAliases()) {
            return;
        }
        PlayerEntity player = event.getPlayer();
        ModWorldState state = ModWorldState.get(player.world);
        migrateInventory(player.inventory, state);
        migrateInventory(player.getInventoryEnderChest(), state);
    }

    public static void remapMissingBlocks(RegistryEvent.MissingMappings<Block> event) {
        if (BuildingBricksCompat.hasLegacyAliases()) return;
        for (RegistryEvent.MissingMappings.Mapping<Block> mapping : event.getAllMappings()) {
            LegacySlabKind kind = legacySlabKind(mapping.key);
            if (kind != null) mapping.remap(kind == LegacySlabKind.GRASS
                    ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB);
        }
    }

    public static void remapMissingItems(RegistryEvent.MissingMappings<Item> event) {
        if (BuildingBricksCompat.hasLegacyAliases()) return;
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            LegacySlabKind kind = legacySlabKind(mapping.key);
            if (kind != null) mapping.remap((kind == LegacySlabKind.GRASS
                    ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB).asItem());
        }
    }

    static LegacySlabKind legacySlabKind(ResourceLocation id) {
        if (id.equals(BuildingBricksCompat.GRASS_SLAB_ID) ||
                id.equals(BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID)) {
            return LegacySlabKind.GRASS;
        }
        return id.equals(BuildingBricksCompat.DIRT_SLAB_ID) ? LegacySlabKind.DIRT : null;
    }

    public static boolean migrateStacksInNbt(INBT tag, ModWorldState state) {
        boolean changed = false;
        if (tag instanceof CompoundNBT) {
            CompoundNBT compound = (CompoundNBT) tag;
            if (compound.contains("id", 8) && compound.contains("Count", 99)) {
                String id = compound.getString("id");
                boolean grass = BuildingBricksCompat.GRASS_SLAB_ID.toString().equals(id) ||
                        BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID.toString().equals(id);
                boolean dirt = BuildingBricksCompat.DIRT_SLAB_ID.toString().equals(id);
                if (grass || dirt) {
                    compound.putString("id", (grass ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB)
                            .getRegistryName().toString());
                    int count = compound.getByte("Count") & 255;
                    if (state != null && grass) state.recordGrassItems(count);
                    if (state != null && dirt) state.recordDirtItems(count);
                    changed = true;
                }
            }
            for (String key : new ArrayList<String>(compound.keySet())) {
                INBT child = compound.get(key);
                if (child != null) changed |= migrateStacksInNbt(child, state);
            }
        } else if (tag instanceof ListNBT) {
            ListNBT list = (ListNBT) tag;
            for (int index = 0; index < list.size(); ++index) {
                changed |= migrateStacksInNbt(list.get(index), state);
            }
        }
        return changed;
    }

    private static boolean migrateChunkInventories(Chunk chunk, ModWorldState state) {
        boolean changed = false;
        for (TileEntity tileEntity : chunk.getTileEntityMap().values()) {
            CompoundNBT serialized = tileEntity.write(new CompoundNBT());
            if (migrateStacksInNbt(serialized, state)) {
                tileEntity.read(serialized);
                tileEntity.markDirty();
                changed = true;
            }
        }
        for (ClassInheritanceMultiMap<Entity> list : chunk.getEntityLists()) {
            for (Entity entity : list) {
                CompoundNBT serialized = entity.writeWithoutTypeId(new CompoundNBT());
                if (migrateStacksInNbt(serialized, state)) {
                    entity.read(serialized);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static void migrateInventory(IInventory inventory, ModWorldState state) {
        boolean changed = false;
        for (int slot = 0; slot < inventory.getSizeInventory(); ++slot) {
            ItemStack migrated = migrateStack(inventory.getStackInSlot(slot), state);
            if (!migrated.isEmpty()) {
                inventory.setInventorySlotContents(slot, migrated);
                changed = true;
            }
        }
        if (changed) inventory.markDirty();
    }

    private static ItemStack migrateStack(ItemStack stack, ModWorldState state) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        Item oldItem = stack.getItem();
        boolean grass = BuildingBricksCompat.grassSlab() != null &&
                oldItem == BuildingBricksCompat.grassSlab().asItem() ||
                BuildingBricksCompat.historicalGrassSlab() != null &&
                        oldItem == BuildingBricksCompat.historicalGrassSlab().asItem();
        boolean dirt = BuildingBricksCompat.dirtSlab() != null &&
                oldItem == BuildingBricksCompat.dirtSlab().asItem();
        if (!grass && !dirt) return ItemStack.EMPTY;
        ItemStack migrated = new ItemStack(grass ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB,
                stack.getCount());
        if (stack.hasTag()) migrated.setTag(stack.getTag().copy());
        if (state != null && grass) state.recordGrassItems(stack.getCount());
        if (state != null && dirt) state.recordDirtItems(stack.getCount());
        return migrated;
    }

    enum LegacySlabKind {
        GRASS,
        DIRT
    }

    private LegacyMigrationHandler() {
    }
}
