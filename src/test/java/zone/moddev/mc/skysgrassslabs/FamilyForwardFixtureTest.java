package zone.moddev.mc.skysgrassslabs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class FamilyForwardFixtureTest {
    @org.junit.jupiter.api.Test
    void retiredRegistryNoticesAreBoundToTheNativeSaveAndCompleteBiomeAudit() throws Exception {
        Path folder = Paths.get("src/test/resources/fixtures/grass-family-1.18.2");
        Path usageFile = folder.resolve("registry-uses.json");
        assertEquals("7887EE37023BBEC0C684CAC0FF034F3A039367F401A09F5310D3414EFE96E2A7",
                digest(usageFile));
        JsonObject usage = json(usageFile);
        JsonObject profile = json(Paths.get("tools/native-log-profiles.json"))
                .getAsJsonObject("profiles").getAsJsonObject("family-118-upgrade-run");
        assertEquals(digest(folder.resolve("world.zip")), usage.get("source_world_sha256").getAsString());
        assertEquals(usage.get("mappings"), profile.get("mappings"));
        assertEquals(0, usage.get("numeric_biome_arrays").getAsInt());
        assertEquals(30720, usage.get("named_biome_cells").getAsInt());
        for (var entry : usage.getAsJsonObject("retired_poi_saved_uses").entrySet()) {
            assertEquals(0, entry.getValue().getAsInt(), entry.getKey());
        }
        try (java.util.zip.ZipFile archive = new java.util.zip.ZipFile(folder.resolve("world.zip").toFile())) {
            var registries = net.minecraft.nbt.NbtIo.readCompressed(
                    archive.getInputStream(archive.getEntry("world/level.dat")))
                    .getCompound("fml").getCompound("Registries");
            for (var registry : usage.getAsJsonObject("mappings").entrySet()) {
                for (var tag : registries.getCompound(registry.getKey()).getList("ids", 10)) {
                    var saved = (net.minecraft.nbt.CompoundTag) tag;
                    var expected = registry.getValue().getAsJsonObject().get(saved.getString("K"));
                    if (expected != null) {
                        assertEquals(expected.getAsInt(), saved.getInt("V"), saved.getString("K"));
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
            "1.19.4,77A6FC338FC08EDFD7CFF06DE495C69352D3BA1D2BD2F990A985D484B5420CC5,20,0,0,family-119-upgrade-run",
            "1.18.2,989DA26DB3BF6BE780EE58D8DEAB89A52BA4E18F95902FAC6D06B8F74C9FAEB6,20,0,0,family-118-upgrade-run",
            "1.16.5,F885F3E9113B20BBE7489A333E98923D3B7BFFB62C486AD669CBBE7E6A519E93,568,0,14400,family-116-upgrade-run",
            "1.17.1,8EE2B8BA2D7A9AF4DAD729B77746C7C44A3ABFE8D3299A99E9EFEFFCA1185407,588,50,14400,family-117-upgrade-run",
            "1.16.5,E4CA9898FDDC40843510851E006CA690A6825C081D1094944C9715CE3EB7346F,1,0,0,forward-1.16-upgrade-run"
    })
    void nativeBiomeContractCoversEveryCompletedSourceChunk(String minecraft, String hash,
            int completedChunks, int partialChunks, int expectedRenamed, String profileName) throws Exception {
        Path folder = Paths.get("src/test/resources/fixtures/grass-family-" + minecraft);
        Path contractFile = folder.resolve("biomes.json");
        Path worldFile = folder.resolve("world.zip");
        if (profileName.equals("forward-1.16-upgrade-run")) {
            folder = Paths.get("src/test/resources/fixtures");
            contractFile = folder.resolve("skysgrassslabs-1.16.5-forward-biomes.json");
            worldFile = folder.resolve("skysgrassslabs-1.16.5-forward-world.zip");
        }
        assertEquals(hash, digest(contractFile));
        JsonObject contract = json(contractFile);
        assertEquals(minecraft, contract.get("source_minecraft").getAsString());
        assertEquals(digest(worldFile), contract.get("source_world_sha256").getAsString());
        assertEquals(completedChunks, contract.getAsJsonArray("chunks").size());
        assertEquals(partialChunks, contract.getAsJsonArray("partial_chunks").size());
        assertEquals(expectedRenamed, contract.get("native_renamed_cells").getAsInt());
        assertEquals(completedChunks * ((minecraft.equals("1.18.2") || minecraft.equals("1.19.4")) ? 1536 : 1024), contract.get("checked_cells").getAsInt());
        JsonObject profile = json(Paths.get("tools/native-log-profiles.json"))
                .getAsJsonObject("profiles").getAsJsonObject(profileName);
        assertEquals(expectedRenamed, profile.get("saved_uses").getAsInt());
        assertEquals(hash, profile.getAsJsonObject("native_biome_contract").get("sha256").getAsString());
        java.util.Set<String> coordinates = new java.util.HashSet<>();
        int renamed = 0;
        for (var entry : contract.getAsJsonArray("chunks")) {
            JsonObject row = entry.getAsJsonObject();
            assertTrue(coordinates.add(row.get("x") + "," + row.get("z")));
            try (var input = new java.util.zip.InflaterInputStream(new java.io.ByteArrayInputStream(
                    java.util.Base64.getDecoder().decode(row.get("cells").getAsString())))) {
                byte[] cells = input.readAllBytes();
                assertEquals((minecraft.equals("1.18.2") || minecraft.equals("1.19.4")) ? 1536 : 1024, cells.length);
                for (byte cell : cells) {
                    String name = contract.getAsJsonObject("source_names")
                            .get(Integer.toString(Byte.toUnsignedInt(cell))).getAsString();
                    if (net.minecraft.util.datafix.fixes.CavesAndCliffsRenames.RENAMES.containsKey(name)
                            || name.equals("minecraft:deep_warm_ocean")) {
                        renamed++;
                    }
                }
            }
        }
        assertEquals(expectedRenamed, renamed);
    }

    @ParameterizedTest
    @CsvSource({
            "1.19.4,B804B42B65942345E99F004130FC13932BBE55565188DBCF46CB4FFFD5A28B5C,3A97C2D7E1A2E1D9C33D513BE08F9B2D3611AC914185DED8C8B9138D76EF5B82,7459aaa214c86c8e759f77feb2e2f61c1790285b,3337,source_jar_sha256",
            "1.18.2,FB1D5187194299216263B8E789A3762B63FD52CA11BEE8E865A86FBC55A0126B,357E8792542EEC84E4D3DEF3E91193559B826574DC23DB8A5081A69ABF112CE7,98bf6c4313589ad233b8c6301de781f608da41ea,2975,source_jar_sha256",
            "1.15.2,15CF16E1CE347C1D85B524D07B9D167AE82423AE240F354F617648EB5FF6D93C,A0D741B5D641DE05EFA37DDC0565B5FC3164661A6B03890E71ECF86F7EB56E51,5f63feb3dffa4dbabd2f974c22c40d6be8e6b928,2230,main_sha256",
            "1.16.5,9F5998322AE6EF044DD1A2DAB525CE539ABC2CF67C26CF0AA1D77CF0ACBC870F,C975A9BC836DBFC9B4A01E2D1DCB350F8201AB12BFBA8A0D9CFE1D0609A2A568,803bd3dcbe6a67ec631da99a616ca3efe2e404a2,2586,source_jar_sha256",
            "1.17.1,E4EBBC9FC8F9749E4498C3BE9FB2F625E642F166E9E76C886E40AF69CC48851B,5A5EC232E9EB05B870315F7080B2F2B66003F2D66755607135E559B1D1E0D506,d74f2e6abaafe07e5f0755f937c9e880086931c8,2730,source_jar_sha256"
    })
    void precedingFamilyFixtureIsSealedAndContainsTheCompleteOrdinaryStateMatrix(
            String minecraft, String worldHash, String jarHash, String sourceCommit,
            int dataVersion, String jarField) throws Exception {
        Path folder = Paths.get("src/test/resources/fixtures/grass-family-" + minecraft);
        JsonObject source = json(folder.resolve("provenance.json"));
        assertEquals(worldHash, digest(folder.resolve("world.zip")));
        assertEquals(digest(folder.resolve("world.zip")), source.get("world_sha256").getAsString());
        assertEquals(digest(folder.resolve("source-fixture.json")), source.get("manifest_sha256").getAsString());
        assertEquals(jarHash, source.get(jarField).getAsString());
        assertEquals(sourceCommit, source.get("source_commit").getAsString());
        assertEquals(dataVersion, source.get("source_data_version").getAsInt());
        assertEquals(19, source.get("states").getAsInt());
        assertEquals(8, source.get("wet_states").getAsInt());
        assertEquals(19, source.get("container_stacks").getAsInt());
        assertEquals(1, source.get("schema_version").getAsInt());
        assertTrue(source.get("source_world_unchanged").getAsBoolean());
        try (java.util.zip.ZipFile archive = new java.util.zip.ZipFile(folder.resolve("world.zip").toFile())) {
            assertNotNull(archive.getEntry("world/level.dat"));
            assertNull(archive.getEntry("level.dat"));
        }
    }

    private JsonObject json(Path path) throws Exception {
        return new JsonParser().parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private String digest(Path path) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))) {
            result.append(String.format(Locale.ROOT, "%02X", value));
        }
        return result.toString();
    }
}
