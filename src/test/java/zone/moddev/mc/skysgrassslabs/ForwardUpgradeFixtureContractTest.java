package zone.moddev.mc.skysgrassslabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

public class ForwardUpgradeFixtureContractTest {
    private static final String FIXTURE_DIRECTORY = "src/test/resources/fixtures/";
    private static final Fixture[] FIXTURES = {
            new Fixture("1.10.2", "1.0.0.110021",
                    "D6923BFFE062C1F0C454190AB11F031825949DF8080D8000133A723DEC2770BF",
                    "2030960E217C3F61AE4919C91058696B02F9FAE570BE1CD7B698696EA7BEB861", 7,
                    false),
            new Fixture("1.11.2", "1.0.1.111021",
                    "E6ABAECBC818C4EB28D3324ACC27DCCCD30247B4268879D7A789F0E1F55028F5",
                    "56D8B8C1FA7F2289C9F9A3BCF2BEB2D15F0F880373D0647BB9BDBFA7E1D5FE54", 7,
                    false),
            new Fixture("1.12.2", "1.0.1.112021",
                    "A903E0AE94DE7AFEA92BBF773859A31119BDBEA6976C5135D0AD56E845DDB028",
                    "C6E83E66AFB35AE47661FB560F81A458B95FB50D87E940CE682B7C91DB034543", 7,
                    false),
            new Fixture("1.13.2", "1.0.1.113021",
                    "BB30D8108476F2E38EBA26D7B4FD9E0FB431413032A20071D2DE2139BEC7BA0C",
                    "42772E921FE7EAF8A8D1EA7C12F48C04626FDD0B880B827FB3A82FB7A5ACFC7A", 11,
                    false),
            new Fixture("1.14.4", "1.0.1.114041",
                    "F74B9D82994631492ADFC8685BDB8C4DB485B45B4017605C1954A595826D7B8B",
                    "213A09F31EE02CE1C01E4C504147C13D3BD63A6AA11EBE52CE41A38735D45D1B", 11,
                    false),
            new Fixture("1.15.2", "1.0.1.115021",
                    "8D2FEA6A0289B35F0C2728A6E93C09EFDDC3A699EC28DCAC4115EBFE47269D05",
                    "9AE28332EA21700C5DE8D3597FC40F5B06D85E8A7FB3C0DE650A2F8BC5E0895C", 11,
                    false),
            new Fixture("1.16.5", "1.0.1.116051",
                    "1A8DC264AFBEF2683C2D30963853B8E3D84329782D98347CB4CA44B78DE816C3",
                    "54388D358581579723EFBEF6B06DDEF775CEB6FF8DD9289DFDDE82B951A9A2D7", 11,
                    false),
            new Fixture("1.17.1", "1.0.1.117011",
                    "434BADF94D16E9DAE1C5C9ED0D49E067FE6E9669E3EEED32A078D57B0DF403EE",
                    "EA0705D70D95E4897449E53DBB91BE2EF7F2B500F731D4B137818942B42CC19B", 11,
                    true)
    };

    @Test
    public void fixturesAreLockedGenuineLegacyWorlds() throws Exception {
        for (Fixture fixture : FIXTURES) {
            Properties manifest = loadManifest(fixture);
            assertEquals(fixture.fixtureSha256, sha256(fixture.archive));
            assertEquals(fixture.fixtureSha256, manifest.getProperty("fixture_sha256"));
            assertEquals(fixture.minecraftVersion, manifest.getProperty("source_minecraft"));
            assertEquals(fixture.modVersion, manifest.getProperty("source_mod_version"));
            assertEquals(fixture.jarSha256, manifest.getProperty("source_jar_sha256"));
            assertEquals(Long.toString(fixture.archive.length()),
                    manifest.getProperty("fixture_bytes"));
            assertTrue(fixture.minecraftVersion + " fixture is no longer compact",
                    fixture.archive.length() < 300_000L);
        }
    }

    @Test
    public void fixturesContainOnlyRequiredWorldFiles() throws Exception {
        Set<String> baseExpected = new TreeSet<>(Arrays.asList(
                "data/capabilities.dat",
                "data/skysgrassslabs_world_state.dat",
                "level.dat",
                "level.dat_old",
                "region/r.0.0.mca",
                "skysgrassslabs-forward-fixture.properties"));
        for (Fixture fixture : FIXTURES) {
            Set<String> expected = new TreeSet<>(baseExpected);
            if (fixture.separateEntityStorage) {
                expected.add("entities/r.0.0.mca");
            }
            try (ZipFile zip = new ZipFile(fixture.archive)) {
                Set<String> actual = new TreeSet<>();
                zip.stream().filter(entry -> !entry.isDirectory())
                        .forEach(entry -> actual.add(entry.getName().replace('\\', '/')));
                assertEquals(fixture.minecraftVersion, expected, actual);
                assertFalse(actual.stream().anyMatch(name -> {
                    String lower = name.toLowerCase(Locale.ROOT);
                    return lower.contains("playerdata") || lower.contains("agent")
                            || lower.contains(".codex") || lower.contains(".claude");
                }));

                ZipEntry marker = zip.getEntry("skysgrassslabs-forward-fixture.properties");
                String contents = new String(readAll(zip.getInputStream(marker)),
                        StandardCharsets.UTF_8);
                assertTrue(contents.contains("source_minecraft=" + fixture.minecraftVersion));
                assertTrue(contents.contains("source_mod_version=" + fixture.modVersion));
                assertTrue(contents.contains("expected_blocks=" + fixture.expectedBlocks));
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
        try (InputStream source = input;
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            for (int read = source.read(buffer); read >= 0; read = source.read(buffer)) {
                if (read > 0) {
                    output.write(buffer, 0, read);
                }
            }
            return output.toByteArray();
        }
    }

    private static final class Fixture {
        private final String minecraftVersion;
        private final String modVersion;
        private final String fixtureSha256;
        private final String jarSha256;
        private final int expectedBlocks;
        private final boolean separateEntityStorage;
        private final File archive;
        private final File manifest;

        private Fixture(String minecraftVersion, String modVersion, String fixtureSha256,
                String jarSha256, int expectedBlocks, boolean separateEntityStorage) {
            this.minecraftVersion = minecraftVersion;
            this.modVersion = modVersion;
            this.fixtureSha256 = fixtureSha256;
            this.jarSha256 = jarSha256;
            this.expectedBlocks = expectedBlocks;
            this.separateEntityStorage = separateEntityStorage;
            String baseName = "skysgrassslabs-" + minecraftVersion + "-forward-world";
            archive = new File(FIXTURE_DIRECTORY + baseName + ".zip");
            manifest = new File(FIXTURE_DIRECTORY + baseName + ".manifest");
        }
    }
}
