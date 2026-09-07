package zone.moddev.mc.skysgrassslabs.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
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
import org.junit.Test;

public class GrassSlabsMigrationFixtureContractTest {
    private static final File ARCHIVE = new File(
            "src/test/resources/fixtures/grassslabs-1.18.2-migration-world.zip");
    private static final File MANIFEST = new File(
            "src/test/resources/fixtures/grassslabs-1.18.2-migration-world.manifest");
    private static final String FIXTURE_SHA256 =
            "B7597F48AFB9596583D8690222F4ABDD29396C00311DF7FD881BB2EA69E95422";
    private static final String SOURCE_JAR_SHA256 =
            "53D4DCE01D5BB0DC15AEC48B3AFA38588A0F840E086E3846CA9B7D51D33799A4";

    @Test
    public void fixtureIsChecksumLockedAndSourceQualified() throws Exception {
        Properties manifest = loadManifest();
        assertEquals(FIXTURE_SHA256, sha256(ARCHIVE));
        assertEquals(FIXTURE_SHA256, manifest.getProperty("fixture_sha256"));
        assertEquals(Long.toString(ARCHIVE.length()), manifest.getProperty("fixture_bytes"));
        assertEquals("1.18.2", manifest.getProperty("source_minecraft"));
        assertEquals("1.2.1", manifest.getProperty("source_mod_version"));
        assertEquals("grassslabs", manifest.getProperty("source_mod_id"));
        assertEquals(SOURCE_JAR_SHA256, manifest.getProperty("source_jar_sha256"));
        assertEquals("4f715ffd1ab73c1eaafc4ccff2f056ee9d2a56df",
                manifest.getProperty("source_commit"));
        assertTrue("migration fixture is no longer compact", ARCHIVE.length() < 100_000L);
    }

    @Test
    public void fixtureContainsOnlyTheRequiredAnonymousWorldFiles() throws Exception {
        Set<String> expected = new TreeSet<>(Arrays.asList(
                "data/capabilities.dat",
                "entities/r.0.0.mca",
                "grassslabs-migration-fixture.properties",
                "level.dat",
                "level.dat_old",
                "region/r.0.0.mca"));
        try (ZipFile zip = new ZipFile(ARCHIVE)) {
            Set<String> actual = new TreeSet<>();
            zip.stream().filter(entry -> !entry.isDirectory())
                    .forEach(entry -> actual.add(entry.getName().replace('\\', '/')));
            assertEquals(expected, actual);
            assertFalse(actual.stream().anyMatch(name -> {
                String lower = name.toLowerCase(Locale.ROOT);
                return lower.contains("playerdata") || lower.contains("agent")
                        || lower.contains(".codex") || lower.contains(".claude");
            }));

            ZipEntry marker = zip.getEntry("grassslabs-migration-fixture.properties");
            String contents = new String(readAll(zip.getInputStream(marker)),
                    StandardCharsets.UTF_8);
            assertTrue(contents.contains("fixture_identity=grassslabs-migration-118"));
            assertTrue(contents.contains("expected_supported_slab_blocks=9"));
            assertTrue(contents.contains("expected_supported_safe_carpet_blocks=1"));
            assertTrue(contents.contains("expected_supported_retained_carpet_blocks=1"));
            assertTrue(contents.contains("expected_unsupported_blocks=8"));
            assertFalse(contents.toLowerCase(Locale.ROOT).contains(":\\users\\"));
            assertFalse(contents.toLowerCase(Locale.ROOT).contains(":\\skysgrassslabs"));
        }
    }

    private static Properties loadManifest() throws Exception {
        Properties manifest = new Properties();
        try (InputStream input = Files.newInputStream(MANIFEST.toPath())) {
            manifest.load(input);
        }
        return manifest;
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            for (int read = input.read(buffer); read >= 0; read = input.read(buffer)) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) {
            result.append(String.format("%02X", value & 0xff));
        }
        return result.toString();
    }

    private static byte[] readAll(InputStream input) throws Exception {
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            for (int read = source.read(buffer); read >= 0; read = source.read(buffer)) {
                if (read > 0) {
                    output.write(buffer, 0, read);
                }
            }
            return output.toByteArray();
        }
    }
}
