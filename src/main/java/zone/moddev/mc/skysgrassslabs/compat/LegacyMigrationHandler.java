package zone.moddev.mc.skysgrassslabs.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Converts supported historical slab blocks and item stacks as their owners load. */
public final class LegacyMigrationHandler {
    private static final String CHUNK_MARKER = "skysgrassslabs_buildingbricks_migration_version";
    private static final Set<LevelChunk> MIGRATED_CHUNKS =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LegacyMigrationHandler::loadChunk);
        NeoForge.EVENT_BUS.addListener(LegacyMigrationHandler::saveChunk);
        NeoForge.EVENT_BUS.addListener(LegacyMigrationHandler::playerLogin);
        NeoForge.EVENT_BUS.addListener(LegacyMigrationHandler::entityJoin);
        NeoForge.EVENT_BUS.addListener(LegacyMigrationHandler::blockPlaced);
    }

    public static void loadChunk(ChunkDataEvent.Load event) {
        if (!(event.getLevel() instanceof Level level)
                || !(event.getChunk() instanceof LevelChunk chunk)
                || level.isClientSide || !replacementEnabled()) {
            return;
        }
        ModWorldState state = ModWorldState.get(level);
        boolean changed = migrateStacksInNbt(event.getData(), state);
        changed |= migrateChunkInventories(chunk, state);
        changed |= migrateChunkBlocks(chunk, event.getData(), state);
        if (changed) {
            chunk.setUnsaved(true);
        }
    }

    public static void saveChunk(ChunkDataEvent.Save event) {
        if (event.getChunk() instanceof LevelChunk chunk && MIGRATED_CHUNKS.remove(chunk)) {
            event.getData().putInt(CHUNK_MARKER, ModWorldState.MIGRATION_VERSION);
        }
    }

    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!replacementEnabled()) {
            return;
        }
        Player player = event.getEntity();
        ModWorldState state = ModWorldState.get(player.level());
        migrateInventory(player.getInventory(), state);
        migrateInventory(player.getEnderChestInventory(), state);
    }

    public static void entityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity() instanceof Player
                || !replacementEnabled()) {
            return;
        }
        Level level = event.getLevel();
        Entity entity = event.getEntity();
        CompoundTag serialized = entity.saveWithoutId(new CompoundTag());
        if (migrateStacksInNbt(serialized, ModWorldState.get(level))) {
            entity.load(serialized);
        }
    }

    public static void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide
                || !BuildingBricksCompat.isInstalled()
                || !SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs()) {
            return;
        }
        LegacySlabKind kind = legacySlabKind(event.getPlacedBlock().getBlock());
        if (kind != null) {
            level.setBlock(event.getPos(), replacement(event.getPlacedBlock(), kind),
                    Block.UPDATE_ALL);
        }
    }

    static LegacySlabKind legacySlabKind(ResourceLocation id) {
        if (BuildingBricksCompat.GRASS_SLAB_ID.equals(id)
                || BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID.equals(id)) {
            return LegacySlabKind.GRASS;
        }
        return BuildingBricksCompat.DIRT_SLAB_ID.equals(id) ? LegacySlabKind.DIRT : null;
    }

    private static LegacySlabKind legacySlabKind(Block block) {
        if (block == BuildingBricksCompat.grassSlab()
                || block == BuildingBricksCompat.historicalGrassSlab()) {
            return LegacySlabKind.GRASS;
        }
        return block == BuildingBricksCompat.dirtSlab() ? LegacySlabKind.DIRT : null;
    }

    public static boolean migrateStacksInNbt(Tag tag, ModWorldState state) {
        boolean changed = false;
        if (tag instanceof CompoundTag compound) {
            if (compound.contains("id", Tag.TAG_STRING) && hasStackCount(compound)) {
                LegacySlabKind kind = legacySlabKind(ResourceLocation.tryParse(compound.getString("id")));
                if (kind != null) {
                    compound.putString("id", (kind == LegacySlabKind.GRASS
                            ? ModBlocks.GRASS_SLAB_ITEM.get() : ModBlocks.DIRT_SLAB_ITEM.get())
                            .builtInRegistryHolder().key().location().toString());
                    int count = stackCount(compound);
                    if (state != null && kind == LegacySlabKind.GRASS) {
                        state.recordGrassItems(count);
                    } else if (state != null) {
                        state.recordDirtItems(count);
                    }
                    changed = true;
                }
            }
            for (String key : new ArrayList<>(compound.getAllKeys())) {
                Tag child = compound.get(key);
                if (child != null) {
                    changed |= migrateStacksInNbt(child, state);
                }
            }
        } else if (tag instanceof ListTag list) {
            for (Tag child : list) {
                changed |= migrateStacksInNbt(child, state);
            }
        }
        return changed;
    }

    private static boolean migrateChunkBlocks(
            LevelChunk chunk, CompoundTag chunkData, ModWorldState state) {
        if (chunkData.getInt(CHUNK_MARKER) >= ModWorldState.MIGRATION_VERSION
                || (!BuildingBricksCompat.isInstalled() && !containsLegacyId(chunkData))) {
            return false;
        }
        long grass = 0;
        long dirt = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); ++y) {
            for (int z = 0; z < 16; ++z) {
                for (int x = 0; x < 16; ++x) {
                    cursor.set(chunk.getPos().getMinBlockX() + x, y,
                            chunk.getPos().getMinBlockZ() + z);
                    BlockState oldState = chunk.getBlockState(cursor);
                    LegacySlabKind kind = legacySlabKind(oldState.getBlock());
                    if (kind == null) {
                        continue;
                    }
                    ChunkMigrationAccess.setBlockState(chunk, cursor,
                            replacement(oldState, kind));
                    if (kind == LegacySlabKind.GRASS) {
                        ++grass;
                    } else {
                        ++dirt;
                    }
                }
            }
        }
        if (grass + dirt == 0) {
            return false;
        }
        MIGRATED_CHUNKS.add(chunk);
        state.recordChunk();
        state.recordGrassBlocks(grass);
        state.recordDirtBlocks(dirt);
        return true;
    }

    private static boolean containsLegacyId(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                Tag child = compound.get(key);
                if (child != null && containsLegacyId(child)) {
                    return true;
                }
            }
            return false;
        }
        if (tag instanceof ListTag list) {
            for (Tag child : list) {
                if (containsLegacyId(child)) {
                    return true;
                }
            }
            return false;
        }
        if (tag.getId() == Tag.TAG_STRING) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getAsString());
            return id != null && legacySlabKind(id) != null;
        }
        return false;
    }

    private static BlockState replacement(BlockState source, LegacySlabKind kind) {
        BlockState replacement = (kind == LegacySlabKind.GRASS
                ? ModBlocks.GRASS_SLAB.get() : ModBlocks.DIRT_SLAB.get()).defaultBlockState();
        if (source.hasProperty(SlabBlock.TYPE)) {
            replacement = replacement.setValue(SlabBlock.TYPE, source.getValue(SlabBlock.TYPE));
        }
        replacement = replacement.setValue(SlabBlock.WATERLOGGED, false);
        return replacement.setValue(SnowyDirtBlock.SNOWY, false);
    }

    private static boolean migrateChunkInventories(LevelChunk chunk, ModWorldState state) {
        boolean changed = false;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            CompoundTag serialized = blockEntity.saveWithFullMetadata(
                    chunk.getLevel().registryAccess());
            if (migrateStacksInNbt(serialized, state)) {
                blockEntity.loadWithComponents(serialized, chunk.getLevel().registryAccess());
                blockEntity.setChanged();
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
        if (changed) {
            inventory.setChanged();
        }
    }

    private static ItemStack migrateStack(ItemStack stack, ModWorldState state) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        LegacySlabKind kind = legacySlabKind(Block.byItem(stack.getItem()));
        if (kind == null) {
            return ItemStack.EMPTY;
        }
        ItemStack migrated = stack.transmuteCopy(kind == LegacySlabKind.GRASS
                ? ModBlocks.GRASS_SLAB_ITEM.get() : ModBlocks.DIRT_SLAB_ITEM.get(),
                stack.getCount());
        if (state != null && kind == LegacySlabKind.GRASS) {
            state.recordGrassItems(stack.getCount());
        } else if (state != null) {
            state.recordDirtItems(stack.getCount());
        }
        return migrated;
    }

    private static boolean replacementEnabled() {
        return BuildingBricksCompat.hasLegacyAliases()
                || BuildingBricksCompat.isInstalled()
                        && SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs();
    }

    private static boolean hasStackCount(CompoundTag stack) {
        return stack.contains("Count", Tag.TAG_ANY_NUMERIC)
                || stack.contains("count", Tag.TAG_ANY_NUMERIC);
    }

    private static int stackCount(CompoundTag stack) {
        return stack.contains("count", Tag.TAG_ANY_NUMERIC)
                ? stack.getInt("count") : stack.getByte("Count") & 255;
    }

    enum LegacySlabKind {
        GRASS,
        DIRT
    }

    private LegacyMigrationHandler() {
    }
}
