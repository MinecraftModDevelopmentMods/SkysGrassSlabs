package zone.moddev.mc.skysgrassslabs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class SameVersionFixtureContractTest {
    @Test
    void acceptedWorldAndItsProvenanceRemainSealed() throws Exception {
        Path directory = Paths.get("src/test/resources/fixtures");
        Path archive = directory.resolve("skysgrassslabs-1.15.2-forward-world.zip");
        Properties manifest = new Properties();
        try (InputStream input = Files.newInputStream(directory.resolve(
                "skysgrassslabs-1.15.2-forward-world.manifest"))) {
            manifest.load(input);
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(Files.readAllBytes(archive));
        StringBuilder checksum = new StringBuilder();
        for (byte value : digest.digest()) {
            checksum.append(String.format(Locale.ROOT, "%02X", value & 255));
        }
        assertEquals("8D2FEA6A0289B35F0C2728A6E93C09EFDDC3A699EC28DCAC4115EBFE47269D05",
                checksum.toString());
        assertEquals(checksum.toString(), manifest.getProperty("fixture_sha256"));
        assertEquals("1.15.2", manifest.getProperty("source_minecraft"));
        assertEquals("1.0.1.115021", manifest.getProperty("source_mod_version"));
        assertEquals("9AE28332EA21700C5DE8D3597FC40F5B06D85E8A7FB3C0DE650A2F8BC5E0895C",
                manifest.getProperty("source_jar_sha256"));

        // Use the original archive, including the old level backup, rather than
        // rebuilding an approximation with the new mod under test.
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            assertEquals(6, zip.size());
            assertTrue(zip.getEntry("level.dat") != null);
            assertTrue(zip.getEntry("level.dat_old") != null);
            assertTrue(zip.getEntry("region/r.0.0.mca") != null);
            assertTrue(zip.getEntry("data/skysgrassslabs_world_state.dat") != null);
        }
    }
}
