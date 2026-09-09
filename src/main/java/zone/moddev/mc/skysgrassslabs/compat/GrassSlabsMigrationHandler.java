package zone.moddev.mc.skysgrassslabs.compat;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Converts supported content from the Grass Slabs source mod. */
public final class GrassSlabsMigrationHandler {
    private static final org.slf4j.Logger SERIALIZATION_LOGGER = LogUtils.getLogger();
    static final String CHUNK_MARKER = "skysgrassslabs_grassslabs_migration_version";

    private static final LongAdder RUN_BLOCKS = new LongAdder();
    private static final LongAdder RUN_ITEMS = new LongAdder();
    private static final LongAdder RUN_RETAINED_CARPETS = new LongAdder();
    private static final Queue<LevelChunk> PENDING_CHUNKS = new ConcurrentLinkedQueue<>();

    public static void register() {
        ChunkEvent.Load.BUS.addListener(GrassSlabsMigrationHandler::loadChunk);
        TickEvent.ServerTickEvent.Post.BUS.addListener(
                GrassSlabsMigrationHandler::serverTick);
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(
                GrassSlabsMigrationHandler::playerLogin);
        EntityJoinLevelEvent.BUS.addListener(GrassSlabsMigrationHandler::entityJoin);
        BlockEvent.EntityPlaceEvent.BUS.addListener(GrassSlabsMigrationHandler::blockPlaced);
        ServerAboutToStartEvent.BUS.addListener(
                GrassSlabsMigrationHandler::serverAboutToStart);
        ServerStoppingEvent.BUS.addListener(GrassSlabsMigrationHandler::serverStopping);
    }

