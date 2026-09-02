package zone.moddev.mc.skysgrassslabs.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class SkysGrassSlabsConfig {
    public static final String COMPAT_CATEGORY = "compat";
    public static final String FORCE_REPLACE_BUILDINGBRICKS_SLABS =
            "forceReplaceBuildingBricksSlabs";
    public static final String WORLDGEN_CATEGORY = "worldgen";
    public static final String GENERATE_GRASS_SLABS = "generateGrassSlabs";

    private static boolean forceReplaceBuildingBricksSlabs;
    private static boolean generateGrassSlabs = true;
    private static boolean compatibilitySuppressed;

    public static void load(File file) {
        compatibilitySuppressed = false;
        Configuration config = new Configuration(file);
        config.load();
        forceReplaceBuildingBricksSlabs = config.getBoolean(
                FORCE_REPLACE_BUILDINGBRICKS_SLABS, COMPAT_CATEGORY, false,
                "Replace supported BuildingBricks grass and dirt slabs and items with Sky's " +
                "versions while BuildingBricks is installed. Existing conversions cannot be undone.");
        generateGrassSlabs = config.getBoolean(GENERATE_GRASS_SLABS, WORLDGEN_CATEGORY, true,
                "Place grass slabs on suitable one-block slopes in newly generated Overworld chunks.");
        if (config.hasChanged()) {
            config.save();
        }
    }

    public static boolean generateGrassSlabs() {
        return generateGrassSlabs;
    }

    public static boolean forceReplaceBuildingBricksSlabs() {
        return forceReplaceBuildingBricksSlabs;
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
