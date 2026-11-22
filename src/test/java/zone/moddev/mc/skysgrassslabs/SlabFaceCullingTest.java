package zone.moddev.mc.skysgrassslabs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SlabFaceCullingTest {
    @Test
    void neighboursCannotCullFacesInsideTheSlabSpace() throws Exception {
        Path directory = Paths.get("src/main/resources/assets/skysgrassslabs/models/block");
        for (String name : Arrays.asList("dirt_slab", "dirt_slab_top",
                "dirt_slab_snow", "dirt_slab_top_snow", "grass_slab", "grass_slab_top",
                "grass_slab_snow", "grass_slab_top_snow", "path_slab", "path_slab_top", "turf")) {
            try (Reader input = Files.newBufferedReader(directory.resolve(name + ".json"),
                    StandardCharsets.UTF_8)) {
                JsonObject model = new JsonParser().parse(input).getAsJsonObject();
                if (!model.has("elements")) {
                    assertTrue(model.get("parent").getAsString().equals("minecraft:block/slab")
                            || model.get("parent").getAsString().equals("minecraft:block/slab_top"));
                    continue;
                }
                for (JsonElement value : model.getAsJsonArray("elements")) {
                    JsonObject element = value.getAsJsonObject();
                    JsonObject faces = element.getAsJsonObject("faces");
                    // A neighbour touches the block boundary, never the surface
                    // half way through our block. Such faces must stay unculled.
                    if (element.getAsJsonArray("to").get(1).getAsDouble() < 16 && faces.has("up")) {
                        assertFalse(faces.getAsJsonObject("up").has("cullface"), name + " top");
                    }
                    if (element.getAsJsonArray("from").get(1).getAsDouble() > 0 && faces.has("down")) {
                        assertFalse(faces.getAsJsonObject("down").has("cullface"), name + " underside");
                    }
                }
            }
        }
    }
}
