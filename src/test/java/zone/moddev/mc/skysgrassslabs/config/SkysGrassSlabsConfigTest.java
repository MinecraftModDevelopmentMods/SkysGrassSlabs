package zone.moddev.mc.skysgrassslabs.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkysGrassSlabsConfigTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void provideForgeMinecraftHome() throws Exception {
        java.lang.reflect.Field field = FMLInjectionData.class.getDeclaredField("minecraftHome");
        field.setAccessible(true);
        field.set(null, temporaryDirectory.toFile());
    }

    @Test
    void defaultsToSmoothingEnabledAndReplacementDisabled() {
        File file = temporaryDirectory.resolve("default.cfg").toFile();

        SkysGrassSlabsConfig.load(file);

        assertTrue(SkysGrassSlabsConfig.generateGrassSlabs());
        assertTrue(SkysGrassSlabsConfig.isSmoothingActive());
        assertFalse(SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs());
        Configuration written = new Configuration(file);
        written.load();
        assertFalse(written.get(SkysGrassSlabsConfig.COMPAT_CATEGORY,
                SkysGrassSlabsConfig.FORCE_REPLACE_BUILDINGBRICKS_SLABS, false).getBoolean());
    }

    @Test
    void worldgenAndReplacementSettingsAreIndependent() {
        boolean[] values = new boolean[] {false, true};
        int index = 0;
        for (boolean worldgen : values) {
            for (boolean replacement : values) {
                File file = temporaryDirectory.resolve("matrix-" + index++ + ".cfg").toFile();
                write(file, worldgen, replacement);

                SkysGrassSlabsConfig.load(file);

                assertEquals(worldgen, SkysGrassSlabsConfig.generateGrassSlabs());
                assertEquals(worldgen, SkysGrassSlabsConfig.isSmoothingActive());
                assertEquals(replacement,
                        SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs());
            }
        }
    }

    @Test
    void loadingConfigurationClearsOnlyTheRunSpecificSmoothingSuppression() {
        File file = temporaryDirectory.resolve("reload.cfg").toFile();
        write(file, true, true);
        SkysGrassSlabsConfig.load(file);
        SkysGrassSlabsConfig.suppressSmoothingForThisRun();
        assertFalse(SkysGrassSlabsConfig.isSmoothingActive());

        SkysGrassSlabsConfig.load(file);

        assertTrue(SkysGrassSlabsConfig.isSmoothingActive());
        assertTrue(SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs());
    }

    private static void write(File file, boolean worldgen, boolean replacement) {
        Configuration configuration = new Configuration(file);
        configuration.load();
        configuration.get(SkysGrassSlabsConfig.WORLDGEN_CATEGORY,
                SkysGrassSlabsConfig.GENERATE_GRASS_SLABS, true).set(worldgen);
        configuration.get(SkysGrassSlabsConfig.COMPAT_CATEGORY,
                SkysGrassSlabsConfig.FORCE_REPLACE_BUILDINGBRICKS_SLABS, false)
                .set(replacement);
        configuration.save();
    }
}
