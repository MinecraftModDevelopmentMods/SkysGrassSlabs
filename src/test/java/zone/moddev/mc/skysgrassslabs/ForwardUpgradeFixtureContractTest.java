package zone.moddev.mc.skysgrassslabs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class ForwardUpgradeFixtureContractTest {
    private static final File FIXTURE = new File(
            "src/test/resources/fixtures/skysgrassslabs-1.10.2-forward-world.zip");
    private static final File MANIFEST = new File(
            "src/test/resources/fixtures/skysgrassslabs-1.10.2-forward-world.manifest");

    @Test
    void fixtureIsTheLockedGenuineOneTenWorld() throws Exception {
        Properties manifest = new Properties();
        try (InputStream input = Files.newInputStream(MANIFEST.toPath())) {
            manifest.load(input);
        }
        assertEquals("D6923BFFE062C1F0C454190AB11F031825949DF8080D8000133A723DEC2770BF",
                sha256(FIXTURE));
        assertEquals(sha256(FIXTURE), manifest.getProperty("fixture_sha256"));
        assertEquals("1.10.2", manifest.getProperty("source_minecraft"));
        assertEquals("1.0.0.110021", manifest.getProperty("source_mod_version"));
        assertEquals("2030960E217C3F61AE4919C91058696B02F9FAE570BE1CD7B698696EA7BEB861",
                manifest.getProperty("source_jar_sha256"));
        assertTrue(FIXTURE.length() < 100_000L, "Forward fixture is no longer compact");
    }

    @Test
    void fixtureContainsOnlyTheRequiredWorldFiles() throws Exception {
        Set<String> expected = new TreeSet<String>(Arrays.asList(
                "data/capabilities.dat",
                "data/skysgrassslabs_world_state.dat",
                "level.dat",
                "level.dat_old",
                "region/r.0.0.mca",
                "skysgrassslabs-forward-fixture.properties"));
        try (ZipFile zip = new ZipFile(FIXTURE)) {
            Set<String> actual = new TreeSet<String>();
            zip.stream().filter(entry -> !entry.isDirectory())
                    .forEach(entry -> actual.add(entry.getName().replace('\\', '/')));
            assertEquals(expected, actual);
            assertFalse(actual.stream().anyMatch(name -> {
                String lower = name.toLowerCase(Locale.ROOT);
                return lower.contains("playerdata") || lower.contains("agent") ||
                        lower.contains(".codex") || lower.contains(".claude");
            }));

            ZipEntry marker = zip.getEntry("skysgrassslabs-forward-fixture.properties");
            String contents = new String(readAll(zip.getInputStream(marker)), StandardCharsets.UTF_8);
            assertTrue(contents.contains("source_minecraft=1.10.2"));
            assertTrue(contents.contains("source_mod_version=1.0.0.110021"));
            assertTrue(contents.contains("expected_blocks=7"));
            assertFalse(contents.toLowerCase(Locale.ROOT).contains(":\\users\\"));
            assertFalse(contents.toLowerCase(Locale.ROOT).contains(":\\skysgrassslabs"));
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            for (int read = input.read(buffer); read >= 0; read = input.read(buffer)) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) {
            result.append(String.format("%02X", value & 0xff));
        }
        return result.toString();
    }

    private static byte[] readAll(InputStream input) throws Exception {
        try (InputStream source = input; java.io.ByteArrayOutputStream output =
                new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            for (int read = source.read(buffer); read >= 0; read = source.read(buffer)) {
                if (read > 0) output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
