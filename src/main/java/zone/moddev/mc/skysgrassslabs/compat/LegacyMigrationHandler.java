package zone.moddev.mc.skysgrassslabs.compat;

import java.util.ArrayList;
import net.minecraft.world.level.block.Block;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Converts the hidden legacy item holders as their owning containers load. */
public final class LegacyMigrationHandler {
    private static final ResourceLocation GRASS_PATH =
            new ResourceLocation("minecraft", "grass_path");
    private static final ResourceLocation DIRT_PATH =
            new ResourceLocation("minecraft", "dirt_path");
    private static final ResourceLocation SWEET_BERRIES_PICK =
            new ResourceLocation("minecraft", "item.sweet_berries.pick_from_bush");
    private static final ResourceLocation SWEET_BERRY_BUSH_PICK =
            new ResourceLocation("minecraft", "block.sweet_berry_bush.pick_berries");

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(LegacyMigrationHandler::loadChunk);
        MinecraftForge.EVENT_BUS.addListener(LegacyMigrationHandler::playerLogin);
        MinecraftForge.EVENT_BUS.addListener(LegacyMigrationHandler::entityJoin);
    }

    public static void loadChunk(ChunkDataEvent.Load event) {
        if (!(event.getWorld() instanceof Level) || !(event.getChunk() instanceof LevelChunk)) return;
        Level world = (Level) event.getWorld();
        if (world.isClientSide || !BuildingBricksCompat.hasLegacyAliases()) return;
        ModWorldState state = ModWorldState.get(world);
        boolean changed = migrateStacksInNbt(event.getData(), state);
        LevelChunk chunk = (LevelChunk) event.getChunk();
        changed |= migrateChunkInventories(chunk, state);
        if (changed) chunk.setUnsaved(true);
    }

    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!BuildingBricksCompat.hasLegacyAliases()) {
            return;
        }
        Player player = event.getPlayer();
        ModWorldState state = ModWorldState.get(player.level);
        migrateInventory(player.getInventory(), state);
        migrateInventory(player.getEnderChestInventory(), state);
    }

    public static void entityJoin(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide || event.getEntity() instanceof Player ||
                !BuildingBricksCompat.hasLegacyAliases()) {
            return;
        }
        Entity entity = event.getEntity();
        CompoundTag serialized = entity.saveWithoutId(new CompoundTag());
        if (migrateStacksInNbt(serialized, ModWorldState.get((Level) event.getWorld()))) {
            entity.load(serialized);
        }
    }

    public static void remapMissingBlocks(RegistryEvent.MissingMappings<Block> event) {
        for (RegistryEvent.MissingMappings.Mapping<Block> mapping : event.getAllMappings()) {
            if (GRASS_PATH.equals(mapping.key)) {
                remap(mapping, ForgeRegistries.BLOCKS.getValue(DIRT_PATH));
                continue;
            }
            if (BuildingBricksCompat.hasLegacyAliases()) continue;
            LegacySlabKind kind = legacySlabKind(mapping.key);
            if (kind != null) mapping.remap(kind == LegacySlabKind.GRASS
                    ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB);
        }
    }

    public static void remapMissingItems(RegistryEvent.MissingMappings<Item> event) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (GRASS_PATH.equals(mapping.key)) {
                remap(mapping, ForgeRegistries.ITEMS.getValue(DIRT_PATH));
                continue;
            }
            if (BuildingBricksCompat.hasLegacyAliases()) continue;
            LegacySlabKind kind = legacySlabKind(mapping.key);
            if (kind != null) mapping.remap((kind == LegacySlabKind.GRASS
                    ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB).asItem());
        }
    }

    public static void remapMissingSounds(RegistryEvent.MissingMappings<SoundEvent> event) {
        for (RegistryEvent.MissingMappings.Mapping<SoundEvent> mapping : event.getAllMappings()) {
            if (SWEET_BERRIES_PICK.equals(mapping.key)) {
                remap(mapping, ForgeRegistries.SOUND_EVENTS.getValue(SWEET_BERRY_BUSH_PICK));
            }
        }
    }

    private static <T extends IForgeRegistryEntry<T>> void remap(
            RegistryEvent.MissingMappings.Mapping<T> mapping,
            T replacement) {
        if (replacement != null) {
            mapping.remap(replacement);
        } else {
            mapping.warn();
        }
    }

    static LegacySlabKind legacySlabKind(ResourceLocation id) {
        if (id.equals(BuildingBricksCompat.GRASS_SLAB_ID) ||
                id.equals(BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID)) {
            return LegacySlabKind.GRASS;
        }
        return id.equals(BuildingBricksCompat.DIRT_SLAB_ID) ? LegacySlabKind.DIRT : null;
    }

    public static boolean migrateStacksInNbt(Tag tag, ModWorldState state) {
        boolean changed = false;
        if (tag instanceof CompoundTag) {
            CompoundTag compound = (CompoundTag) tag;
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
            for (String key : new ArrayList<String>(compound.getAllKeys())) {
                Tag child = compound.get(key);
                if (child != null) changed |= migrateStacksInNbt(child, state);
            }
        } else if (tag instanceof ListTag) {
            ListTag list = (ListTag) tag;
            for (int index = 0; index < list.size(); ++index) {
                changed |= migrateStacksInNbt(list.get(index), state);
            }
        }
        return changed;
    }

    private static boolean migrateChunkInventories(LevelChunk chunk, ModWorldState state) {
        boolean changed = false;
        for (BlockEntity tileEntity : chunk.getBlockEntities().values()) {
            CompoundTag serialized = tileEntity.save(new CompoundTag());
            if (migrateStacksInNbt(serialized, state)) {
                tileEntity.load(serialized);
                tileEntity.setChanged();
                changed = true;
            }
        }
        return changed;
    }

    private static void migrateInventory(Container inventory, ModWorldState state) {
        boolean changed = false;
        for (int slot = 0; slot < inventory.getContainerSize(); ++slot) {
            ItemStack migrated = migrateStack(inventory.getItem(slot), state);
            if (!migrated.isEmpty()) {
                inventory.setItem(slot, migrated);
                changed = true;
            }
        }
        if (changed) inventory.setChanged();
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
