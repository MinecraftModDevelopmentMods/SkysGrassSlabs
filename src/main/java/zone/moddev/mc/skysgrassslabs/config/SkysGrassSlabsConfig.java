package zone.moddev.mc.skysgrassslabs.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class SkysGrassSlabsConfig {
    public static final String WORLDGEN_CATEGORY = "worldgen";
    public static final String GENERATE_GRASS_SLABS = "generateGrassSlabs";

    private static boolean generateGrassSlabs = true;
    private static boolean compatibilitySuppressed;

    public static void load(File file) {
        compatibilitySuppressed = false;
        Configuration config = new Configuration(file);
        config.load();
        generateGrassSlabs = config.getBoolean(GENERATE_GRASS_SLABS, WORLDGEN_CATEGORY, true,
                "Place grass slabs on suitable one-block slopes in newly generated Overworld chunks.");
        if (config.hasChanged()) {
            config.save();
        }
    }

    public static boolean generateGrassSlabs() {
        return generateGrassSlabs;
    }

    public static boolean isSmoothingActive() {
        return generateGrassSlabs && !compatibilitySuppressed;
    }

    public static void suppressSmoothingForThisRun() {
        compatibilitySuppressed = true;
    }

    private SkysGrassSlabsConfig() {
    }
}
