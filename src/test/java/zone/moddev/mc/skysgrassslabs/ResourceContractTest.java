package zone.moddev.mc.skysgrassslabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static void assertParses(Path path) {
        try {
            JsonParser.parseString(Files.readString(path));
        } catch (IOException | RuntimeException exception) {
            throw new AssertionError("Invalid JSON: " + path, exception);
        }
    }
}
