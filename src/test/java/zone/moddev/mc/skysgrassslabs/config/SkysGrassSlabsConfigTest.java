package zone.moddev.mc.skysgrassslabs.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkysGrassSlabsConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void legacyConfigurationMigratesBothIndependentSettings() throws Exception {
        Files.write(temporaryDirectory.resolve("skysgrassslabs.cfg"), Arrays.asList(
                "# retained legacy file", "B:generateGrassSlabs=false",
                "B:forceReplaceBuildingBricksSlabs=true"), StandardCharsets.UTF_8);

        assertTrue(SkysGrassSlabsConfig.migrateLegacyConfig(temporaryDirectory));

        String toml = new String(Files.readAllBytes(
                temporaryDirectory.resolve(SkysGrassSlabsConfig.FILE_NAME)),
                StandardCharsets.UTF_8);
        assertTrue(toml.contains("[worldgen]"));
        assertTrue(toml.contains("generateGrassSlabs = false"));
        assertTrue(toml.contains("[compat]"));
        assertTrue(toml.contains("forceReplaceBuildingBricksSlabs = true"));
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve("skysgrassslabs.cfg")));
    }

    @Test
    void existingTomlAlwaysWinsAndIsLeftUntouched() throws Exception {
        Path oldFile = temporaryDirectory.resolve("skysgrassslabs.cfg");
        Path newFile = temporaryDirectory.resolve(SkysGrassSlabsConfig.FILE_NAME);
        Files.write(oldFile, Arrays.asList("B:generateGrassSlabs=false"),
                StandardCharsets.UTF_8);
        byte[] original = "[worldgen]\ngenerateGrassSlabs = true\n".getBytes(StandardCharsets.UTF_8);
        Files.write(newFile, original);

        assertFalse(SkysGrassSlabsConfig.migrateLegacyConfig(temporaryDirectory));
        assertArrayEquals(original, Files.readAllBytes(newFile));
    }

    @Test
    void malformedLegacyValuesFallBackSafely() throws Exception {
        Files.write(temporaryDirectory.resolve("skysgrassslabs.cfg"), Arrays.asList(
                "B:generateGrassSlabs=perhaps",
                "B:forceReplaceBuildingBricksSlabs=not-a-boolean"),
                StandardCharsets.UTF_8);

        assertTrue(SkysGrassSlabsConfig.migrateLegacyConfig(temporaryDirectory));
        String toml = new String(Files.readAllBytes(
                temporaryDirectory.resolve(SkysGrassSlabsConfig.FILE_NAME)),
                StandardCharsets.UTF_8);
        assertTrue(toml.contains("generateGrassSlabs = true"));
        assertTrue(toml.contains("forceReplaceBuildingBricksSlabs = false"));
    }
}
