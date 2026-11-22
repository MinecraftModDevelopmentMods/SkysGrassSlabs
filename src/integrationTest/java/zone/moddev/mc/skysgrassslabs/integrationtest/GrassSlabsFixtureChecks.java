package zone.moddev.mc.skysgrassslabs.integrationtest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import zone.moddev.mc.skysgrassslabs.compat.GrassSlabsCompat;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

/** Checks the genuine source-mod save through the ordinary packaged loader. */
final class GrassSlabsFixtureChecks {
    private static final Map<String, String> ITEMS = Map.of(
            "grassslabs:grass_slab", "skysgrassslabs:grass_slab",
            "grassslabs:dirt_slab", "skysgrassslabs:dirt_slab",
            "grassslabs:dirt_path_slab", "skysgrassslabs:path_slab",
            "grassslabs:grass_carpet", "skysgrassslabs:turf");

    static void verify(ServerLevel world, String phase) throws Exception {
        boolean installed = !phase.startsWith("grassslabs-absent-");
        boolean enabled = !phase.startsWith("grassslabs-off-");
        require(GrassSlabsCompat.isInstalled() == installed, "Wrong source-mod installation");
        require(GrassSlabsCompat.hasLegacyAliases() == !installed, "Wrong hidden-holder ownership");
        JsonObject expected = new JsonParser().parse(Files.readString(
                new File("grassslabs-expected.json").toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
        int checked = 0;
        for (var entry : expected.getAsJsonArray("blocks")) {
            JsonObject row = entry.getAsJsonObject();
            BlockPos position = new BlockPos(row.get("x").getAsInt(), row.get("y").getAsInt(), row.get("z").getAsInt());
            CompoundTag state = TagParser.parseTag(row.get("source").getAsString());
            String id = state.getString("Name");
            if (enabled && ITEMS.containsKey(id)) {
                CompoundTag properties = state.getCompound("Properties");
                if (id.equals("grassslabs:grass_carpet")) {
                    if (position.getX() == 10) {
                        state.putString("Name", "skysgrassslabs:turf");
                        state.put("Properties", new CompoundTag());
                    }
                } else if (properties.getString("type").equals("double")) {
                    state = new CompoundTag();
                    state.putString("Name", id.equals("grassslabs:grass_slab") ? "minecraft:grass_block"
                            : id.equals("grassslabs:dirt_slab") ? "minecraft:dirt" : "minecraft:dirt_path");
                } else {
                    state.putString("Name", ITEMS.get(id));
                }
            } else if (!installed && !ITEMS.containsKey(id)) {
                // Forge 47 loads these missing shapes as air after confirmation.
                // Check the same result as the Forge-only control, with no Sky substitution.
                var actual = world.getBlockState(position);
                require(actual.isAir(), "Unsupported shape " + id + " at " + position
                        + " differs from the Forge-only control: " + actual);
                checked++;
                continue;
            }
            var target = NbtUtils.readBlockState(world.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), state);
            require(world.getBlockState(position).equals(target), "Source block changed at " + position
                    + ": expected=" + target + " actual=" + world.getBlockState(position));
            checked++;
        }
        require(checked == 19, "Incomplete source block matrix");
        JsonObject chest = expected.getAsJsonObject("chest");
        var blockEntity = world.getBlockEntity(new BlockPos(chest.get("x").getAsInt(),
                chest.get("y").getAsInt(), chest.get("z").getAsInt()));
        require(blockEntity instanceof Container, "Source chest was lost");
        Container inventory = (Container) blockEntity;
        for (var entry : chest.getAsJsonArray("items")) {
            CompoundTag source = TagParser.parseTag(entry.getAsString());
            int slot = source.getByte("Slot");
            if (enabled && ITEMS.containsKey(source.getString("id"))) {
                source.putString("id", ITEMS.get(source.getString("id")));
            }
            require(ItemStack.matches(inventory.getItem(slot), ItemStack.of(source)),
                    "Source inventory count, identity or custom data changed in slot " + slot);
        }
        var sourceEntities = world.getEntitiesOfClass(ItemEntity.class,
                new net.minecraft.world.phys.AABB(0, 90, 0, 16, 120, 16));
        require(sourceEntities.size() == 1, "The source dropped stack was lost or duplicated");
        require(sourceEntities.get(0).getUUID().equals(UUID.fromString(expected.get("dropped_uuid").getAsString())),
                "The dropped item's identity changed");
        CompoundTag sourceDrop = TagParser.parseTag(expected.get("dropped_item").getAsString());
        if (enabled) sourceDrop.putString("id", "skysgrassslabs:turf");
        require(ItemStack.matches(sourceEntities.get(0).getItem(), ItemStack.of(sourceDrop)),
                "The dropped count or custom data changed");
        File snapshot = new File("grassslabs-world-state-first.dat");
        if (enabled) {
            CompoundTag actual = ModWorldState.get(world).save(new CompoundTag());
            for (String kind : new String[] {"grass_slab", "dirt_slab", "path_slab"}) {
                require(actual.getLong("grassslabs_migrated_" + kind + "_blocks") == 3, "Wrong migrated " + kind + " total");
            }
            require(actual.getLong("grassslabs_migrated_turf_blocks") == 1, "Wrong turf block total");
            require(actual.getLong("grassslabs_retained_grass_carpet_blocks") == 1, "Wrong retained carpet total");
            require(actual.getLong("grassslabs_migrated_grass_slab_items") == 3, "Wrong grass item total");
            require(actual.getLong("grassslabs_migrated_dirt_slab_items") == 4, "Wrong dirt item total");
            require(actual.getLong("grassslabs_migrated_path_slab_items") == 5, "Wrong path item total");
            require(actual.getLong("grassslabs_migrated_turf_items") == 8, "Wrong turf item total");
            if (phase.endsWith("first")) {
                require(!snapshot.exists(), "The first-load evidence already exists");
                NbtIo.writeCompressed(actual, snapshot);
            } else {
                require(actual.equals(NbtIo.readCompressed(snapshot)), "Migration advanced during reload");
            }
        } else {
            require(!Files.exists(world.getServer().getWorldPath(LevelResource.ROOT)
                    .resolve("data/skysgrassslabs_world_state.dat")), "Disabled replacement wrote migration state");
        }
        org.apache.logging.log4j.LogManager.getLogger().info(
                "GRASS_SOURCE_MOD_FIXTURE_PASS phase={} installed={} replacement={} blocks=19 chest_slots=5 dropped=1 nbt=true",
                phase, installed, enabled);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private GrassSlabsFixtureChecks() {
    }
}
