package zone.moddev.mc.skysgrassslabs.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.utils.CommentedConfigWrapper;
import com.electronwill.nightconfig.core.utils.ObservedMap;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConfigSaveTest {
    @After
    public void clearRunSettings() throws Exception {
        SkysGrassSlabsConfig.resetRunStateForTests();
        field("loadedValues").set(null, null);
    }

    @Test
    public void autosaveNeverPublishesOnlyPartOfThePlayersChoices() throws Exception {
        ForgeConfigSpec spec = (ForgeConfigSpec) field("SPEC").get(null);
        CommentedConfig values = CommentedConfig.inMemory();
        spec.setConfig(values);
        AutosaveView watched = new AutosaveView(values);
        spec.setConfig(watched);
        field("loadedValues").set(null, watched);
        values.set("compat.unrelated", "kept");
        values.setComment("compat.unrelated", "A pack setting");
        values.set("other.setting", 42);
        SkysGrassSlabsConfig.activateSettingsForThisRun();
        watched.saves.clear();

        SkysGrassSlabsConfig.saveForNextStart(false, true, true);

        assertFalse("The edit reaches the watched config", watched.saves.isEmpty());
        for (List<Boolean> saved : watched.saves) {
            assertEquals("A watcher must never receive an intermediate set of choices",
                    List.of(false, true, true), saved);
        }
        assertFalse(SkysGrassSlabsConfig.configuredGrassGeneration());
        assertTrue(SkysGrassSlabsConfig.configuredLegacyReplacement());
        assertTrue(SkysGrassSlabsConfig.configuredGrassSlabsReplacement());
        assertTrue(SkysGrassSlabsConfig.generateGrassSlabs());
        assertFalse(SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs());
        assertFalse(SkysGrassSlabsConfig.forceReplaceGrassSlabsModContent());
        assertEquals("kept", values.get("compat.unrelated"));
        assertEquals("A pack setting", values.getComment("compat.unrelated"));
        assertEquals(42, (int) values.get("other.setting"));
    }

    private static Field field(String name) throws Exception {
        Field field = SkysGrassSlabsConfig.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    /** Mirrors NightConfig's autosave callbacks without relying on watcher timing. */
    private static final class AutosaveView extends CommentedConfigWrapper<CommentedConfig> {
        private final List<List<Boolean>> saves = new ArrayList<>();

        AutosaveView(CommentedConfig values) {
            super(values);
        }

        @Override
        public <T> T set(List<String> path, Object value) {
            T previous = super.set(path, value);
            recordSave();
            return previous;
        }

        @Override
        public Map<String, Object> valueMap() {
            return new ObservedMap<>(super.valueMap(), this::recordSave);
        }

        private void recordSave() {
            saves.add(List.of(get("worldgen.generateGrassSlabs"),
                    get("compat.forceReplaceBuildingBricksSlabs"),
                    get("compat.forceReplaceGrassSlabsModContent")));
        }
    }
}
