package zone.moddev.mc.skysgrassslabs;

import static org.junit.Assert.assertEquals;
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
    public void recipesUseStableIdsAndForgeSeedTag() throws Exception {
        String slab = Files.readString(Path.of(
                "src/main/resources/data/skysgrassslabs/recipes/grass_slab_from_seeds.json"));
        String block = Files.readString(Path.of(
                "src/main/resources/data/skysgrassslabs/recipes/grass_block_from_seeds.json"));
        assertTrue(slab.contains("forge:seeds"));
        assertTrue(block.contains("forge:seeds"));
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
        String client = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/client/ClientEvents.java"));
        assertTrue(client.contains("RenderType.cutoutMipped()"));
        assertTrue(client.contains("ModBlocks.GRASS_SLAB.get()"));
    }

    @Test
    public void turfRecipeUsesStableCustomSerializer() throws Exception {
        String recipe = Files.readString(Path.of(
                "src/main/resources/data/skysgrassslabs/recipes/turf.json"));
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
