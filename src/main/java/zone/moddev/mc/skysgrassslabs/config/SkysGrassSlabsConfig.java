package zone.moddev.mc.skysgrassslabs.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.concurrent.ConcurrentCommentedConfig;
import org.apache.commons.lang3.tuple.Pair;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.config.IConfigSpec.ILoadedConfig;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;

/** Common configuration and one-time migration from the 1.10 configuration file. */
public final class SkysGrassSlabsConfig {
    public static final String FORCE_REPLACE_BUILDINGBRICKS_SLABS =
            "forceReplaceBuildingBricksSlabs";
    public static final String FORCE_REPLACE_GRASS_SLABS_MOD_CONTENT =
            "forceReplaceGrassSlabsModContent";
    public static final String GENERATE_GRASS_SLABS = "generateGrassSlabs";
    public static final String FILE_NAME = "skysgrassslabs-common.toml";

    private static final Common COMMON;
    private static final ModConfigSpec SPEC;
    private static volatile ModConfig commonConfig;
    private static volatile boolean compatibilitySuppressed;
    private static boolean settingsActivated;
    private static boolean activeGrassGeneration;
    private static boolean activeLegacyReplacement;
    private static boolean activeGrassSlabsReplacement;

    static {
        Pair<Common, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static void register(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, FILE_NAME);
        modBus.addListener((ModConfigEvent.Loading event) -> {
            if (event.getConfig().getSpec() == SPEC) {
                commonConfig = event.getConfig();
            }
        });
    }

    public static void migrateLegacyConfig() {
        migrateLegacyConfig(FMLPaths.CONFIGDIR.get());
    }

    static boolean migrateLegacyConfig(Path configDirectory) {
        Path oldFile = configDirectory.resolve("skysgrassslabs.cfg");
        Path newFile = configDirectory.resolve(FILE_NAME);
        if (Files.exists(newFile) || !Files.isRegularFile(oldFile)) {
            return false;
        }
        boolean forceReplace = readLegacyBoolean(oldFile,
                FORCE_REPLACE_BUILDINGBRICKS_SLABS, false);
        boolean forceReplaceGrassSlabs = readLegacyBoolean(oldFile,
                FORCE_REPLACE_GRASS_SLABS_MOD_CONTENT, false);
        boolean worldgen = readLegacyBoolean(oldFile, GENERATE_GRASS_SLABS, true);
        List<String> toml = List.of(
                "[compat]",
                FORCE_REPLACE_BUILDINGBRICKS_SLABS + " = " + forceReplace,
                FORCE_REPLACE_GRASS_SLABS_MOD_CONTENT + " = " + forceReplaceGrassSlabs,
                "",
                "[worldgen]",
                GENERATE_GRASS_SLABS + " = " + worldgen);
        try {
            Files.createDirectories(configDirectory);
            Files.write(newFile, toml, StandardCharsets.UTF_8);
            SkysGrassSlabs.LOGGER.info("Migrated the legacy Sky's Grass Slabs configuration to {}",
                    newFile);
            return true;
        } catch (IOException exception) {
            SkysGrassSlabs.LOGGER.warn("Could not migrate the legacy Sky's Grass Slabs config; "
                    + "NeoForge defaults will be used", exception);
            return false;
        }
    }

