package zone.moddev.mc.skysgrassslabs.upgradeprobe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.compat.GrassSlabsCompat;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Build-only verification for committed forward-upgrade fixtures. */
@Mod(UpgradeProbeMod.MOD_ID)
public final class UpgradeProbeMod {
    public static final String MOD_ID = "skysgrassslabsupgradeprobe";
    private static final BlockPos ORIGIN = new BlockPos(8, 65, 8);
    private static MinecraftServer pendingServer;
    private static int requiredWarmupTicks;
    private static int warmupTicks;
    private static String observedStates = "";

    public UpgradeProbeMod() {
        ServerStartedEvent.BUS.addListener(UpgradeProbeMod::serverStarted);
        TickEvent.ServerTickEvent.Post.BUS.addListener(UpgradeProbeMod::serverTick);
    }

    private static void serverStarted(ServerStartedEvent event) {
        pendingServer = event.getServer();
        pendingServer.overworld().getGameRules().set(
                GameRules.RANDOM_TICK_SPEED, 0, pendingServer);
        Path root = pendingServer.getWorldPath(LevelResource.ROOT);
        requiredWarmupTicks = Files.exists(
                root.resolve("grassslabs-migration-fixture.properties")) ? 100 : 20;
        warmupTicks = 0;
        setFixtureChunksForced(pendingServer.overworld(), true);
    }

    private static void serverTick(TickEvent.ServerTickEvent.Post event) {
        MinecraftServer server = pendingServer;
        if (server == null || ++warmupTicks < requiredWarmupTicks) {
            return;
        }
        pendingServer = null;
        try {
            verify(server);
            writeResult(server, "PASS", "");
        } catch (Exception failure) {
            try {
                writeResult(server, "FAIL", failure.toString());
            } catch (IOException suppressed) {
                failure.addSuppressed(suppressed);
            }
            throw new IllegalStateException("Forward-upgrade verification failed", failure);
        } finally {
            setFixtureChunksForced(server.overworld(), false);
            server.halt(false);
        }
    }

