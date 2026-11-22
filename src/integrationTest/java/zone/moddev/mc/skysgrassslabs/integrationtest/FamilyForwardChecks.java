package zone.moddev.mc.skysgrassslabs.integrationtest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Base64;
import java.util.zip.InflaterInputStream;
import net.minecraft.core.Registry;
import net.minecraft.util.datafix.fixes.CavesAndCliffsRenames;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Checks the exact named save produced by the preceding family candidate. */
final class FamilyForwardChecks {
    private static final net.minecraft.world.level.ChunkPos DROP_CHUNK =
            new net.minecraft.world.level.ChunkPos(new BlockPos(508, 205, 512));
    private static UUID expectedDrop;

    static void prepare(ServerLevel world) throws Exception {
        try (InputStreamReader input = new InputStreamReader(new FileInputStream("source-fixture.json"),
                StandardCharsets.UTF_8)) {
            JsonObject manifest = new JsonParser().parse(input).getAsJsonObject();
            expectedDrop = UUID.fromString(manifest.get("drop").getAsString());
        }
        // A full chunk alone does not make its separate entity storage visible.
        // Keep this disposable fixture ticking until the normal loader finishes.
        world.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED,
                DROP_CHUNK, 2, DROP_CHUNK);
        world.getChunk(DROP_CHUNK.x, DROP_CHUNK.z);
    }

    static boolean entityReady(ServerLevel world) {
        return expectedDrop != null && world.getEntity(expectedDrop) != null;
    }

    static void finish(ServerLevel world) {
        world.getChunkSource().removeRegionTicket(net.minecraft.server.level.TicketType.FORCED,
                DROP_CHUNK, 2, DROP_CHUNK);
    }

    static void run(ServerLevel world) throws Exception {
        JsonObject manifest;
        try (InputStreamReader input = new InputStreamReader(new FileInputStream("source-fixture.json"),
                StandardCharsets.UTF_8)) {
            manifest = new JsonParser().parse(input).getAsJsonObject();
        }
        String sourceVersion = manifest.get("source_minecraft").getAsString();
        int expectedVersion;
        if (sourceVersion.equals("1.15.2")) {
            expectedVersion = 2230;
        } else if (sourceVersion.equals("1.16.5")) {
            expectedVersion = 2586;
        } else if (sourceVersion.equals("1.17.1")) {
            expectedVersion = 2730;
        } else if (sourceVersion.equals("1.18.2")) {
            expectedVersion = 2975;
        } else if (sourceVersion.equals("1.19.4")) {
            expectedVersion = 3337;
        } else {
            throw new AssertionError("Unknown source family fixture: " + sourceVersion);
        }
        require(manifest.get("source_data_version").getAsInt() == expectedVersion,
                "The native source version changed");
        verifyBiomes(world, sourceVersion);
        int wet = 0;
        int checked = 0;
        for (JsonElement element : manifest.getAsJsonArray("blocks")) {
            JsonObject saved = element.getAsJsonObject();
            BlockState expected = NbtUtils.readBlockState(world.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), TagParser.parseTag(saved.get("state").getAsString()));
            BlockState actual = world.getBlockState(position(saved));
            require(actual.equals(expected), "Named state changed at " + position(saved) + ": " + actual);
            wet += actual.getFluidState().isEmpty() ? 0 : 1;
            checked++;
        }
        require(checked == 19 && wet == 8, "Every retainable orientation, snow and water state survives");
        for (JsonElement element : manifest.getAsJsonArray("stacks")) {
            JsonObject saved = element.getAsJsonObject();
            ChestBlockEntity chest = (ChestBlockEntity) world.getBlockEntity(position(saved));
            require(chest != null, "The source chest survives");
            ItemStack expected = ItemStack.of(TagParser.parseTag(saved.get("item").getAsString()));
            require(ItemStack.matches(expected, chest.getItem(saved.get("slot").getAsInt())),
                    "Source piece count or custom data changed");
        }
        FakePlayer player = FakePlayerFactory.get(world,
                new GameProfile(UUID.fromString(manifest.get("player").getAsString()), "SkyFixture"));
        require(world.getServer().getPlayerList().load(player) != null, "The saved player file was loaded");
        ItemStack shovel = ItemStack.of(TagParser.parseTag(manifest.get("shovel").getAsString()));
        ItemStack nested = ItemStack.of(TagParser.parseTag(manifest.get("nested").getAsString()));
        require(ItemStack.matches(shovel, player.getInventory().getItem(0)), "The complete player shovel survives");
        ItemStack actualNested = player.getInventory().getItem(1);
        CompoundTag expectedNestedTag = nested.save(new CompoundTag());
        CompoundTag actualNestedTag = actualNested.save(new CompoundTag());
        CompoundTag expectedStorage = expectedNestedTag.getCompound("tag").getCompound("BlockEntityTag");
        CompoundTag actualStorage = actualNestedTag.getCompound("tag").getCompound("BlockEntityTag");
        if (!expectedStorage.contains("id") && actualStorage.getString("id").equals("minecraft:shulker_box")) {
            // Vanilla adds the block-entity identity; every original slot and custom field must still match.
            actualStorage.remove("id");
        }
        require(expectedNestedTag.equals(actualNestedTag),
                "The nested inventory changed: expected=" + expectedNestedTag + " actual=" + actualNestedTag);
        require(ItemStack.matches(shovel, player.getEnderChestInventory().getItem(0)),
                "The complete ender-chest shovel survives");
        world.getChunk(new BlockPos(508, 205, 512));
        net.minecraft.world.entity.Entity dropped = world.getEntity(UUID.fromString(manifest.get("drop").getAsString()));
        require(dropped instanceof ItemEntity && ItemStack.matches(shovel, ((ItemEntity) dropped).getItem()),
                "The dropped item's UUID, tool and custom data survive");
        CompoundTag expectedState = TagParser.parseTag(manifest.get("world_state").getAsString());
        CompoundTag actualState = ModWorldState.get(world).save(new CompoundTag());
        // The 1.18 compatibility module adds its own counters. Existing fields
        // must match exactly; only these new, unused fields may be introduced.
        if (!expectedState.contains("grassslabs_migration_version")) {
            require(actualState.getTagType("grassslabs_migration_version") == 3
                    && actualState.getInt("grassslabs_migration_version") == 1,
                    "The additional migration module must start at version one");
            expectedState.putInt("grassslabs_migration_version", 1);
        }
        for (String key : new String[] {"grassslabs_migrated_chunks",
                "grassslabs_migrated_grass_slab_blocks", "grassslabs_migrated_dirt_slab_blocks",
                "grassslabs_migrated_path_slab_blocks", "grassslabs_migrated_turf_blocks",
                "grassslabs_migrated_grass_slab_items", "grassslabs_migrated_dirt_slab_items",
                "grassslabs_migrated_path_slab_items", "grassslabs_migrated_turf_items",
                "grassslabs_retained_grass_carpet_blocks"}) {
            if (!expectedState.contains(key)) {
                require(actualState.getTagType(key) == 4 && actualState.getLong(key) == 0,
                        "A named world must not advance the new migration counter: " + key);
                expectedState.putLong(key, 0);
            }
        }
        require(actualState.equals(expectedState),
                "Every schema-1 counter and report survives unchanged");
        require(!new File("world/data/skysgrassslabs_legacy_registry.dat").exists(),
                "A named source must not run the numeric bridge");
        System.out.println("GRASS_FAMILY_FORWARD_PASS source=" + sourceVersion + " states=19 wet=8 stacks=19 player=true drop=true schema=1");
    }

    private static BlockPos position(JsonObject row) {
        return new BlockPos(row.get("x").getAsInt(), row.get("y").getAsInt(), row.get("z").getAsInt());
    }

    static void verifyBiomes(ServerLevel world, String sourceVersion) throws Exception {
        File file = new File("family-biomes.json");
        if (!file.isFile()) {
            require(sourceVersion.equals("1.15.2"), "The native biome contract is missing");
            return;
        }
        JsonObject contract;
        try (InputStreamReader input = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            contract = new JsonParser().parse(input).getAsJsonObject();
        }
        require(contract.get("source_minecraft").getAsString().equals(sourceVersion), "Wrong biome fixture");
        boolean expandedHeight = sourceVersion.equals("1.18.2") || sourceVersion.equals("1.19.4");
        String expectedFormat = expandedHeight
                ? "zlib-u8-quart-volume-v2" : "zlib-u8-quart-volume-v1";
        require(contract.get("format").getAsString().equals(expectedFormat), "Unknown biome encoding");
        int minQuartY = expandedHeight ? contract.get("min_quart_y").getAsInt() : 0;
        int quartHeight = expandedHeight ? contract.get("quart_height").getAsInt() : 64;
        require(!expandedHeight || minQuartY == -16 && quartHeight == 96,
                "The expanded Overworld biome height changed");
        JsonObject names = contract.getAsJsonObject("source_names");
        var registry = world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        long cells = 0;
        long renamed = 0;
        for (JsonElement entry : contract.getAsJsonArray("chunks")) {
            JsonObject row = entry.getAsJsonObject();
            int chunkX = row.get("x").getAsInt();
            int chunkZ = row.get("z").getAsInt();
            byte[] source;
            try (InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(
                    Base64.getDecoder().decode(row.get("cells").getAsString())))) {
                source = input.readAllBytes();
            }
            require(source.length == 16 * quartHeight, "The source quart-biome volume changed");
            var chunk = world.getChunk(chunkX, chunkZ);
            for (int index = 0; index < source.length; index++) {
                String oldName = names.get(Integer.toString(Byte.toUnsignedInt(source[index]))).getAsString();
                String expected = CavesAndCliffsRenames.RENAMES.getOrDefault(oldName, oldName);
                if (expected.equals("minecraft:deep_warm_ocean")) {
                    expected = "minecraft:warm_ocean";
                }
                var actual = registry.getKey(chunk.getNoiseBiome((chunkX << 2) + (index & 3),
                        minQuartY + (index >> 4), (chunkZ << 2) + ((index >> 2) & 3)).value());
                require(actual != null && actual.toString().equals(expected), "Native biome conversion at "
                        + chunkX + "," + chunkZ + " quart=" + index + " expected=" + expected + " actual=" + actual);
                cells++;
                if (!oldName.equals(expected)) {
                    renamed++;
                }
            }
        }
        require(cells == contract.get("checked_cells").getAsLong(), "Incomplete biome audit");
        require(renamed == contract.get("native_renamed_cells").getAsLong(), "Native rename coverage changed");
        org.apache.logging.log4j.LogManager.getLogger().info(
                "GRASS_NATIVE_BIOME_UPGRADE_PASS source={} cells={} renamed={}", sourceVersion, cells, renamed);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private FamilyForwardChecks() {
    }
}