    private static boolean readLegacyBoolean(Path file, String key, boolean fallback) {
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String value = line.trim();
                String prefix = "B:" + key + "=";
                if (!value.startsWith(prefix)) {
                    continue;
                }
                String parsed = value.substring(prefix.length()).trim();
                if ("true".equalsIgnoreCase(parsed)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(parsed)) {
                    return false;
                }
                return fallback;
            }
        } catch (IOException exception) {
            SkysGrassSlabs.LOGGER.warn("Could not read legacy configuration {}", file, exception);
        }
        return fallback;
    }

    public static boolean generateGrassSlabs() {
        return settingsActivated ? activeGrassGeneration : COMMON.generateGrassSlabs.get();
    }

    public static boolean forceReplaceBuildingBricksSlabs() {
        return settingsActivated ? activeLegacyReplacement : COMMON.forceReplaceBuildingBricksSlabs.get();
    }

    public static boolean forceReplaceGrassSlabsModContent() {
        return settingsActivated ? activeGrassSlabsReplacement : COMMON.forceReplaceGrassSlabsModContent.get();
    }

    public static boolean configuredGrassSlabsReplacement() {
        return COMMON.forceReplaceGrassSlabsModContent.getRaw();
    }

    /** Keep a running world on its startup settings when the Config screen saves new choices. */
    public static void activateSettingsForThisRun() {
        activeGrassGeneration = COMMON.generateGrassSlabs.get();
        activeLegacyReplacement = COMMON.forceReplaceBuildingBricksSlabs.get();
        activeGrassSlabsReplacement = COMMON.forceReplaceGrassSlabsModContent.get();
        settingsActivated = true;
    }

    public static boolean configuredGrassGeneration() {
        return COMMON.generateGrassSlabs.getRaw();
    }

    public static boolean configuredLegacyReplacement() {
        return COMMON.forceReplaceBuildingBricksSlabs.getRaw();
    }

    public static void saveForNextStart(boolean generateGrass, boolean replaceLegacy) {
        saveForNextStart(generateGrass, replaceLegacy, configuredGrassSlabsReplacement());
    }

    public static void saveForNextStart(boolean generateGrass, boolean replaceLegacy,
            boolean replaceGrassSlabs) {
        ModConfig config = commonConfig;
        ILoadedConfig loaded = config == null ? null : config.getLoadedConfig();
        if (loaded == null) {
            throw new IllegalStateException("Grass Slabs settings have not loaded yet");
        }
        CommentedConfig values = loaded.config();
        // Keep all three changes together under NightConfig's write lock, then
        // let NeoForge save the file and notify any reload listeners.
        if (values instanceof ConcurrentCommentedConfig concurrent) {
            concurrent.bulkCommentedUpdate(view -> {
                setChoices(view, generateGrass, replaceLegacy, replaceGrassSlabs);
            });
        } else {
            setChoices(values, generateGrass, replaceLegacy, replaceGrassSlabs);
        }
        loaded.save();
    }

    private static void setChoices(CommentedConfig values, boolean generateGrass,
            boolean replaceLegacy, boolean replaceGrassSlabs) {
        values.set("worldgen." + GENERATE_GRASS_SLABS, generateGrass);
        values.set("compat." + FORCE_REPLACE_BUILDINGBRICKS_SLABS, replaceLegacy);
        values.set("compat." + FORCE_REPLACE_GRASS_SLABS_MOD_CONTENT, replaceGrassSlabs);
    }

    public static boolean isSmoothingActive() {
        return generateGrassSlabs() && !compatibilitySuppressed
                && GrassSlabsApi.ownerAllowsGeneration();
    }

    public static void suppressSmoothingForThisRun() {
        compatibilitySuppressed = true;
    }

    static void resetRunStateForTests() {
        compatibilitySuppressed = false;
        settingsActivated = false;
    }

    private static final class Common {
        private final ModConfigSpec.BooleanValue forceReplaceBuildingBricksSlabs;
        private final ModConfigSpec.BooleanValue forceReplaceGrassSlabsModContent;
        private final ModConfigSpec.BooleanValue generateGrassSlabs;

        private Common(ModConfigSpec.Builder builder) {
            builder.comment("Legacy world compatibility settings.").push("compat");
            forceReplaceBuildingBricksSlabs = builder.comment(
                    "Replace supported historical grass and dirt slabs when their original mod "
                            + "is installed. A restart is required.")
                    .worldRestart().define(FORCE_REPLACE_BUILDINGBRICKS_SLABS, false);
            forceReplaceGrassSlabsModContent = builder.comment(
                    "Replace supported Grass Slabs, Carpets & Stairs content when that mod is "
                            + "installed. A restart is required.")
                    .worldRestart().define(FORCE_REPLACE_GRASS_SLABS_MOD_CONTENT, false);
            builder.pop();
            builder.comment("World generation settings.").push("worldgen");
            generateGrassSlabs = builder.comment(
                    "Place grass slabs on suitable one block slopes in newly generated "
                            + "Overworld chunks. A restart is required.")
                    .worldRestart().define(GENERATE_GRASS_SLABS, true);
            builder.pop();
        }
    }

    private SkysGrassSlabsConfig() {
    }
}
