package zone.moddev.mc.skysgrassslabs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProjectContractTest {
    @Test
    void releaseIdentityAndToolchainArePinned() throws Exception {
        String properties = read("gradle.properties");
        assertTrue(properties.contains("minecraft_version=1.12.2"));
        assertTrue(properties.contains("forge_version=14.23.5.2859"));
        assertTrue(properties.contains("mapping_version=39-1.12"));
        assertTrue(properties.contains("mod_version=1.0.1.112021"));
        assertTrue(properties.contains("curseforge_project_id=1677588"));
        assertTrue(properties.contains("java_toolchain_version=8.0.502+7"));
    }

    @Test
    void saveFacingIdsAndLegacyRecipeKeyRemainStable() throws Exception {
        String blocks = read("src/main/java/zone/moddev/mc/skysgrassslabs/init/ModBlocks.java");
        String compatibilityRecipe = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/compat/BuildingBricksDirtSlabRecipe.java");
        String config = read("src/main/java/zone/moddev/mc/skysgrassslabs/config/SkysGrassSlabsConfig.java");
        String worldState = read("src/main/java/zone/moddev/mc/skysgrassslabs/world/ModWorldState.java");
        String recipes = read("src/main/java/zone/moddev/mc/skysgrassslabs/init/ModRecipes.java");
        String registryEvents = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/init/ModRegistryEvents.java");
        for (String id : new String[] {"dirt_slab", "grass_slab", "path_slab", "turf"}) {
            assertTrue(blocks.contains("\"" + id + "\""));
        }
        assertTrue(worldState.contains("skysgrassslabs_world_state"));
        assertTrue(worldState.contains("SCHEMA_VERSION = 1"));
        assertTrue(recipes.contains("skysgrassslabs:turf_cutting"));
        for (String id : new String[] {"dirt_slab", "grass_slab",
                "grass_block_from_seeds", "grass_slab_from_seeds", "turf"}) {
            assertTrue(recipes.contains("\"" + id + "\""));
        }
        assertTrue(registryEvents.contains("RegistryEvent.Register<Block>"));
        assertTrue(registryEvents.contains("RegistryEvent.Register<Item>"));
        assertTrue(registryEvents.contains("RegistryEvent.Register<IRecipe>"));
        assertTrue(config.contains("forceReplaceBuildingBricksSlabs"));
        assertTrue(config.contains("COMPAT_CATEGORY, false"));
        assertTrue(compatibilityRecipe.contains("BuildingBricksCompat.isDirtSlabItem"));
    }

    @Test
    void turfRecipeIsDiscoverableThroughTheRecipeBook() throws Exception {
        String recipe = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/recipe/TurfCuttingRecipe.java");
        String advancement = read(
                "src/main/resources/assets/skysgrassslabs/advancements/recipes/turf.json");
        assertTrue(recipe.contains("public boolean isDynamic()"));
        assertTrue(recipe.contains("return false;"));
        assertTrue(recipe.contains("public NonNullList<Ingredient> getIngredients()"));
        assertTrue(advancement.contains("\"skysgrassslabs:turf\""));
        assertTrue(advancement.contains("\"minecraft:grass\""));
        assertTrue(advancement.contains("\"skysgrassslabs:grass_slab\""));
        assertTrue(advancement.contains("\"minecraft:recipe_unlocked\""));
        assertTrue(advancement.contains("\"minecraft:inventory_changed\""));
    }

    @Test
    void oneTwelveResourcesAndLegacyRecoveryUseTheTargetNativeContracts() throws Exception {
        String pack = read("src/main/resources/pack.mcmeta");
        String compatibility = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/compat/BuildingBricksCompat.java");
        String migration = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/compat/LegacyMigrationHandler.java");
        String client = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/proxy/ClientProxy.java");
        String clientRegistry = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/init/ClientRegistryEvents.java");
        assertTrue(pack.contains("\"pack_format\": 3"));
        assertTrue(compatibility.contains("legacyAlias(GRASS_SLAB_ID, true)"));
        assertTrue(compatibility.contains("legacyAlias(DIRT_SLAB_ID, false)"));
        assertTrue(compatibility.contains(
                "legacyAlias(HISTORICAL_GRASS_SLAB_ID, true)"));
        assertTrue(migration.contains("RegistryEvent.MissingMappings<Block>"));
        assertTrue(migration.contains("RegistryEvent.MissingMappings<Item>"));
        assertTrue(migration.contains("event.getAllMappings()"));
        assertTrue(migration.contains("BuildingBricksCompat.hasLegacyAliases()"));
        assertTrue(migration.contains("migrateBlocks(chunk, event.getData(), null)"));
        assertTrue(client.contains("ModelLoader.setCustomStateMapper(block"));
        assertTrue(clientRegistry.contains("ModelRegistryEvent"));
        assertTrue(clientRegistry.contains("ClientProxy.registerModels()"));
        assertTrue(client.contains("BuildingBricksCompat.historicalGrassSlab()"));
    }

    @Test
    void localContextIsIgnoredAndModernDataResourcesAreAbsent() throws Exception {
        String ignore = read(".gitignore");
        assertTrue(ignore.contains("AGENTS.md"));
        assertTrue(ignore.contains("agent-notes/"));
        assertTrue(ignore.contains(".codex/"));
        assertTrue(ignore.contains(".claude/"));
        assertFalse(new File("src/main/resources/META-INF/mods.toml").exists());
        File modernData = new File("src/main/resources/data");
        if (modernData.isDirectory()) {
            try (Stream<Path> paths = Files.walk(modernData.toPath())) {
                assertFalse(paths.anyMatch(Files::isRegularFile));
            }
        }
    }

    @Test
    void releaseTagWorkflowPointsAtTheManualDispatcherInputs() throws Exception {
        String workflow = read(".github/workflows/release-on-tag.yml");
        assertTrue(workflow.contains("release_version:"));
        assertTrue(workflow.contains("curseforge_release_level:"));
        assertTrue(workflow.contains("confirm_live_publication:"));
        assertFalse(workflow.contains("release_ref:"));
        assertFalse(workflow.contains("curseforge_channel:"));
        assertFalse(workflow.contains("confirm_version:"));
        assertTrue(new File("docs/RELEASE-1.0.1.112021.md").isFile());
    }

    @Test
    void codeQlCannotReuseCachedCompilationOutput() throws Exception {
        String workflow = read(".github/workflows/codeql-analysis.yml");
        assertTrue(workflow.contains("clean classes --rerun-tasks --no-daemon"));
    }

    @Test
    void releaseArchivesNormalizeLicenceLineEndings() throws Exception {
        String build = read("build.gradle");
        assertTrue(build.contains("'**/LICENSE', '**/LICENSE.spdx', '**/NOTICE'"));
    }

    @Test
    void localContextIsNotTracked() throws Exception {
        Process process = new ProcessBuilder("git", "ls-files", "-z")
                .redirectErrorStream(true).start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        InputStream input = process.getInputStream();
        byte[] buffer = new byte[4096];
        for (int read = input.read(buffer); read >= 0; read = input.read(buffer)) {
            if (read > 0) output.write(buffer, 0, read);
        }
        assertTrue(process.waitFor() == 0, output.toString(StandardCharsets.UTF_8.name()));

        String[] tracked = new String(output.toByteArray(), StandardCharsets.UTF_8).split("\u0000");
        for (String path : tracked) {
            String lower = ("/" + path.replace('\\', '/')).toLowerCase(Locale.ROOT);
            assertFalse(lower.endsWith("/agent.md") || lower.endsWith("/agents.md") ||
                    lower.endsWith("/claude.md") || lower.contains("/agent-notes/") ||
                    lower.contains("/agent_notes/") || lower.contains("/.codex/") ||
                    lower.contains("/.claude/") || lower.contains("/.cursor/"),
                    "Local agent context is tracked: " + path);
        }
    }

    @Test
    void blockModelsUseLegacySlabParents() throws Exception {
        Path models = new File("src/main/resources/assets/skysgrassslabs/models/block").toPath();
        try (Stream<Path> paths = Files.walk(models)) {
            for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {
                String model = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                assertFalse(model.contains("\"parent\": \"block/slab\"") ||
                        model.contains("\"parent\": \"block/slab_top\""),
                        "Modern slab model parent in " + path);
            }
        }
    }

    @Test
    void snowCapsUseUntintedSnowTopsAndSnowEdgedDirtSides() throws Exception {
        String dirtState = read("src/main/resources/assets/skysgrassslabs/blockstates/dirt_slab.json");
        String grassState = read("src/main/resources/assets/skysgrassslabs/blockstates/grass_slab.json");
        assertTrue(dirtState.contains("half=bottom,snowy=true"));
        assertTrue(dirtState.contains("half=top,snowy=true"));
        assertTrue(grassState.contains("half=bottom,snowy=true"));
        assertTrue(grassState.contains("half=top,snowy=true"));

        for (String name : new String[] {"dirt_slab_snow.json", "dirt_slab_top_snow.json"}) {
            String model = read("src/main/resources/assets/skysgrassslabs/models/block/" + name);
            assertTrue(model.contains("\"top\": \"blocks/snow\""));
            assertTrue(model.contains("\"side\": \"blocks/grass_side_snowed\""));
            assertFalse(upFace(model).contains("tintindex"));
        }
        for (String name : new String[] {"grass_slab_snow.json", "grass_slab_top_snow.json"}) {
            String model = read("src/main/resources/assets/skysgrassslabs/models/block/" + name);
            assertTrue(model.contains("\"top\": \"blocks/snow\""));
            assertTrue(model.contains("\"side\": \"blocks/grass_side_snowed\""));
            assertFalse(upFace(model).contains("tintindex"));
        }
    }

    @Test
    void pathSlabSidesKeepTheVanillaPathSurfaceLip() throws Exception {
        String bottom = read(
                "src/main/resources/assets/skysgrassslabs/models/block/path_slab.json");
        String top = read(
                "src/main/resources/assets/skysgrassslabs/models/block/path_slab_top.json");
        for (String face : new String[] {"north", "south", "west", "east"}) {
            assertTrue(bottom.contains("\"" + face + "\": { \"uv\": [0, 1, 16, 8]"));
            assertTrue(top.contains("\"" + face + "\": { \"uv\": [0, 1, 16, 8]"));
        }
    }

    @Test
    void turfEatingIsAnAdditiveDuplicateSafeVanillaStyleTask() throws Exception {
        String events = read("src/main/java/zone/moddev/mc/skysgrassslabs/event/CommonEvents.java");
        String task = read("src/main/java/zone/moddev/mc/skysgrassslabs/entity/ai/TurfEatingAI.java");
        assertTrue(events.contains("instanceof TurfEatingAI"));
        assertTrue(events.contains("addTask(5, new TurfEatingAI(sheep))"));
        assertTrue(task.contains("setMutexBits(7)"));
        assertTrue(task.contains("sheep.isChild() ? 50 : 1000"));
        assertTrue(task.contains("setEntityState(sheep, (byte) 10)"));
        assertTrue(task.contains("getBoolean(\"mobGriefing\")"));
        assertTrue(task.contains("destroyBlock(pos, false)"));
        assertTrue(task.contains("sheep.eatGrassBonus()"));
    }

    private static String upFace(String model) {
        int start = model.indexOf("\"up\"");
        int end = model.indexOf('\n', start);
        return model.substring(start, end < 0 ? model.length() : end);
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }
}
