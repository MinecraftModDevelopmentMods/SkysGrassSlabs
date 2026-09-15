package zone.moddev.mc.skysgrassslabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ResourceContractTest {
    @Test
    public void everyJsonResourceParses() throws Exception {
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources"))) {
            files.filter(path -> path.toString().endsWith(".json"))
                    .forEach(ResourceContractTest::assertParses);
        }
    }

    @Test
    public void recipesUseStableIdsAndCommonSeedTag() throws Exception {
        String slab = Files.readString(Path.of(
                "src/main/resources/data/skysgrassslabs/recipe/grass_slab_from_seeds.json"));
        String block = Files.readString(Path.of(
                "src/main/resources/data/skysgrassslabs/recipe/grass_block_from_seeds.json"));
        assertTrue(slab.contains("c:seeds"));
        assertTrue(block.contains("c:seeds"));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/data/c/tags/item/seeds.json")));
        assertFalse(Files.exists(Path.of(
                "src/main/resources/data/forge/tags/item/seeds.json")));
        assertTrue(slab.contains("skysgrassslabs:dirt_slab"));
        assertTrue(slab.contains("skysgrassslabs:grass_slab"));
    }

    @Test
    public void pathModelsMatchSevenPixelCollisionProfiles() throws Exception {
        JsonElement bottom = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/skysgrassslabs/models/block/path_slab.json")));
        JsonElement top = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/skysgrassslabs/models/block/path_slab_top.json")));
        assertEquals(7, bottom.getAsJsonObject().getAsJsonArray("elements").get(0)
                .getAsJsonObject().getAsJsonArray("to").get(1).getAsInt());
        assertEquals(8, top.getAsJsonObject().getAsJsonArray("elements").get(0)
                .getAsJsonObject().getAsJsonArray("from").get(1).getAsInt());
        assertEquals(15, top.getAsJsonObject().getAsJsonArray("elements").get(0)
                .getAsJsonObject().getAsJsonArray("to").get(1).getAsInt());
    }

    @Test
    public void turfModelIsOnePixelHighAndBiomeTinted() throws Exception {
        JsonElement model = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/skysgrassslabs/models/block/turf.json")));
        var object = model.getAsJsonObject();
        assertEquals("minecraft:block/grass_block_top",
                object.getAsJsonObject("textures").get("turf").getAsString());
        var element = object.getAsJsonArray("elements").get(0).getAsJsonObject();
        assertEquals(1, element.getAsJsonArray("to").get(1).getAsInt());
        var faces = element.getAsJsonObject("faces");
        for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
            assertEquals(face.getKey(), 0,
                    face.getValue().getAsJsonObject().get("tintindex").getAsInt());
        }
    }

    @Test
    public void grassSlabUsesVanillaGrassCutoutLayer() throws Exception {
        for (String modelName : new String[] {"grass_slab", "grass_slab_top",
                "grass_slab_snow", "grass_slab_top_snow"}) {
            String model = Files.readString(Path.of(
                    "src/main/resources/assets/skysgrassslabs/models/block/"
                            + modelName + ".json"));
            assertTrue(modelName, model.contains("\"render_type\": \"minecraft:cutout\""));
            assertFalse(modelName, model.contains("cutout_mipped"));
        }
    }

    @Test
    public void snowyGrassSlabModelsUseUntintedSnowCaps() throws Exception {
        for (String modelName : new String[] {"grass_slab_snow", "grass_slab_top_snow"}) {
            JsonElement model = JsonParser.parseString(Files.readString(Path.of(
                    "src/main/resources/assets/skysgrassslabs/models/block/"
                            + modelName + ".json")));
            var object = model.getAsJsonObject();
            assertEquals("minecraft:block/snow",
                    object.getAsJsonObject("textures").get("top").getAsString());
            assertEquals("minecraft:block/grass_block_snow",
                    object.getAsJsonObject("textures").get("side").getAsString());
            var up = object.getAsJsonArray("elements").get(0).getAsJsonObject()
                    .getAsJsonObject("faces").getAsJsonObject("up");
            assertEquals("#top", up.get("texture").getAsString());
            assertFalse(up.has("tintindex"));
        }
    }

    @Test
    public void grassSlabUsesComponentAwareSilkTouchPredicate() throws Exception {
        String loot = Files.readString(Path.of(
                "src/main/resources/data/skysgrassslabs/loot_table/blocks/grass_slab.json"));
        assertTrue(loot.contains("\"predicates\""));
        assertTrue(loot.contains("\"minecraft:enchantments\""));
        assertTrue(loot.contains("\"enchantments\": \"minecraft:silk_touch\""));
    }

    @Test
    public void turfRecipeUsesStableCustomSerializer() throws Exception {
        String recipe = Files.readString(Path.of(
                "src/main/resources/data/skysgrassslabs/recipe/turf.json"));
        assertTrue(recipe.contains("skysgrassslabs:turf_cutting"));
        String implementation = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/recipe/TurfCuttingRecipe.java"));
        assertTrue(implementation.contains("canPerformAction(ToolActions.SHOVEL_FLATTEN)"));
    }

    private static void assertParses(Path path) {
        try {
            JsonParser.parseString(Files.readString(path));
        } catch (IOException | RuntimeException exception) {
            throw new AssertionError("Invalid JSON: " + path, exception);
        }
    }
}
