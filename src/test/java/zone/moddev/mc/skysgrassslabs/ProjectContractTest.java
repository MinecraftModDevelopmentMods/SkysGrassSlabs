package zone.moddev.mc.skysgrassslabs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class ProjectContractTest {
    @Test
    public void metadataUsesStableIdentityWithoutOreSpawnDependency() throws Exception {
        String metadata = Files.readString(
                Path.of("src/main/resources/META-INF/neoforge.mods.toml"), StandardCharsets.UTF_8);
        String properties = Files.readString(Path.of("gradle.properties"), StandardCharsets.UTF_8);

        assertTrue(properties.contains("mod_id=skysgrassslabs"));
        assertTrue(properties.contains("minecraft_version=1.21.1"));
        assertTrue(properties.contains("neo_version=21.1.247"));
        assertTrue(properties.contains("java_toolchain_version=21.0.7+6"));
        String build = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
        assertTrue(build.contains("verifyJava21Toolchain"));
        assertTrue(build.contains("runtimeVersion == '21.0.7+6'"));
        assertFalse(metadata.contains("modId=\"orespawn\""));
    }

    @Test
    public void onlyTheGameTestServerEnablesTheGameTestNamespace() throws Exception {
        String build = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
        String setting = "systemProperty 'neoforge.enabledGameTestNamespaces', mod_id";

        assertEquals(1, countOccurrences(build, setting));
        int gameTestServer = build.indexOf("gameTestServer {");
        int dataRun = build.indexOf("data {", gameTestServer);
        assertTrue(gameTestServer >= 0);
        assertTrue(dataRun > gameTestServer);
        assertTrue(build.substring(gameTestServer, dataRun).contains(setting));
    }

    @Test
    public void eclipseAndNeoGradleUseOneGradleCache() throws Exception {
        String build = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
        int eclipseTask = build.indexOf("tasks.named('eclipse').configure {");

        assertTrue(eclipseTask >= 0);
        assertTrue(build.contains("preferences.setProperty('override.workspace.settings', 'true')"));
        assertTrue(build.indexOf("doLast writeEclipseBuildshipPreferences", eclipseTask)
                > eclipseTask);
        assertTrue(build.contains("it.name == 'idePostSync'"));
        assertTrue(build.contains("dependsOn 'idePostSync'"));
        assertTrue(build.contains("it.name.startsWith('writeMinecraftClasspath')"));
        assertTrue(build.contains("inputs.property('skysGrassSlabsGradleUserHome')"));
        assertTrue(build.contains("gradle.gradleUserHomeDir.canonicalPath"));
    }

    @Test
    public void playerAndMaintainerDocumentsExist() {
        assertTrue(Files.isRegularFile(Path.of("docs/CONFIGURATION.md")));
        assertTrue(Files.isRegularFile(Path.of("docs/GAMEPLAY.md")));
        assertTrue(Files.isRegularFile(Path.of("docs/WORLD-UPGRADES.md")));
        assertFalse(Files.exists(Path.of("docs/REPOSITORY.md")));
        assertTrue(Files.isRegularFile(Path.of("docs/RELEASE-1.1.0.121012.md")));
        assertTrue(Files.isRegularFile(Path.of("docs/BETA-0.2.0.118021.md")));
    }

    @Test
    public void releaseIdentityAndLicenseAreStable() throws Exception {
        String properties = Files.readString(Path.of("gradle.properties"), StandardCharsets.UTF_8);
        assertTrue(properties.contains("mod_version=1.1.0.121012"));
        assertTrue(properties.contains("mod_license=LGPL-2.1-only"));
        assertEquals("LGPL-2.1-only", Files.readString(Path.of("LICENSE.spdx"), StandardCharsets.UTF_8).trim());
        assertTrue(Files.readString(Path.of("NOTICE"), StandardCharsets.UTF_8)
                .contains("Copyright (C) 2026 SkyBlade1978"));
    }

    @Test
    public void releaseDispatcherIsExplicitAndUsesTheImmutableBundle() throws Exception {
        String properties = Files.readString(Path.of("gradle.properties"), StandardCharsets.UTF_8);
        String workflow = Files.readString(
                Path.of(".github/workflows/deploy-release.yml"), StandardCharsets.UTF_8);
        String build = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
        assertTrue(build.contains("server-port=0"));

        assertTrue(properties.contains("loader_name=neoforge"));
        assertTrue(properties.contains("loader_code=2"));
        assertTrue(properties.contains("curseforge_project_id=1677588"));
        assertTrue(workflow.contains("confirm_live_publication:"));
        assertFalse(workflow.contains("    environment:\n      name: release"));
        assertTrue(workflow.contains("CURSEFORGE_TOKEN: ${{ secrets.CURSEFORGE_TOKEN }}"));
        assertTrue(workflow.contains("MAVEN_UPLOAD_URL: ${{ secrets.MAVEN_UPLOAD_URL }}"));
        assertTrue(workflow.contains("MAVEN_UPLOAD_USERNAME: ${{ secrets.MAVEN_UPLOAD_USERNAME }}"));
        assertTrue(workflow.contains("MAVEN_UPLOAD_PASSWORD: ${{ secrets.MAVEN_UPLOAD_PASSWORD }}"));
        assertTrue(workflow.contains("-PpreparedReleaseDir="));
        assertTrue(workflow.contains("MinecraftModDevelopmentMods/SkysGrassSlabs"));
        assertTrue(workflow.indexOf("  publish_maven:") <
                workflow.indexOf("  publish_curseforge:"));
        assertTrue(workflow.indexOf("  publish_curseforge:") <
                workflow.indexOf("  publish_github:"));
        assertTrue(build.contains("if (preparedReleaseDir.isPresent())"));
    }

    @Test
    public void codeQlCannotReuseCachedCompilationOutput() throws Exception {
        String workflow = Files.readString(
                Path.of(".github/workflows/codeql-analysis.yml"), StandardCharsets.UTF_8);
        assertTrue(workflow.contains("clean classes --rerun-tasks --no-daemon"));
    }


    @Test
    public void commonConfigAndWorldStateUsePermanentKeys() throws Exception {
        String config = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/config/SkysGrassSlabsConfig.java"),
                StandardCharsets.UTF_8);
        String main = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/SkysGrassSlabs.java"),
                StandardCharsets.UTF_8);
        String state = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/world/ModWorldState.java"),
                StandardCharsets.UTF_8);
        assertTrue(config.contains("push(\"worldgen\")"));
        assertTrue(config.contains("define(GENERATE_GRASS_SLABS, true)"));
        assertTrue(config.contains("push(\"compat\")"));
        assertTrue(config.contains("define(FORCE_REPLACE_BUILDINGBRICKS_SLABS, false)"));
        assertTrue(config.contains("define(FORCE_REPLACE_GRASS_SLABS_MOD_CONTENT, false)"));
        assertTrue(main.contains("VERSION = \"1.1.0.121012\""));
        assertTrue(state.contains("skysgrassslabs_world_state"));
        assertTrue(state.contains("SCHEMA_VERSION = 1"));
        assertTrue(state.contains("schema_version"));
    }

    @Test
    public void allSaveFacingResourcesExist() {
        String[] paths = {
                "src/main/resources/assets/skysgrassslabs/blockstates/dirt_slab.json",
                "src/main/resources/assets/skysgrassslabs/blockstates/grass_slab.json",
                "src/main/resources/assets/skysgrassslabs/blockstates/path_slab.json",
                "src/main/resources/assets/skysgrassslabs/blockstates/turf.json",
                "src/main/resources/assets/skysgrassslabs/models/block/turf.json",
                "src/main/resources/assets/skysgrassslabs/models/item/turf.json",
                "src/main/resources/data/skysgrassslabs/recipe/dirt_slab.json",
                "src/main/resources/data/skysgrassslabs/recipe/grass_slab.json",
                "src/main/resources/data/skysgrassslabs/recipe/grass_block_from_seeds.json",
                "src/main/resources/data/skysgrassslabs/recipe/grass_slab_from_seeds.json",
                "src/main/resources/data/skysgrassslabs/recipe/turf.json",
                "src/main/resources/data/skysgrassslabs/loot_table/blocks/dirt_slab.json",
                "src/main/resources/data/skysgrassslabs/loot_table/blocks/grass_slab.json",
                "src/main/resources/data/skysgrassslabs/loot_table/blocks/path_slab.json",
                "src/main/resources/data/skysgrassslabs/loot_table/blocks/turf.json"
        };
        for (String path : paths) {
            assertTrue(path, Files.isRegularFile(Path.of(path)));
        }
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/assets/grassslabs/blockstates/grass_slab.json")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/assets/grassslabs/blockstates/dirt_slab.json")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/assets/grassslabs/blockstates/dirt_path_slab.json")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/assets/grassslabs/blockstates/grass_carpet.json")));
    }

    @Test
    public void legacyUpgradeHooksAreNarrowAndAvoidUnsafe() throws Exception {
        String coremod = Files.readString(Path.of(
                "src/main/resources/coremods/skysgrassslabs_legacy_world.js"),
                StandardCharsets.UTF_8);
        String bridge = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/compat/LegacyWorldDataHook.java"),
                StandardCharsets.UTF_8);
        assertTrue(coremod.contains("net.minecraft.util.datafix.fixes.BlockStateData"));
        assertTrue(coremod.contains("net.neoforged.neoforge.common.CommonHooks"));
        assertTrue(coremod.contains("net.minecraft.world.level.chunk.storage.ChunkStorage"));
        assertTrue(coremod.contains("65536"));
        assertTrue(coremod.contains("4096"));
        assertTrue(coremod.contains("current !== vanilla && current !== target"));
        assertTrue(coremod.contains("throw new Error"));
        assertTrue(bridge.contains("skysgrassslabs_legacy_registry.dat"));
        assertTrue(bridge.contains("indexLegacyChunks"));
        assertTrue(bridge.contains("SUPPORTED_BLOCK_IDS"));
        assertFalse(bridge.contains("import sun.misc"));
        assertFalse(bridge.contains("Unsafe.class"));
        assertFalse(bridge.contains("Field.class.getDeclaredField(\"modifiers\")"));
    }

    @Test
    public void acceptedRenderingAndRecipeContractsRemainVisible() throws Exception {
        String turf = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/block/TurfBlock.java"),
                StandardCharsets.UTF_8);
        String recipe = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/recipe/TurfCuttingRecipe.java"),
                StandardCharsets.UTF_8);
        String client = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/client/ClientEvents.java"),
                StandardCharsets.UTF_8);
        assertTrue(turf.contains("extends CarpetBlock"));
        assertTrue(recipe.contains("extends CustomRecipe"));
        assertTrue(recipe.contains("CraftingBookCategory"));
        assertTrue(recipe.contains("HolderLookup.Provider"));
        assertTrue(recipe.contains("return false;"));
        assertTrue(recipe.contains("canPerformAction(ItemAbilities.SHOVEL_FLATTEN)"));
        assertFalse(client.contains("ItemBlockRenderTypes"));
        for (String modelName : new String[] {"grass_slab", "grass_slab_top",
                "grass_slab_snow", "grass_slab_top_snow"}) {
            String model = Files.readString(Path.of(
                    "src/main/resources/assets/skysgrassslabs/models/block/"
                            + modelName + ".json"), StandardCharsets.UTF_8);
            assertTrue(modelName, model.contains("\"render_type\": \"cutout_mipped\""));
        }
    }

    @Test
    public void neoForge21LifecycleAndDataPackContractsArePresent() throws Exception {
        String blocks = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/init/ModBlocks.java"),
                StandardCharsets.UTF_8);
        String worldgen = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/world/WorldgenBootstrap.java"),
                StandardCharsets.UTF_8);
        String modifier = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/world/SmoothingBiomeModifier.java"),
                StandardCharsets.UTF_8);
        String modifierJson = Files.readString(Path.of(
                "src/main/resources/data/skysgrassslabs/neoforge/biome_modifier/"
                        + "grass_slab_smoothing.json"), StandardCharsets.UTF_8);
        String pack = Files.readString(Path.of("src/main/resources/pack.mcmeta"),
                StandardCharsets.UTF_8);
        assertTrue(blocks.contains("BuildCreativeModeTabContentsEvent"));
        assertTrue(blocks.contains("event.getTabKey()"));
        assertTrue(worldgen.contains("NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS"));
        assertTrue(worldgen.contains("FEATURE_NAME = \"grass_slab_smoothing\""));
        assertTrue(modifier.contains("phase != Phase.AFTER_EVERYTHING"));
        assertTrue(modifier.contains("features.add(0, smoothing)"));
        assertTrue(modifier.contains("BiomeTags.IS_NETHER"));
        assertTrue(modifier.contains("BiomeTags.IS_END"));
        assertTrue(modifierJson.contains("skysgrassslabs:grass_slab_smoothing"));
        assertTrue(worldgen.contains("DeferredRegister<MapCodec<? extends BiomeModifier>>"));
        assertTrue(pack.contains("\"pack_format\": 34"));
        assertTrue(pack.contains("\"min_inclusive\": 34"));
        assertTrue(pack.contains("\"max_inclusive\": 48"));

        for (String recipeName : new String[] {"dirt_slab", "grass_slab",
                "grass_block_from_seeds", "grass_slab_from_seeds", "turf"}) {
            String recipe = Files.readString(Path.of(
                    "src/main/resources/data/skysgrassslabs/recipe/" + recipeName + ".json"),
                    StandardCharsets.UTF_8);
            assertTrue(recipeName, recipe.contains("\"category\": \"building\""));
            if (!recipeName.equals("turf")) {
                assertTrue(recipeName, recipe.contains("\"id\":"));
                assertFalse(recipeName, recipe.contains("\"result\": { \"item\":"));
            }
        }
    }

    @Test
    public void continuousIntegrationTargetsTheStableBranchAndArtifacts() throws Exception {
        String attributes = Files.readString(Path.of(".gitattributes"),
                StandardCharsets.UTF_8);
        assertTrue(attributes.contains("* text=auto eol=lf"));
        assertTrue(attributes.contains("*.patch text eol=lf -diff"));
        assertTrue(attributes.contains("*.txt text eol=lf"));

        for (String name : new String[] {"ci.yml", "codeql-analysis.yml",
                "validate-gradle-build.yml"}) {
            String workflow = Files.readString(Path.of(".github/workflows", name),
                    StandardCharsets.UTF_8);
            assertTrue(name, workflow.contains("master-1.21.1-neo"));
        }
        String ci = Files.readString(Path.of(".github/workflows/ci.yml"),
                StandardCharsets.UTF_8);
        assertTrue(ci.contains("SkysGrassSlabs-1.1.0.121012.jar"));
        assertTrue(ci.contains("SkysGrassSlabs-1.1.0.121012-sources.jar"));
        assertTrue(ci.contains("SkysGrassSlabs-1.1.0.121012-javadoc.jar"));
        assertTrue(ci.contains("if-no-files-found: error"));
        assertTrue(ci.contains("java-version: '21.0.7+6.0.LTS'"));
        assertTrue(ci.contains("--offline --no-daemon"));
        assertTrue(ci.contains("runGameTestServer"));
        assertTrue(ci.contains("skysGrassSlabsGameTestRunDirectory=build/game-test-run"));
        assertTrue(Files.isRegularFile(Path.of(".github/workflows/release-on-tag.yml")));
    }

    @Test
    public void adjacentUpgradeFixtureIsTracked() {
        assertTrue(Files.isRegularFile(Path.of("src/test/resources/fixtures/"
                + "skysgrassslabs-1.20.6-forward-world.zip")));
        assertTrue(Files.isRegularFile(Path.of("src/test/resources/fixtures/"
                + "skysgrassslabs-1.20.6-forward-world.manifest")));
        assertTrue(Files.isRegularFile(Path.of("src/test/resources/fixtures/"
                + "grassslabs-1.18.2-migration-world.zip")));
        assertTrue(Files.isRegularFile(Path.of("src/test/resources/fixtures/"
                + "grassslabs-1.18.2-migration-world.manifest")));
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = text.indexOf(needle, fromIndex)) >= 0) {
            count++;
            fromIndex += needle.length();
        }
        return count;
    }
}
