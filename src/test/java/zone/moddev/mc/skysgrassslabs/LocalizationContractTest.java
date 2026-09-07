package zone.moddev.mc.skysgrassslabs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Test;

public class LocalizationContractTest {
    private static final Path LANG_DIR =
            Path.of("src/main/resources/assets/skysgrassslabs/lang");
    private static final List<String> KEYS = Arrays.asList(
            "block.skysgrassslabs.dirt_slab",
            "block.skysgrassslabs.grass_slab",
            "block.skysgrassslabs.path_slab",
            "block.skysgrassslabs.turf");
    private static final List<String> LOCALES = Arrays.asList(
            "de_at", "de_au", "de_de", "en_ca", "en_en", "en_gb", "en_pt", "en_us",
            "es_es", "es_mx", "fr_ca", "fr_fr", "ja_jp", "ko_kr", "pt_br", "pt_pt",
            "ru_ru", "zh_cn");

    @Test
    public void allEighteenLocalesHaveIdenticalOrderedKeys() throws Exception {
        Set<String> actual = new LinkedHashSet<>();
        try (Stream<Path> files = Files.list(LANG_DIR)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(path -> actual.add(path.getFileName().toString()
                            .replaceFirst("\\.json$", "")));
        }
        assertEquals(new LinkedHashSet<>(LOCALES), actual);
        for (String locale : LOCALES) {
            Map<String, String> translations = read(locale);
            assertEquals(locale, KEYS, new ArrayList<>(translations.keySet()));
            assertEquals(locale, 4, translations.size());
            assertFalse(locale, translations.values().stream().anyMatch(String::isBlank));
        }
    }

    @Test
    public void localeFilesAreCleanUtf8Json() throws Exception {
        for (String locale : LOCALES) {
            byte[] bytes = Files.readAllBytes(file(locale));
            assertFalse(locale + " must not contain a UTF-8 BOM",
                    bytes.length >= 3 && bytes[0] == (byte) 0xef
                            && bytes[1] == (byte) 0xbb && bytes[2] == (byte) 0xbf);
            String content = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            assertTrue(locale + " must end with a newline", content.endsWith("\n"));
            assertFalse(locale + " must use LF line endings", content.contains("\r"));
            assertFalse(locale + " contains a replacement character", content.contains("\ufffd"));
            for (String key : KEYS) {
                assertEquals(locale + " contains a missing or duplicate key " + key,
                        1, occurrences(content, "\"" + key + "\""));
            }
            for (String line : content.split("\n", -1)) {
                assertEquals(locale + " has trailing whitespace",
                        line.replaceFirst("[ \\t]+$", ""), line);
            }
            assertTrue(JsonParser.parseString(content).isJsonObject());
        }
    }

    @Test
    public void translationsAndRegionalDifferencesRemainIntact() throws Exception {
        assertValues("de_at", "Eanstufn", "Grosstufn", "Steigstufn", "Grassodn");
        assertValues("es_es", "Losa de tierra", "Losa de césped",
                "Losa de camino de hierba", "Tepe de césped");
        assertValues("es_mx", "Losa de tierra", "Losa de pasto",
                "Losa de sendero de pasto", "Tapete de pasto");
        assertValues("ja_jp", "土のハーフブロック", "草ブロックのハーフブロック",
                "草の道のハーフブロック", "芝生");
        assertValues("ru_ru", "Земляная плита", "Дёрновая плита", "Плита тропы", "Дёрн");
        assertValues("zh_cn", "泥土台阶", "草方块台阶", "草径台阶", "草皮");

        byte[] english = Files.readAllBytes(file("en_us"));
        for (String locale : Arrays.asList("en_ca", "en_en", "en_gb", "en_pt")) {
            assertArrayEquals(locale, english, Files.readAllBytes(file(locale)));
        }
        assertArrayEquals(Files.readAllBytes(file("de_de")), Files.readAllBytes(file("de_au")));
        assertNotEquals(read("es_es"), read("es_mx"));
        assertNotEquals(read("fr_ca"), read("fr_fr"));
        assertNotEquals(read("pt_br"), read("pt_pt"));
    }

    private static void assertValues(String locale, String... values) throws Exception {
        assertEquals(locale, Arrays.asList(values), new ArrayList<>(read(locale).values()));
    }

    private static Map<String, String> read(String locale) throws Exception {
        JsonObject json = JsonParser.parseString(Files.readString(file(locale))).getAsJsonObject();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            assertFalse(locale + " has duplicate keys", result.containsKey(entry.getKey()));
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        return result;
    }

    private static Path file(String locale) {
        return LANG_DIR.resolve(locale + ".json");
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        for (int index = value.indexOf(needle); index >= 0;
                index = value.indexOf(needle, index + needle.length())) {
            ++count;
        }
        return count;
    }
}
