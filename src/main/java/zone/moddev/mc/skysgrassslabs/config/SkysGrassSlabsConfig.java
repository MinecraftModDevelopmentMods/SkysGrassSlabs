package zone.moddev.mc.skysgrassslabs.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;

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
    private static volatile boolean compatibilitySuppressed;

    static {
        Pair<Common, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SPEC, FILE_NAME);
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
                    + "built-in defaults will be used", exception);
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
        return COMMON.generateGrassSlabs.get();
    }

    public static boolean forceReplaceBuildingBricksSlabs() {
        return COMMON.forceReplaceBuildingBricksSlabs.get();
    }

    public static boolean forceReplaceGrassSlabsModContent() {
        return COMMON.forceReplaceGrassSlabsModContent.get();
    }

    public static boolean isSmoothingActive() {
        return generateGrassSlabs() && !compatibilitySuppressed;
    }

    public static void suppressSmoothingForThisRun() {
        compatibilitySuppressed = true;
    }

    static void resetRunStateForTests() {
        compatibilitySuppressed = false;
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
