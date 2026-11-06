package zone.moddev.mc.skysgrassslabs.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class ConfigSaveTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void savePublishesOneCompleteSetOfChoicesAndKeepsStartupSettings() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path file = directory.resolve(SkysGrassSlabsConfig.FILE_NAME);
        FMLPaths.loadAbsolutePaths(directory);
        TestContainer container = new TestContainer();
        SkysGrassSlabsConfig.register(container.getEventBus(), container);

        CommentedConfig defaults = CommentedConfig.inMemory();
        container.config.getSpec().correct(defaults);
        try (var writer = Files.newBufferedWriter(file)) {
            new TomlWriter().write(defaults, writer);
        }
        container.tracker.loadConfigs(ModConfig.Type.COMMON, directory);
        try {
            CommentedConfig values = container.config.getLoadedConfig().config();
            values.set("compat.unrelated", "kept");
            values.setComment("compat.unrelated", "A pack setting");
            values.set("other", values.createSubConfig());
            values.set("other.setting", 42);

            List<List<Boolean>> published = new CopyOnWriteArrayList<>();
            container.getEventBus().addListener((ModConfigEvent.Reloading event) -> {
                if (event.getConfig() == container.config) {
                    published.add(choices(read(file)));
                }
            });
            SkysGrassSlabsConfig.activateSettingsForThisRun();
            SkysGrassSlabsConfig.saveForNextStart(false, true, true);

            assertFalse("NeoForge should notify listeners after saving", published.isEmpty());
            for (List<Boolean> saved : published) {
                assertEquals(List.of(false, true, true), saved);
            }
            assertFalse(SkysGrassSlabsConfig.configuredGrassGeneration());
            assertTrue(SkysGrassSlabsConfig.configuredLegacyReplacement());
            assertTrue(SkysGrassSlabsConfig.configuredGrassSlabsReplacement());
            assertTrue(SkysGrassSlabsConfig.generateGrassSlabs());
            assertFalse(SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs());
            assertFalse(SkysGrassSlabsConfig.forceReplaceGrassSlabsModContent());
            CommentedConfig saved = read(file);
            assertEquals("kept", saved.get("compat.unrelated"));
            assertEquals("A pack setting", saved.getComment("compat.unrelated").trim());
            assertEquals(42, (int) saved.get("other.setting"));
        } finally {
            container.tracker.unloadConfigs(ModConfig.Type.COMMON);
            SkysGrassSlabsConfig.resetRunStateForTests();
        }
    }

    private static List<Boolean> choices(CommentedConfig values) {
        return List.of(values.get("worldgen.generateGrassSlabs"),
                values.get("compat.forceReplaceBuildingBricksSlabs"),
                values.get("compat.forceReplaceGrassSlabsModContent"));
    }

    private static CommentedConfig read(Path file) {
        try (var input = Files.newBufferedReader(file)) {
            return new TomlParser().parse(input);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** Uses a private tracker and bus so the test never touches the game's settings. */
    private static final class TestContainer extends ModContainer {
        private final ConfigTracker tracker = new ConfigTracker();
        private final IEventBus bus = BusBuilder.builder().build();
        private ModConfig config;

        private TestContainer() {
            super((IModInfo) Proxy.newProxyInstance(IModInfo.class.getClassLoader(),
                    new Class<?>[] {IModInfo.class}, (proxy, method, arguments) -> {
                        if (method.getName().equals("getModId")) {
                            return "skysgrassslabs";
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }));
        }

        @Override
        public IEventBus getEventBus() {
            return bus;
        }

        @Override
        public void registerConfig(ModConfig.Type type, IConfigSpec spec, String fileName) {
            config = tracker.registerConfig(type, spec, this, fileName);
        }
    }
}