    private static void verify(MinecraftServer server) throws IOException {
        ServerLevel level = server.overworld();
        level.getChunk(0, 0);
        Path root = server.getWorldPath(LevelResource.ROOT);
        Path grassSlabsMarker = root.resolve("grassslabs-migration-fixture.properties");
        if (Files.exists(grassSlabsMarker)) {
            verifyGrassSlabsFixture(level, grassSlabsMarker);
            return;
        }
        Path sylvesterMarker = root.resolve("skysgrassslabs-integration.properties");
        if (Files.exists(sylvesterMarker)) {
            verifySylvesterFixture(level, sylvesterMarker, root);
            return;
        }

        Properties marker = new Properties();
        try (InputStream input = Files.newInputStream(
                root.resolve("skysgrassslabs-forward-fixture.properties"))) {
            marker.load(input);
        }
        observedStates = describeStates(level);

        int expectedBlocks = Integer.parseInt(marker.getProperty("expected_blocks"));
        if (expectedBlocks == 7) {
            verifyClassicFixture(level);
        } else if (usesFirstFlattenedLayout(marker.getProperty("source_minecraft"))) {
            verifyFirstFlattenedFixture(level);
        } else if (expectedBlocks == 11) {
            verifyWaterloggedFixture(level);
        } else {
            throw new IllegalStateException("Unsupported fixture layout: " + expectedBlocks);
        }

        BlockEntity blockEntity = level.getBlockEntity(ORIGIN.south(2));
        require(blockEntity instanceof Container, "Fixture chest was lost");
        Container chest = (Container) blockEntity;
        stack(chest.getItem(0), ModBlocks.DIRT_SLAB_ITEM.get(), 2);
        stack(chest.getItem(1), ModBlocks.GRASS_SLAB_ITEM.get(), 3);
        stack(chest.getItem(2), ModBlocks.PATH_SLAB_ITEM.get(), 4);
        stack(chest.getItem(3), ModBlocks.TURF_ITEM.get(), 5);
        CustomData custom = chest.getItem(1).getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        net.minecraft.nbt.CompoundTag expected = new net.minecraft.nbt.CompoundTag();
        expected.putString("fixture", "retained");
        require(custom.matchedBy(expected), "Fixture stack custom data was lost");
        if (Boolean.parseBoolean(marker.getProperty("component_rich_tool", "false"))) {
            ItemStack tool = chest.getItem(4);
            stack(tool, Items.DIAMOND_SHOVEL, 1);
            require(tool.getOrDefault(DataComponents.DAMAGE, 0) == 7,
                    "Fixture shovel damage was lost");
            require(tool.get(DataComponents.CUSTOM_NAME) != null
                    && "Fixture Shovel".equals(tool.get(DataComponents.CUSTOM_NAME).getString()),
                    "Fixture shovel name was lost");
            ItemEnchantments enchantments = tool.getOrDefault(
                    DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            require(enchantments.size() == 1
                    && enchantments.entrySet().iterator().next().getIntValue() == 2,
                    "Fixture shovel enchantment was lost");
            net.minecraft.nbt.CompoundTag expectedTool = new net.minecraft.nbt.CompoundTag();
            expectedTool.putString("fixture_tool", "retained");
            require(tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .matchedBy(expectedTool), "Fixture shovel custom data was lost");
        }

        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class,
                new AABB(-16, level.getMinY(), -16,
                        32, level.getMaxY() + 1, 32));
        require(entities.size() == 1,
                "Fixture entity count changed: " + entities.size());
        stack(entities.getFirst().getItem(), ModBlocks.GRASS_SLAB_ITEM.get(), 6);
        if (Boolean.parseBoolean(marker.getProperty("component_rich_tool", "false"))) {
            net.minecraft.nbt.CompoundTag expectedEntity = new net.minecraft.nbt.CompoundTag();
            expectedEntity.putString("fixture_entity", "retained");
            require(entities.getFirst().getItem().getOrDefault(
                    DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(expectedEntity),
                    "Fixture entity custom data was lost");
        }

        ModWorldState state = ModWorldState.get(level);
        require(state.schemaVersion() == 1
                && state.migratedChunks() == 2
                && state.migratedGrassBlocks() == 8
                && state.migratedGrassBlocksTop() == 3
                && state.migratedGrassBlocksBottom() == 5
                && state.migratedDirtBlocks() == 18
                && state.migratedDirtBlocksTop() == 7
                && state.migratedDirtBlocksBottom() == 11
                && state.migratedGrassItems() == 13
                && state.migratedDirtItems() == 17
                && Long.valueOf(19L).equals(
                        state.unsupported().get("fixture:unsupported_shape")),
                "Schema-1 migration totals changed");
    }

    private static void verifyGrassSlabsFixture(ServerLevel level, Path markerPath)
            throws IOException {
        Properties marker = new Properties();
        try (InputStream input = Files.newInputStream(markerPath)) {
            marker.load(input);
        }

        int dirtTop = 0;
        int dirtBottom = 0;
        int grassTop = 0;
        int grassBottom = 0;
        int pathTop = 0;
        int pathBottom = 0;
        int turf = 0;
        int retainedCarpet = 0;
        int containerItems = 0;
        StringBuilder states = new StringBuilder();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = level.getMinY(); y <= level.getMaxY(); ++y) {
            for (int z = 0; z < 16; ++z) {
                for (int x = 0; x < 16; ++x) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(ModBlocks.DIRT_SLAB.get())) {
                        if (state.getValue(SlabBlock.TYPE) == SlabType.TOP) {
                            ++dirtTop;
                        } else if (state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) {
                            ++dirtBottom;
                        }
                        states.append(cursor.immutable()).append('=').append(state).append(';');
                    } else if (state.is(ModBlocks.GRASS_SLAB.get())) {
                        if (state.getValue(SlabBlock.TYPE) == SlabType.TOP) {
                            ++grassTop;
                        } else if (state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) {
                            ++grassBottom;
                        }
                        states.append(cursor.immutable()).append('=').append(state).append(';');
                    } else if (state.is(ModBlocks.PATH_SLAB.get())) {
                        if (state.getValue(SlabBlock.TYPE) == SlabType.TOP) {
                            ++pathTop;
                        } else if (state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) {
                            ++pathBottom;
                        }
                        states.append(cursor.immutable()).append('=').append(state).append(';');
                    } else if (state.is(ModBlocks.TURF.get())) {
                        ++turf;
                        states.append(cursor.immutable()).append('=').append(state).append(';');
                    } else if (state.is(GrassSlabsCompat.grassCarpet())) {
                        ++retainedCarpet;
                        states.append(cursor.immutable()).append('=').append(state).append(';');
                    }
                }
            }
        }

        for (BlockEntity blockEntity : level.getChunk(0, 0).getBlockEntities().values()) {
            if (blockEntity instanceof Container container) {
                for (int slot = 0; slot < container.getContainerSize(); ++slot) {
                    ItemStack stack = container.getItem(slot);
                    if (isSkyMigrationItem(stack)) {
                        containerItems += stack.getCount();
                    }
                }
            }
        }
        int entityItems = 0;
        for (Entity loadedEntity : level.getAllEntities()) {
            if (!(loadedEntity instanceof ItemEntity entity)) {
                continue;
            }
            states.append("item@").append(entity.blockPosition()).append('=')
                    .append(ForgeRegistries.ITEMS.getKey(entity.getItem().getItem()))
                    .append('x').append(entity.getItem().getCount()).append(';');
            if (isSkyMigrationItem(entity.getItem())) {
                entityItems += entity.getItem().getCount();
            }
        }
        ModWorldState state = ModWorldState.get(level);
        states.append("counters=blocks[")
                .append(state.grassSlabsMigratedGrassSlabBlocks()).append(',')
                .append(state.grassSlabsMigratedDirtSlabBlocks()).append(',')
                .append(state.grassSlabsMigratedPathSlabBlocks()).append(',')
                .append(state.grassSlabsMigratedTurfBlocks()).append("],items[")
                .append(state.grassSlabsMigratedGrassSlabItems()).append(',')
                .append(state.grassSlabsMigratedDirtSlabItems()).append(',')
                .append(state.grassSlabsMigratedPathSlabItems()).append(',')
                .append(state.grassSlabsMigratedTurfItems()).append("]; ");
        observedStates = states.toString();

        require(dirtTop == 1 && dirtBottom == 1,
                "Grass Slabs dirt orientations were not preserved: "
                        + dirtTop + "/" + dirtBottom);
        require(grassTop == 1 && grassBottom == 1,
                "Grass Slabs grass orientations were not preserved: "
                        + grassTop + "/" + grassBottom);
        require(pathTop == 1 && pathBottom == 1,
                "Grass Slabs path orientations were not preserved: "
                        + pathTop + "/" + pathBottom);
        require(turf == Integer.parseInt(marker.getProperty(
                        "expected_supported_safe_carpet_blocks")),
                "Dirt-supported grass carpet did not migrate to turf: " + turf);
        require(retainedCarpet == Integer.parseInt(marker.getProperty(
                        "expected_supported_retained_carpet_blocks")),
                "Unsafe grass carpet was not retained: " + retainedCarpet);
        require(containerItems == Integer.parseInt(marker.getProperty(
                        "expected_supported_container_items")),
                "Migrated container item total changed: " + containerItems);
        require(entityItems == Integer.parseInt(marker.getProperty(
                        "expected_supported_entity_items")),
                "Migrated entity item total changed: " + entityItems);

        require(state.grassSlabsMigratedChunks() == 1
                && state.grassSlabsMigratedGrassSlabBlocks() == 3
                && state.grassSlabsMigratedDirtSlabBlocks() == 3
                && state.grassSlabsMigratedPathSlabBlocks() == 3
                && state.grassSlabsMigratedTurfBlocks() == 1
                && state.grassSlabsMigratedGrassSlabItems()
                        + state.grassSlabsMigratedDirtSlabItems()
                        + state.grassSlabsMigratedPathSlabItems()
                        + state.grassSlabsMigratedTurfItems() == containerItems + entityItems
                && state.grassSlabsRetainedGrassCarpets() == retainedCarpet,
                "Grass Slabs migration counters changed or were applied twice");
    }

    private static boolean isSkyMigrationItem(ItemStack stack) {
        return stack.is(ModBlocks.DIRT_SLAB_ITEM.get())
                || stack.is(ModBlocks.GRASS_SLAB_ITEM.get())
                || stack.is(ModBlocks.PATH_SLAB_ITEM.get())
                || stack.is(ModBlocks.TURF_ITEM.get());
    }

    private static void verifySylvesterFixture(ServerLevel level, Path markerPath,
            Path worldRoot) throws IOException {
        Properties marker = new Properties();
        try (InputStream input = Files.newInputStream(markerPath)) {
            marker.load(input);
        }
        ModWorldState state = ModWorldState.get(level);
        long unsupportedTotal = state.unsupported().values().stream()
                .mapToLong(Long::longValue).sum();
        require(state.schemaVersion() == 1
                && state.migratedGrassBlocks() == Long.parseLong(
                        marker.getProperty("migrated_grass_blocks"))
                && state.migratedGrassBlocksTop() == Long.parseLong(
                        marker.getProperty("migrated_grass_blocks_top"))
                && state.migratedGrassBlocksBottom() == Long.parseLong(
                        marker.getProperty("migrated_grass_blocks_bottom"))
                && state.migratedDirtBlocks() == Long.parseLong(
                        marker.getProperty("migrated_dirt_blocks"))
                && state.migratedDirtBlocksTop() == Long.parseLong(
                        marker.getProperty("migrated_dirt_blocks_top"))
                && state.migratedDirtBlocksBottom() == Long.parseLong(
                        marker.getProperty("migrated_dirt_blocks_bottom"))
                && state.migratedDirtItems() == Long.parseLong(
                        marker.getProperty("migrated_dirt_items"))
                && state.unsupported().size() == Integer.parseInt(
                        marker.getProperty("unsupported_entries"))
                && unsupportedTotal == Long.parseLong(
                        marker.getProperty("unsupported_total")),
                "Qualified Sylvester migration totals or schema changed");
        require(Files.isRegularFile(worldRoot.resolve(
                        "data/skysgrassslabs_legacy_registry.dat")),
                "Sylvester legacy registry sidecar was lost");
        observedStates = "grass=" + state.migratedGrassBlocks()
                + ";dirt=" + state.migratedDirtBlocks()
                + ";dirt_items=" + state.migratedDirtItems()
                + ";unsupported=" + unsupportedTotal;
    }

    private static void slab(ServerLevel level, BlockPos pos, Block block,
            SlabType type, boolean snowy, boolean waterlogged) {
        BlockState state = level.getBlockState(pos);
        require(state.is(block), "Wrong block at " + pos + ": " + state);
        require(state.getValue(SlabBlock.TYPE) == type,
                "Wrong slab orientation at " + pos + ": " + state);
        require(state.getValue(SlabBlock.WATERLOGGED) == waterlogged,
                "Wrong water state at " + pos + ": " + state);
        if (state.hasProperty(SnowyBlock.SNOWY)) {
            require(state.getValue(SnowyBlock.SNOWY) == snowy,
                    "Wrong snowy state at " + pos + ": " + state);
        }
    }

    private static void stack(ItemStack stack, Item item, int count) {
        require(stack.is(item) && stack.getCount() == count,
                "Wrong fixture stack: " + stack);
    }

    private static void setFixtureChunksForced(ServerLevel level, boolean forced) {
        for (int chunkX = -1; chunkX <= 1; chunkX++) {
            for (int chunkZ = -1; chunkZ <= 1; chunkZ++) {
                level.setChunkForced(chunkX, chunkZ, forced);
            }
        }
    }

    private static void verifyClassicFixture(ServerLevel level) {
        slab(level, ORIGIN, ModBlocks.DIRT_SLAB.get(), SlabType.TOP, false, false);
        slab(level, ORIGIN.east(), ModBlocks.DIRT_SLAB.get(),
                SlabType.BOTTOM, false, false);
        slab(level, ORIGIN.east(2), ModBlocks.GRASS_SLAB.get(),
                SlabType.TOP, false, false);
        slab(level, ORIGIN.east(3), ModBlocks.GRASS_SLAB.get(),
                SlabType.BOTTOM, false, false);
        slab(level, ORIGIN.east(4), ModBlocks.PATH_SLAB.get(),
                SlabType.TOP, false, false);
        slab(level, ORIGIN.east(5), ModBlocks.PATH_SLAB.get(),
                SlabType.BOTTOM, false, false);
        require(level.getBlockState(ORIGIN.east(6)).is(ModBlocks.TURF.get()),
                "Turf was lost");
    }

    private static void verifyWaterloggedFixture(ServerLevel level) {
        slab(level, ORIGIN, ModBlocks.DIRT_SLAB.get(), SlabType.TOP, false, false);
        slab(level, ORIGIN.east(), ModBlocks.DIRT_SLAB.get(),
                SlabType.BOTTOM, false, true);
        slab(level, ORIGIN.east(2), ModBlocks.DIRT_SLAB.get(),
                SlabType.TOP, true, false);
        slab(level, ORIGIN.east(3), ModBlocks.GRASS_SLAB.get(),
                SlabType.TOP, false, false);
        slab(level, ORIGIN.east(4), ModBlocks.GRASS_SLAB.get(),
                SlabType.BOTTOM, false, false);
        slab(level, ORIGIN.east(5), ModBlocks.GRASS_SLAB.get(),
                SlabType.TOP, true, false);
        slab(level, ORIGIN.east(6), ModBlocks.GRASS_SLAB.get(),
                SlabType.BOTTOM, false, true);
        slab(level, ORIGIN.east(7), ModBlocks.PATH_SLAB.get(),
                SlabType.TOP, false, false);
        slab(level, ORIGIN.south(), ModBlocks.PATH_SLAB.get(),
                SlabType.BOTTOM, false, false);
        require(level.getBlockState(ORIGIN.south().east()).is(ModBlocks.TURF.get()),
                "Turf was lost");
        slab(level, ORIGIN.south().east(2), ModBlocks.DIRT_SLAB.get(),
                SlabType.BOTTOM, false, false);
    }

    private static void verifyFirstFlattenedFixture(ServerLevel level) {
        verifyClassicFixture(level);
        slab(level, ORIGIN.south(4), ModBlocks.DIRT_SLAB.get(),
                SlabType.TOP, true, false);
        slab(level, ORIGIN.south(5), ModBlocks.DIRT_SLAB.get(),
                SlabType.BOTTOM, false, true);
        slab(level, ORIGIN.south(6), ModBlocks.GRASS_SLAB.get(),
                SlabType.TOP, true, false);
        slab(level, ORIGIN.south(7), ModBlocks.GRASS_SLAB.get(),
                SlabType.BOTTOM, false, true);
    }

    private static boolean usesFirstFlattenedLayout(String sourceMinecraft) {
        return sourceMinecraft != null && (sourceMinecraft.startsWith("1.13.")
                || sourceMinecraft.startsWith("1.14.")
                || sourceMinecraft.startsWith("1.15.")
                || sourceMinecraft.startsWith("1.16.")
                || sourceMinecraft.startsWith("1.17."));
    }

    private static void writeResult(MinecraftServer server, String status,
            String detail) throws IOException {
        Properties values = new Properties();
        values.setProperty("status", status);
        values.setProperty("detail", detail);
        values.setProperty("schema_version", "1");
        values.setProperty("observed_states", observedStates);
        try (OutputStream output = Files.newOutputStream(server.getWorldPath(
                LevelResource.ROOT).resolve("skysgrassslabs-upgrade-probe.properties"))) {
            values.store(output, "Sky's Grass Slabs forward-upgrade verification");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static String describeStates(ServerLevel level) {
        StringBuilder states = new StringBuilder();
        for (int x = ORIGIN.getX() - 3; x <= ORIGIN.getX() + 9; ++x) {
            for (int z = ORIGIN.getZ() - 3; z <= ORIGIN.getZ() + 9; ++z) {
                BlockPos pos = new BlockPos(x, ORIGIN.getY(), z);
                BlockState state = level.getBlockState(pos);
                if (state.is(ModBlocks.DIRT_SLAB.get())
                        || state.is(ModBlocks.GRASS_SLAB.get())
                        || state.is(ModBlocks.PATH_SLAB.get())
                        || state.is(ModBlocks.TURF.get())) {
                    states.append(pos).append('=').append(state).append(';');
                }
            }
        }
        return states.toString();
    }
}
