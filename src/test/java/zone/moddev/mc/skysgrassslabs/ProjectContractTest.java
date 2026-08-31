package zone.moddev.mc.skysgrassslabs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class ProjectContractTest {
    @Test
    public void metadataUsesStableIdentityWithoutOreSpawnDependency() throws Exception {
        String metadata = Files.readString(
                Path.of("src/main/resources/META-INF/mods.toml"), StandardCharsets.UTF_8);
        String properties = Files.readString(Path.of("gradle.properties"), StandardCharsets.UTF_8);

        assertTrue(properties.contains("mod_id=skysgrassslabs"));
        assertTrue(properties.contains("minecraft_version=1.18.2"));
        assertTrue(properties.contains("forge_version=40.3.0"));
        assertFalse(metadata.contains("modId=\"orespawn\""));
    }

    @Test
    public void handoffDocumentsExist() {
        assertTrue(Files.isRegularFile(Path.of("docs/ARCHITECTURE.md")));
        assertTrue(Files.isRegularFile(Path.of("docs/LEGACY-MIGRATION.md")));
        assertTrue(Files.isRegularFile(Path.of("docs/TESTING.md")));
        assertTrue(Files.isRegularFile(Path.of("docs/REPOSITORY.md")));
    }
}
