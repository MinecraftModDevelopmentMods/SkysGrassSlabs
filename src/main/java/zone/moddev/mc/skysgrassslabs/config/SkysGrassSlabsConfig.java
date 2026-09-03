package zone.moddev.mc.skysgrassslabs.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;

public final class SkysGrassSlabsConfig {
    public static final String FORCE_REPLACE_BUILDINGBRICKS_SLABS =
            "forceReplaceBuildingBricksSlabs";
    public static final String GENERATE_GRASS_SLABS = "generateGrassSlabs";
    public static final String FILE_NAME = "skysgrassslabs-common.toml";

    private static final Common COMMON;
    private static final ForgeConfigSpec SPEC;
    private static volatile boolean compatibilitySuppressed;

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, FILE_NAME);
    }

    public static void migrateLegacyConfig() {
        migrateLegacyConfig(FMLPaths.CONFIGDIR.get());
    }

    public static boolean migrateLegacyConfig(Path configDirectory) {
        Path oldFile = configDirectory.resolve("skysgrassslabs.cfg");
        Path newFile = configDirectory.resolve(FILE_NAME);
        if (Files.exists(newFile) || !Files.isRegularFile(oldFile)) {
            return false;
        }
        boolean forceReplace = readLegacyBoolean(oldFile,
                FORCE_REPLACE_BUILDINGBRICKS_SLABS, false);
        boolean worldgen = readLegacyBoolean(oldFile, GENERATE_GRASS_SLABS, true);
        List<String> toml = Arrays.asList(
                "[compat]",
                FORCE_REPLACE_BUILDINGBRICKS_SLABS + " = " + forceReplace,
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
            SkysGrassSlabs.LOGGER.warn("Could not migrate the legacy Sky's Grass Slabs config; " +
                    "Forge defaults will be used", exception);
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
                if ("true".equalsIgnoreCase(parsed)) return true;
                if ("false".equalsIgnoreCase(parsed)) return false;
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
        final ForgeConfigSpec.BooleanValue forceReplaceBuildingBricksSlabs;
        final ForgeConfigSpec.BooleanValue generateGrassSlabs;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Legacy world compatibility settings.").push("compat");
            forceReplaceBuildingBricksSlabs = builder.comment(
                    "Retained for compatibility with earlier versions. No supported 1.13 " +
                    "BuildingBricks release is modified by this setting.")
                    .worldRestart().define(FORCE_REPLACE_BUILDINGBRICKS_SLABS, false);
            builder.pop();
            builder.comment("World generation settings.").push("worldgen");
            generateGrassSlabs = builder.comment(
                    "Place grass slabs on suitable one block slopes in newly generated Overworld chunks.")
                    .worldRestart().define(GENERATE_GRASS_SLABS, true);
            builder.pop();
        }
    }

    private SkysGrassSlabsConfig() {
    }
}
