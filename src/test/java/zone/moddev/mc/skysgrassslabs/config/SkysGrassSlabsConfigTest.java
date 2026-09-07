package zone.moddev.mc.skysgrassslabs.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SkysGrassSlabsConfigTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void legacyConfigurationMigratesBothIndependentSettings() throws Exception {
        Path directory = temporary.getRoot().toPath();
        Files.write(directory.resolve("skysgrassslabs.cfg"), java.util.List.of(
                "# retained legacy file", "B:generateGrassSlabs=false",
                "B:forceReplaceBuildingBricksSlabs=true"), StandardCharsets.UTF_8);

        assertTrue(SkysGrassSlabsConfig.migrateLegacyConfig(directory));

        String toml = Files.readString(directory.resolve(SkysGrassSlabsConfig.FILE_NAME),
                StandardCharsets.UTF_8);
        assertTrue(toml.contains("[worldgen]"));
        assertTrue(toml.contains("generateGrassSlabs = false"));
        assertTrue(toml.contains("[compat]"));
        assertTrue(toml.contains("forceReplaceBuildingBricksSlabs = true"));
        assertTrue(Files.isRegularFile(directory.resolve("skysgrassslabs.cfg")));
    }

    @Test
    public void existingTomlWinsAndMalformedValuesUseSafeDefaults() throws Exception {
        Path existingDirectory = temporary.newFolder("existing").toPath();
        Files.writeString(existingDirectory.resolve("skysgrassslabs.cfg"),
                "B:generateGrassSlabs=false\n", StandardCharsets.UTF_8);
        byte[] original = "[worldgen]\ngenerateGrassSlabs = true\n"
                .getBytes(StandardCharsets.UTF_8);
        Path existingToml = existingDirectory.resolve(SkysGrassSlabsConfig.FILE_NAME);
        Files.write(existingToml, original);
        assertFalse(SkysGrassSlabsConfig.migrateLegacyConfig(existingDirectory));
        assertArrayEquals(original, Files.readAllBytes(existingToml));

        Path malformedDirectory = temporary.newFolder("malformed").toPath();
        Files.write(malformedDirectory.resolve("skysgrassslabs.cfg"), java.util.List.of(
                "B:generateGrassSlabs=perhaps",
                "B:forceReplaceBuildingBricksSlabs=not-a-boolean"), StandardCharsets.UTF_8);
        assertTrue(SkysGrassSlabsConfig.migrateLegacyConfig(malformedDirectory));
        String migrated = Files.readString(
                malformedDirectory.resolve(SkysGrassSlabsConfig.FILE_NAME),
                StandardCharsets.UTF_8);
        assertTrue(migrated.contains("generateGrassSlabs = true"));
        assertTrue(migrated.contains("forceReplaceBuildingBricksSlabs = false"));
    }
}
