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
    private static final String FIXTURE_DIRECTORY = "src/test/resources/fixtures/";
    private static final Fixture[] FIXTURES = {
            new Fixture("1.10.2", "1.0.0.110021",
                    "D6923BFFE062C1F0C454190AB11F031825949DF8080D8000133A723DEC2770BF",
                    "2030960E217C3F61AE4919C91058696B02F9FAE570BE1CD7B698696EA7BEB861"),
            new Fixture("1.11.2", "1.0.1.111021",
                    "E6ABAECBC818C4EB28D3324ACC27DCCCD30247B4268879D7A789F0E1F55028F5",
                    "56D8B8C1FA7F2289C9F9A3BCF2BEB2D15F0F880373D0647BB9BDBFA7E1D5FE54"),
            new Fixture("1.12.2", "1.0.1.112021",
                    "A903E0AE94DE7AFEA92BBF773859A31119BDBEA6976C5135D0AD56E845DDB028",
                    "C6E83E66AFB35AE47661FB560F81A458B95FB50D87E940CE682B7C91DB034543")
    };

    @Test
    void fixturesAreLockedGenuineLegacyWorlds() throws Exception {
        for (Fixture fixture : FIXTURES) {
            Properties manifest = loadManifest(fixture);
            assertEquals(fixture.fixtureSha256, sha256(fixture.archive));
            assertEquals(fixture.fixtureSha256, manifest.getProperty("fixture_sha256"));
            assertEquals(fixture.minecraftVersion, manifest.getProperty("source_minecraft"));
            assertEquals(fixture.modVersion, manifest.getProperty("source_mod_version"));
            assertEquals(fixture.jarSha256, manifest.getProperty("source_jar_sha256"));
            assertEquals(Long.toString(fixture.archive.length()),
                    manifest.getProperty("fixture_bytes"));
            assertTrue(fixture.archive.length() < 125_000L,
                    fixture.minecraftVersion + " fixture is no longer compact");
        }
    }

    @Test
    void fixturesContainOnlyTheRequiredWorldFiles() throws Exception {
        Set<String> expected = new TreeSet<String>(Arrays.asList(
                "data/capabilities.dat",
                "data/skysgrassslabs_world_state.dat",
                "level.dat",
                "level.dat_old",
                "region/r.0.0.mca",
                "skysgrassslabs-forward-fixture.properties"));
        for (Fixture fixture : FIXTURES) {
            try (ZipFile zip = new ZipFile(fixture.archive)) {
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
                String contents = new String(readAll(zip.getInputStream(marker)),
                        StandardCharsets.UTF_8);
                assertTrue(contents.contains("source_minecraft=" + fixture.minecraftVersion));
                assertTrue(contents.contains("source_mod_version=" + fixture.modVersion));
                assertTrue(contents.contains("expected_blocks=7"));
                assertFalse(contents.toLowerCase(Locale.ROOT).contains(":\\users\\"));
                assertFalse(contents.toLowerCase(Locale.ROOT).contains(":\\skysgrassslabs"));
            }
        }
    }

    private static Properties loadManifest(Fixture fixture) throws Exception {
        Properties manifest = new Properties();
        try (InputStream input = Files.newInputStream(fixture.manifest.toPath())) {
            manifest.load(input);
        }
        return manifest;
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

    private static final class Fixture {
        private final String minecraftVersion;
        private final String modVersion;
        private final String fixtureSha256;
        private final String jarSha256;
        private final File archive;
        private final File manifest;

        private Fixture(String minecraftVersion, String modVersion, String fixtureSha256,
                String jarSha256) {
            this.minecraftVersion = minecraftVersion;
            this.modVersion = modVersion;
            this.fixtureSha256 = fixtureSha256;
            this.jarSha256 = jarSha256;
            String baseName = "skysgrassslabs-" + minecraftVersion + "-forward-world";
            this.archive = new File(FIXTURE_DIRECTORY + baseName + ".zip");
            this.manifest = new File(FIXTURE_DIRECTORY + baseName + ".manifest");
        }
    }
}
