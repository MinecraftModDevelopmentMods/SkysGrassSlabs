package zone.moddev.mc.skysgrassslabs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LocalizationContractTest {
    private static final File LANG_DIR =
            new File("src/main/resources/assets/skysgrassslabs/lang");
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
    void allEighteenLowercaseJsonLocalesHaveExactOrderedKeyParity() throws Exception {
        File[] files = LANG_DIR.listFiles((directory, name) -> name.endsWith(".json"));
        Set<String> actual = new LinkedHashSet<String>();
        if (files != null) {
            Arrays.sort(files, (left, right) -> left.getName().compareTo(right.getName()));
            for (File file : files) actual.add(stripExtension(file.getName()));
        }
        assertEquals(new LinkedHashSet<String>(LOCALES), actual);
        for (String locale : LOCALES) {
            Map<String, String> translations = read(locale);
            assertEquals(KEYS, new ArrayList<String>(translations.keySet()), locale);
            assertEquals(4, translations.size(), locale);
            assertFalse(translations.values().stream().anyMatch(value -> value.trim().isEmpty()));
        }
    }

    @Test
    void localeFilesAreCleanUtf8Json() throws Exception {
        for (String locale : LOCALES) {
            byte[] bytes = Files.readAllBytes(file(locale).toPath());
            assertFalse(hasUtf8Bom(bytes), locale + " must not contain a UTF-8 BOM");
            String content = decodeUtf8(bytes, locale);
            assertTrue(content.endsWith("\n"), locale + " must end with a newline");
            assertFalse(content.contains("\r"), locale + " must use LF line endings");
            assertFalse(content.contains("\ufffd"), locale + " contains a replacement character");
            for (String key : KEYS) {
                assertEquals(1, occurrences(content, "\"" + key + "\""),
                        locale + " contains a missing or duplicate key " + key);
            }
            for (String line : content.split("\n", -1)) {
                assertEquals(line.replaceFirst("[ \\t]+$", ""), line,
                        locale + " has trailing whitespace");
            }
            assertTrue(new JsonParser().parse(content).isJsonObject());
        }
    }

    @Test
    void translationsAndRegionalDifferencesRemainIntact() throws Exception {
        assertValues("de_at", "Eanstufn", "Grosstufn", "Steigstufn", "Grassodn");
        assertValues("es_es", "Losa de tierra", "Losa de césped",
                "Losa de camino de hierba", "Tepe de césped");
        assertValues("es_mx", "Losa de tierra", "Losa de pasto",
                "Losa de sendero de pasto", "Tapete de pasto");
        assertValues("ja_jp", "土のハーフブロック", "草ブロックのハーフブロック",
                "草の道のハーフブロック", "芝生");
        assertValues("ru_ru", "Земляная плита", "Дёрновая плита", "Плита тропы", "Дёрн");
        assertValues("zh_cn", "泥土台阶", "草方块台阶", "草径台阶", "草皮");

        byte[] english = Files.readAllBytes(file("en_us").toPath());
        for (String locale : Arrays.asList("en_ca", "en_en", "en_gb", "en_pt")) {
            assertArrayEquals(english, Files.readAllBytes(file(locale).toPath()), locale);
        }
        assertArrayEquals(Files.readAllBytes(file("de_de").toPath()),
                Files.readAllBytes(file("de_au").toPath()));
        assertNotEquals(read("es_es"), read("es_mx"));
        assertNotEquals(read("fr_ca"), read("fr_fr"));
        assertNotEquals(read("pt_br"), read("pt_pt"));
    }

    private static void assertValues(String locale, String... values) throws Exception {
        assertEquals(Arrays.asList(values), new ArrayList<String>(read(locale).values()), locale);
    }

    private static Map<String, String> read(String locale) throws Exception {
        String content = new String(Files.readAllBytes(file(locale).toPath()),
                StandardCharsets.UTF_8);
        JsonObject json = new JsonParser().parse(content).getAsJsonObject();
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            assertFalse(result.containsKey(entry.getKey()), locale + " has duplicate keys");
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        return result;
    }

    private static String decodeUtf8(byte[] bytes, String locale) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new AssertionError(locale + " is not valid UTF-8", exception);
        }
    }

    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3 && bytes[0] == (byte) 0xef
                && bytes[1] == (byte) 0xbb && bytes[2] == (byte) 0xbf;
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        for (int index = value.indexOf(needle); index >= 0;
                index = value.indexOf(needle, index + needle.length())) {
            ++count;
        }
        return count;
    }

    private static File file(String locale) {
        return new File(LANG_DIR, locale + ".json");
    }

    private static String stripExtension(String name) {
        return name.substring(0, name.length() - ".json".length());
    }
}