    public static void loadChunk(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof Level level)
                || !(event.getChunk() instanceof LevelChunk chunk)
                || level.isClientSide() || !replacementEnabled()) {
            return;
        }
        PENDING_CHUNKS.add(chunk);
    }

    private static void serverTick(TickEvent.ServerTickEvent.Post event) {
        LevelChunk chunk;
        while ((chunk = PENDING_CHUNKS.poll()) != null) {
            migrateChunk(chunk);
        }
    }

    private static void migrateChunk(LevelChunk chunk) {
        Level level = chunk.getLevel();
        if (level.isClientSide() || !replacementEnabled()) {
            return;
        }
        ModWorldState state = ModWorldState.get(level);
        boolean changed = migrateChunkInventories(chunk, java.util.List.of(), state);
        ScanResult result = migrateChunkBlocks(chunk, state);
        if (result.encountered() || changed) {
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
                || !GrassSlabsCompat.isInstalled()
                || !SkysGrassSlabsConfig.forceReplaceGrassSlabsModContent()) {
            return;
        }
        LegacyKind kind = legacyKind(event.getPlacedBlock().getBlock());
        if (kind == null) {
            return;
        }
        BlockState replacement = replacement(event.getPlacedBlock(), kind,
                level.getBlockState(event.getPos().below()));
        if (replacement != null) {
            level.setBlock(event.getPos(), replacement, Block.UPDATE_ALL);
            ModWorldState.get(level).recordGrassSlabsBlock(kind.key(), 1L);
            RUN_BLOCKS.increment();
        }
    }

    private static void serverAboutToStart(ServerAboutToStartEvent event) {
        RUN_BLOCKS.reset();
        RUN_ITEMS.reset();
        RUN_RETAINED_CARPETS.reset();
        if (GrassSlabsCompat.isInstalled()
                && SkysGrassSlabsConfig.forceReplaceGrassSlabsModContent()) {
            SkysGrassSlabs.LOGGER.warn("Forced Grass Slabs content replacement does not "
                    + "convert these unsupported 1.18 IDs: {}",
                    GrassSlabsCompat.UNSUPPORTED_1_18_IDS);
        }
    }

    private static void serverStopping(ServerStoppingEvent event) {
        PENDING_CHUNKS.clear();
        long blocks = RUN_BLOCKS.sum();
        long items = RUN_ITEMS.sum();
        long retained = RUN_RETAINED_CARPETS.sum();
        if (blocks + items + retained > 0L) {
            if (retained == 1L) {
                SkysGrassSlabs.LOGGER.info("Grass Slabs compatibility converted {} blocks and "
                        + "{} items; 1 grass carpet block was retained because it was not on "
                        + "vanilla dirt", blocks, items);
            } else {
                SkysGrassSlabs.LOGGER.info("Grass Slabs compatibility converted {} blocks and "
                        + "{} items; {} grass carpet blocks were retained because they were not "
                        + "on vanilla dirt", blocks, items, retained);
            }
        }
    }

    static boolean replacementEnabled(boolean installed, boolean aliases, boolean forced) {
        return aliases || installed && forced;
    }

    static LegacyKind legacyKind(Identifier id) {
        if (GrassSlabsCompat.GRASS_SLAB_ID.equals(id)) {
            return LegacyKind.GRASS_SLAB;
        }
        if (GrassSlabsCompat.DIRT_SLAB_ID.equals(id)) {
            return LegacyKind.DIRT_SLAB;
        }
        if (GrassSlabsCompat.DIRT_PATH_SLAB_ID.equals(id)) {
            return LegacyKind.PATH_SLAB;
        }
        return GrassSlabsCompat.GRASS_CARPET_ID.equals(id) ? LegacyKind.GRASS_CARPET : null;
    }

    static BlockState replacement(BlockState source, LegacyKind kind, BlockState support) {
        if (kind == LegacyKind.GRASS_CARPET) {
            return support.is(Blocks.DIRT) ? ModBlocks.TURF.get().defaultBlockState() : null;
        }

        SlabType type = source.hasProperty(SlabBlock.TYPE)
                ? source.getValue(SlabBlock.TYPE) : SlabType.BOTTOM;
        if (type == SlabType.DOUBLE) {
            return kind.fullBlock().defaultBlockState();
        }

        BlockState replacement = kind.slabBlock().defaultBlockState()
                .setValue(SlabBlock.TYPE, type);
        boolean waterlogged = source.hasProperty(SlabBlock.WATERLOGGED)
                && source.getValue(SlabBlock.WATERLOGGED);
        replacement = replacement.setValue(SlabBlock.WATERLOGGED, waterlogged);
        if (replacement.hasProperty(SnowyDirtBlock.SNOWY)) {
            replacement = replacement.setValue(SnowyDirtBlock.SNOWY, false);
        }
        return replacement;
    }

    static boolean migrateStacksInNbt(Tag tag, ModWorldState state) {
        boolean changed = false;
        if (tag instanceof CompoundTag compound) {
            if (compound.contains("id") && hasStackCount(compound)) {
                LegacyKind kind = legacyKind(Identifier.tryParse(
                        compound.getStringOr("id", "")));
                if (kind != null) {
                    compound.putString("id", ForgeRegistries.ITEMS.getKey(kind.item()).toString());
                    int count = stackCount(compound);
                    recordItems(state, kind, count);
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

    private static ScanResult migrateChunkBlocks(LevelChunk chunk, ModWorldState state) {

        long converted = 0L;
        long retained = 0L;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = chunk.getMinY(); y <= chunk.getMaxY(); ++y) {
            for (int z = 0; z < 16; ++z) {
                for (int x = 0; x < 16; ++x) {
                    cursor.set(chunk.getPos().getMinBlockX() + x, y,
                            chunk.getPos().getMinBlockZ() + z);
                    BlockState oldState = chunk.getBlockState(cursor);
                    LegacyKind kind = legacyKind(oldState.getBlock());
                    if (kind == null) {
                        continue;
                    }
                    BlockState replacement = replacement(oldState, kind,
                            chunk.getBlockState(cursor.below()));
                    if (replacement == null) {
                        ++retained;
                        continue;
                    }
                    ChunkMigrationAccess.setBlockState(chunk, cursor, replacement);
                    state.recordGrassSlabsBlock(kind.key(), 1L);
                    ++converted;
                }
            }
        }

        if (converted == 0L) {
            return ScanResult.EMPTY;
        }
        state.recordGrassSlabsChunk();
        state.recordRetainedGrassCarpets(retained);
        RUN_BLOCKS.add(converted);
        RUN_RETAINED_CARPETS.add(retained);
        return new ScanResult(converted, retained);
    }

    private static boolean migrateChunkInventories(
            LevelChunk chunk, java.util.List<CompoundTag> pending, ModWorldState state) {
        boolean changed = false;
        Set<BlockPos> visited = new HashSet<>();
        for (CompoundTag serialized : pending) {
            if (!serialized.contains("x") || !serialized.contains("y")
                    || !serialized.contains("z")) {
                continue;
            }
            BlockPos pos = new BlockPos(serialized.getIntOr("x", 0),
                    serialized.getIntOr("y", 0), serialized.getIntOr("z", 0));
            if (migrateStacksInNbt(serialized, state)) {
                visited.add(pos);
                changed = true;
            }
        }
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (visited.contains(blockEntity.getBlockPos())) {
                continue;
            }
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
        LegacyKind kind = legacyKind(Block.byItem(stack.getItem()));
        if (kind == null) {
            return ItemStack.EMPTY;
        }
        ItemStack migrated = stack.transmuteCopy(kind.item(), stack.getCount());
        recordItems(state, kind, stack.getCount());
        return migrated;
    }

    private static void recordItems(ModWorldState state, LegacyKind kind, long count) {
        if (count <= 0L) {
            return;
        }
        if (state != null) {
            state.recordGrassSlabsItem(kind.key(), count);
        }
        RUN_ITEMS.add(count);
    }

    private static boolean hasStackCount(CompoundTag stack) {
        return stack.contains("Count") || stack.contains("count");
    }

    private static int stackCount(CompoundTag stack) {
        return stack.contains("count")
                ? stack.getIntOr("count", 0) : stack.getByteOr("Count", (byte) 0) & 255;
    }

    private static LegacyKind legacyKind(Block block) {
        if (block == GrassSlabsCompat.grassSlab()) {
            return LegacyKind.GRASS_SLAB;
        }
        if (block == GrassSlabsCompat.dirtSlab()) {
            return LegacyKind.DIRT_SLAB;
        }
        if (block == GrassSlabsCompat.pathSlab()) {
            return LegacyKind.PATH_SLAB;
        }
        return block == GrassSlabsCompat.grassCarpet() ? LegacyKind.GRASS_CARPET : null;
    }

    private static boolean replacementEnabled() {
        return replacementEnabled(GrassSlabsCompat.isInstalled(),
                GrassSlabsCompat.hasLegacyAliases(),
                SkysGrassSlabsConfig.forceReplaceGrassSlabsModContent());
    }

    enum LegacyKind {
        GRASS_SLAB("grass_slab"),
        DIRT_SLAB("dirt_slab"),
        PATH_SLAB("path_slab"),
        GRASS_CARPET("turf");

        private final String key;

        LegacyKind(String key) {
            this.key = key;
        }

        String key() {
            return key;
        }

        Block slabBlock() {
            return switch (this) {
                case GRASS_SLAB -> ModBlocks.GRASS_SLAB.get();
                case DIRT_SLAB -> ModBlocks.DIRT_SLAB.get();
                case PATH_SLAB -> ModBlocks.PATH_SLAB.get();
                case GRASS_CARPET -> throw new IllegalStateException("Turf is not a slab");
            };
        }

        Block fullBlock() {
            return switch (this) {
                case GRASS_SLAB -> Blocks.GRASS_BLOCK;
                case DIRT_SLAB -> Blocks.DIRT;
                case PATH_SLAB -> Blocks.DIRT_PATH;
                case GRASS_CARPET -> throw new IllegalStateException("Turf has no full block");
            };
        }

        Item item() {
            return switch (this) {
                case GRASS_SLAB -> ModBlocks.GRASS_SLAB_ITEM.get();
                case DIRT_SLAB -> ModBlocks.DIRT_SLAB_ITEM.get();
                case PATH_SLAB -> ModBlocks.PATH_SLAB_ITEM.get();
                case GRASS_CARPET -> ModBlocks.TURF_ITEM.get();
            };
        }
    }

    private record ScanResult(long converted, long retained) {
        private static final ScanResult EMPTY = new ScanResult(0L, 0L);

        boolean encountered() {
            return converted + retained > 0L;
        }
    }

    private GrassSlabsMigrationHandler() {
    }
}
