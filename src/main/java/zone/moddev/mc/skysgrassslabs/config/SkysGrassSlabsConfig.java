package zone.moddev.mc.skysgrassslabs.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;

public final class SkysGrassSlabsConfig {
    public static final String COMPAT_CATEGORY = "compat";
    public static final String FORCE_REPLACE_BUILDINGBRICKS_SLABS = "forceReplaceBuildingBricksSlabs";
    public static final String WORLDGEN_CATEGORY = "worldgen";
    public static final String GENERATE_GRASS_SLABS = "generateGrassSlabs";

    private static Configuration configuration;
    private static boolean forceReplaceBuildingBricksSlabs;
    private static boolean generateGrassSlabs = true;
    private static boolean compatibilitySuppressed;

    public static void load(File file) {
        compatibilitySuppressed = false;

        configuration = new Configuration(file);
        Configuration config = configuration;

        config.load();
        forceReplaceBuildingBricksSlabs = config.getBoolean(FORCE_REPLACE_BUILDINGBRICKS_SLABS, COMPAT_CATEGORY, false,
                "Replace supported BuildingBricks grass and dirt slabs and items with Sky's "
                        + "versions while BuildingBricks is installed. Existing conversions cannot be undone.");
        generateGrassSlabs = config.getBoolean(GENERATE_GRASS_SLABS, WORLDGEN_CATEGORY, true,
                "Place grass slabs on suitable one-block slopes in newly generated Overworld chunks.");

        config.setCategoryRequiresMcRestart(WORLDGEN_CATEGORY, true);
        config.setCategoryRequiresMcRestart(COMPAT_CATEGORY, true);
        // Keep the comments created above: calling the generic getter again clears Forge's help text.
        config.getCategory(WORLDGEN_CATEGORY).get(GENERATE_GRASS_SLABS)
                .setLanguageKey("config.skysgrassslabs.generateGrassSlabs");
        config.getCategory(COMPAT_CATEGORY).get(FORCE_REPLACE_BUILDINGBRICKS_SLABS)
                .setLanguageKey("config.skysgrassslabs.forceReplaceBuildingBricksSlabs");

        if (config.hasChanged()) {
            config.save();
        }

        GrassSlabsApi.configurationLoaded();
    }

    /** The config screen edits pending settings, not the active generation policy. */
    public static Configuration configuration() {
        if (configuration == null) {
            throw new IllegalStateException("Grass Slabs configuration has not loaded");
        }

        return configuration;
    }

    public static void saveChanges() {
        if (configuration().hasChanged()) {
            configuration.save();
        }
    }

    public static boolean generateGrassSlabs() {
        return generateGrassSlabs;
    }

    public static boolean forceReplaceBuildingBricksSlabs() {
        return forceReplaceBuildingBricksSlabs;
    }

    public static boolean isSmoothingActive() {
        return generateGrassSlabs && !compatibilitySuppressed && GrassSlabsApi.ownerAllowsGeneration();
    }

    public static void suppressSmoothingForThisRun() {
        compatibilitySuppressed = true;
    }

    private SkysGrassSlabsConfig() {
    }
}
