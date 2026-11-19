package zone.moddev.mc.skysgrassslabs.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.electronwill.nightconfig.core.CommentedConfig;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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
    private static final ForgeConfigSpec SPEC;
    private static CommentedConfig loadedValues;
    private static volatile boolean compatibilitySuppressed;
    private static boolean settingsActivated;
    private static boolean activeGrassGeneration;
    private static boolean activeLegacyReplacement;
    private static boolean activeGrassSlabsReplacement;

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SPEC, FILE_NAME);
        ModConfigEvent.Loading.getBus(context.getModBusGroup()).addListener(event -> {
            if (event.getConfig().getSpec() == SPEC) {
                loadedValues = event.getConfig().getConfigData();
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
                    + "Forge defaults will be used", exception);
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
        return settingsActivated ? activeGrassGeneration : configuredGrassGeneration();
    }

    public static boolean forceReplaceBuildingBricksSlabs() {
        return settingsActivated ? activeLegacyReplacement : configuredLegacyReplacement();
    }

    public static boolean forceReplaceGrassSlabsModContent() {
        return settingsActivated ? activeGrassSlabsReplacement : configuredGrassSlabsReplacement();
    }

    public static boolean configuredGrassSlabsReplacement() {
        return COMMON.forceReplaceGrassSlabsModContent.get();
    }

    /** Keep a running world on its startup settings when the Config screen saves new choices. */
    public static void activateSettingsForThisRun() {
        activeGrassGeneration = configuredGrassGeneration();
        activeLegacyReplacement = configuredLegacyReplacement();
        activeGrassSlabsReplacement = configuredGrassSlabsReplacement();
        settingsActivated = true;
    }

    public static boolean configuredGrassGeneration() {
        return COMMON.generateGrassSlabs.get();
    }

    public static boolean configuredLegacyReplacement() {
        return COMMON.forceReplaceBuildingBricksSlabs.get();
    }

    public static void saveForNextStart(boolean generateGrass, boolean replaceLegacy) {
        saveForNextStart(generateGrass, replaceLegacy, configuredGrassSlabsReplacement());
    }

    public static void saveForNextStart(boolean generateGrass, boolean replaceLegacy,
            boolean replaceGrassSlabs) {
        if (loadedValues == null) {
            throw new IllegalStateException("Grass Slabs settings have not loaded yet");
        }
        // Forge autosaves each edit. Publish the complete choices together so
        // its file watcher cannot reload between the three setting changes.
        CommentedConfig pending = CommentedConfig.copy(loadedValues);
        CommentedConfig worldgen = CommentedConfig.copy(loadedValues.get("worldgen"));
        CommentedConfig compatibility = CommentedConfig.copy(loadedValues.get("compat"));
        worldgen.set(GENERATE_GRASS_SLABS, generateGrass);
        compatibility.set(FORCE_REPLACE_BUILDINGBRICKS_SLABS, replaceLegacy);
        compatibility.set(FORCE_REPLACE_GRASS_SLABS_MOD_CONTENT, replaceGrassSlabs);
        pending.set("worldgen", worldgen);
        pending.set("compat", compatibility);
        loadedValues.putAll(pending);
        // Refresh the screen's choices while retaining the active session's
        // startup settings until Minecraft is restarted.
        SPEC.afterReload();
        SPEC.save();
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
        private final ForgeConfigSpec.BooleanValue forceReplaceBuildingBricksSlabs;
        private final ForgeConfigSpec.BooleanValue forceReplaceGrassSlabsModContent;
        private final ForgeConfigSpec.BooleanValue generateGrassSlabs;

        private Common(ForgeConfigSpec.Builder builder) {
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
