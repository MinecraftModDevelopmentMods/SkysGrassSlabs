package zone.moddev.mc.skysgrassslabs.config;

import java.io.File;
import java.nio.file.Path;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import static org.junit.jupiter.api.Assertions.*;

class ConfigScreenContractTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void provideMinecraftHome() throws Exception {
        java.lang.reflect.Field home = FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        home.set(null, temporaryDirectory.toFile());
    }

    @Test
    void configButtonHasAClientFactory() {
        assertEquals("zone.moddev.mc.skysgrassslabs.client.GrassSlabsGuiFactory",
                SkysGrassSlabs.class.getAnnotation(Mod.class).guiFactory());
    }

    @Test
    void loadingKeepsTheHelpTextOnEveryProperty() {
        SkysGrassSlabsConfig.load(temporaryDirectory.resolve("help.cfg").toFile());
        Configuration pending = SkysGrassSlabsConfig.configuration();
        assertFalse(pending.getCategory("worldgen").get("generateGrassSlabs").getComment().trim().isEmpty());
        assertFalse(pending.getCategory("compat").get("forceReplaceBuildingBricksSlabs").getComment().trim().isEmpty());
    }

    @Test
    void allLanguagesExplainEachConfigEntry() throws Exception {
        try (java.util.stream.Stream<Path> languages = java.nio.file.Files.list(
                java.nio.file.Paths.get("src/main/resources/assets/skysgrassslabs/lang"))) {
            for (Path language : languages.collect(java.util.stream.Collectors.toList())) {
                java.util.Properties translations = new java.util.Properties();
                try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(language,
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    translations.load(reader);
                }
                assertFalse(translations.getProperty("config.skysgrassslabs.generateGrassSlabs.tooltip", "").trim().isEmpty(),
                        language + " needs help for generateGrassSlabs");
                assertFalse(translations.getProperty("config.skysgrassslabs.forceReplaceBuildingBricksSlabs.tooltip", "").trim().isEmpty(),
                        language + " needs help for forceReplaceBuildingBricksSlabs");
            }
        }
    }

    @Test
    void savingChoicesDoesNotChangeTheRunningSession() {
        File file = temporaryDirectory.resolve("settings.cfg").toFile();
        SkysGrassSlabsConfig.load(file);
        Configuration pending = SkysGrassSlabsConfig.configuration();

        assertTrue(pending.getCategory("worldgen").requiresMcRestart());
        assertTrue(pending.getCategory("compat").requiresMcRestart());
        pending.get("worldgen", "generateGrassSlabs", true).set(false);
        pending.get("compat", "forceReplaceBuildingBricksSlabs", false).set(true);
        SkysGrassSlabsConfig.saveChanges();

        // Saving the GUI must not re-arbitrate generation or turn on migration mid-world.
        assertTrue(SkysGrassSlabsConfig.generateGrassSlabs());
        assertFalse(SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs());
        Configuration written = new Configuration(file);
        written.load();
        assertFalse(written.get("worldgen", "generateGrassSlabs", true).getBoolean());
        assertTrue(written.get("compat", "forceReplaceBuildingBricksSlabs", false).getBoolean());

        SkysGrassSlabsConfig.load(file);
        assertFalse(SkysGrassSlabsConfig.generateGrassSlabs());
        assertTrue(SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs());
    }

}
