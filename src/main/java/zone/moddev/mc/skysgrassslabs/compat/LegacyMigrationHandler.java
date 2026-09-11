package zone.moddev.mc.skysgrassslabs.compat;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
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
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.common.NeoForge;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Converts supported historical slab blocks and item stacks as their owners load. */
public final class LegacyMigrationHandler {
    private static final org.slf4j.Logger SERIALIZATION_LOGGER = LogUtils.getLogger();
    private static final Queue<LevelChunk> PENDING_CHUNKS = new ConcurrentLinkedQueue<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LegacyMigrationHandler::loadChunk);
        NeoForge.EVENT_BUS.addListener(LegacyMigrationHandler::serverTick);
        NeoForge.EVENT_BUS.addListener(LegacyMigrationHandler::serverStopping);
        NeoForge.EVENT_BUS.addListener(LegacyMigrationHandler::playerLogin);
        NeoForge.EVENT_BUS.addListener(LegacyMigrationHandler::entityJoin);
        NeoForge.EVENT_BUS.addListener(LegacyMigrationHandler::blockPlaced);
    }

    public static void loadChunk(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof Level level)
                || !(event.getChunk() instanceof LevelChunk chunk)
                || level.isClientSide() || !replacementEnabled()) {
            return;
        }
        PENDING_CHUNKS.add(chunk);
    }

    private static void serverTick(ServerTickEvent.Post event) {
        LevelChunk chunk;
        while ((chunk = PENDING_CHUNKS.poll()) != null) {
            migrateChunk(chunk);
        }
    }

    private static void serverStopping(ServerStoppingEvent event) {
        PENDING_CHUNKS.clear();
    }

    private static void migrateChunk(LevelChunk chunk) {
        Level level = chunk.getLevel();
        if (level.isClientSide() || !replacementEnabled()) {
            return;
        }
        ModWorldState state = ModWorldState.get(level);
        boolean changed = migrateChunkInventories(chunk, state);
        changed |= migrateChunkBlocks(chunk, state);
        if (changed) {
            chunk.markUnsaved();
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
        try (ProblemReporter.ScopedCollector problems = new ProblemReporter.ScopedCollector(
                entity.problemPath(), SERIALIZATION_LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(
                    problems, entity.registryAccess());
            entity.saveWithoutId(output);
            CompoundTag serialized = output.buildResult();
            if (migrateStacksInNbt(serialized, ModWorldState.get(level))) {
                entity.load(TagValueInput.create(problems, entity.registryAccess(), serialized));
            }
        }
    }

    public static void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()
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

    static LegacySlabKind legacySlabKind(Identifier id) {
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
            if (compound.contains("id") && hasStackCount(compound)) {
                LegacySlabKind kind = legacySlabKind(Identifier.tryParse(
                        compound.getStringOr("id", "")));
                if (kind != null) {
                    Item replacement = kind == LegacySlabKind.GRASS
                            ? ModBlocks.GRASS_SLAB_ITEM.get() : ModBlocks.DIRT_SLAB_ITEM.get();
                    compound.putString("id", BuiltInRegistries.ITEM.getKey(replacement).toString());
                    int count = stackCount(compound);
                    if (state != null && kind == LegacySlabKind.GRASS) {
                        state.recordGrassItems(count);
                    } else if (state != null) {
                        state.recordDirtItems(count);
                    }
                    changed = true;
                }
            }
            for (String key : new ArrayList<>(compound.keySet())) {
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

    private static boolean migrateChunkBlocks(LevelChunk chunk, ModWorldState state) {
        long grass = 0;
        long dirt = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = chunk.getMinY(); y <= chunk.getMaxY(); ++y) {
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
        state.recordChunk();
        state.recordGrassBlocks(grass);
        state.recordDirtBlocks(dirt);
        return true;
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
                try (ProblemReporter.ScopedCollector problems = new ProblemReporter.ScopedCollector(
                        blockEntity.problemPath(), SERIALIZATION_LOGGER)) {
                    blockEntity.loadWithComponents(TagValueInput.create(problems,
                            chunk.getLevel().registryAccess(), serialized));
                }
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
        return stack.contains("Count") || stack.contains("count");
    }

    private static int stackCount(CompoundTag stack) {
        return stack.contains("count")
                ? stack.getIntOr("count", 0) : stack.getByteOr("Count", (byte) 0) & 255;
    }

    enum LegacySlabKind {
        GRASS,
        DIRT
    }

    private LegacyMigrationHandler() {
    }
}
