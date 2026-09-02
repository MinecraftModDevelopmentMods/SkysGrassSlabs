package zone.moddev.mc.skysgrassslabs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            "tile.skysgrassslabs.dirt_slab.name",
            "tile.skysgrassslabs.grass_slab.name",
            "tile.skysgrassslabs.path_slab.name",
            "tile.skysgrassslabs.turf.name");
    private static final List<String> LOCALES = Arrays.asList(
            "de_AT", "de_AU", "de_DE",
            "en_CA", "en_EN", "en_GB", "en_PT", "en_US",
            "es_ES", "es_MX", "fr_CA", "fr_FR", "ja_JP", "ko_KR",
            "pt_BR", "pt_PT", "ru_RU", "zh_CN");

    @Test
    void allEighteenLocalesHaveExactOrderedKeyParity() throws Exception {
        File[] files = LANG_DIR.listFiles((dir, name) -> name.endsWith(".lang"));
        assertNotNull(files);
        Set<String> actual = new LinkedHashSet<String>();
        for (File file : files) actual.add(stripExtension(file.getName()));
        assertEquals(new LinkedHashSet<String>(LOCALES), actual);

        for (String locale : LOCALES) {
            Map<String, String> translations = read(locale);
            assertEquals(KEYS, new ArrayList<String>(translations.keySet()), locale);
            assertEquals(4, translations.size(), locale);
        }
    }

    @Test
    void localeFilesAreCleanUtf8() throws Exception {
        for (String locale : LOCALES) {
            byte[] bytes = Files.readAllBytes(file(locale).toPath());
            assertFalse(hasUtf8Bom(bytes), locale + " must not contain a UTF-8 BOM");
            String content = decodeUtf8(bytes, locale);
            assertTrue(content.endsWith("\n"), locale + " must end with a newline");
            assertFalse(content.contains("\r"), locale + " must use LF line endings");
            assertFalse(content.contains("\ufffd"), locale + " contains a replacement character");
            for (String line : content.split("\n", -1)) {
                assertEquals(line.replaceFirst("[ \\t]+$", ""), line,
                        locale + " has trailing whitespace");
            }
            for (Map.Entry<String, String> entry : read(locale).entrySet()) {
                assertFalse(entry.getValue().trim().isEmpty(),
                        locale + " has a blank value for " + entry.getKey());
            }
        }
    }

    @Test
    void translationsAndRegionalDifferencesAreLocked() throws Exception {
        assertValues("de_AT", "Eanstufn", "Grosstufn", "Steigstufn", "Grassodn");
        assertValues("de_DE", "Erdstufe", "Grasblockstufe", "Trampelpfadstufe", "Grassode");
        assertValues("es_ES", "Losa de tierra", "Losa de césped",
                "Losa de camino de hierba", "Tepe de césped");
        assertValues("es_MX", "Losa de tierra", "Losa de pasto",
                "Losa de sendero de pasto", "Tapete de pasto");
        assertValues("fr_CA", "Dalle de terre", "Dalle de gazon",
                "Dalle de sentier de gazon", "Plaque de gazon");
        assertValues("fr_FR", "Dalle de terre", "Dalle d'herbe",
                "Dalle de chemin d'herbe", "Plaque de gazon");
        assertValues("ja_JP", "土のハーフブロック", "草ブロックのハーフブロック",
                "草の道のハーフブロック", "芝生");
        assertValues("ko_KR", "흙 반 블록", "잔디 블록 반 블록", "잔디 길 반 블록", "잔디");
        assertValues("pt_BR", "Laje de Terra", "Laje de Bloco de Grama",
                "Laje de Caminho de Grama", "Placa de Grama");
        assertValues("pt_PT", "Degrau de Terra", "Degrau de Bloco de Relva",
                "Degrau de Caminho de Relva", "Placa de Relva");
        assertValues("ru_RU", "Земляная плита", "Дёрновая плита", "Плита тропы", "Дёрн");
        assertValues("zh_CN", "泥土台阶", "草方块台阶", "草径台阶", "草皮");

        byte[] english = Files.readAllBytes(file("en_US").toPath());
        for (String locale : Arrays.asList("en_CA", "en_EN", "en_GB", "en_PT")) {
            assertArrayEquals(english, Files.readAllBytes(file(locale).toPath()), locale);
        }
        assertArrayEquals(Files.readAllBytes(file("de_DE").toPath()),
                Files.readAllBytes(file("de_AU").toPath()));
        assertNotEquals(read("es_ES"), read("es_MX"));
        assertNotEquals(read("fr_CA"), read("fr_FR"));
        assertNotEquals(read("pt_BR"), read("pt_PT"));
    }

    private static void assertValues(String locale, String... values) throws Exception {
        assertEquals(Arrays.asList(values), new ArrayList<String>(read(locale).values()), locale);
    }

    private static Map<String, String> read(String locale) throws Exception {
        Map<String, String> values = new LinkedHashMap<String, String>();
        for (String line : Files.readAllLines(file(locale).toPath(), StandardCharsets.UTF_8)) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            int separator = line.indexOf('=');
            assertTrue(separator > 0, locale + " has a malformed line: " + line);
            String key = line.substring(0, separator);
            assertFalse(values.containsKey(key), locale + " has duplicate key " + key);
            values.put(key, line.substring(separator + 1));
        }
        return values;
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

    private static File file(String locale) {
        return new File(LANG_DIR, locale + ".lang");
    }

    private static String stripExtension(String name) {
        return name.substring(0, name.length() - ".lang".length());
    }

}
