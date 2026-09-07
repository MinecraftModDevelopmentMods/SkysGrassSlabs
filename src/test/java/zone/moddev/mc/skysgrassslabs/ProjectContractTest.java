package zone.moddev.mc.skysgrassslabs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProjectContractTest {
    @Test
    void releaseIdentityAndToolchainArePinned() throws Exception {
        String properties = read("gradle.properties");
        assertTrue(properties.contains("minecraft_version=1.17.1"));
        assertTrue(properties.contains("forge_version=37.1.1"));
        assertTrue(properties.contains("mapping_channel=official"));
        assertTrue(properties.contains("mapping_version=1.17.1"));
        assertTrue(properties.contains("mcp_version=20210706.113038"));
        assertTrue(properties.contains("mod_version=1.0.1.117011"));
        assertTrue(properties.contains("curseforge_project_id=1677588"));
        assertTrue(properties.contains("java_toolchain_version=16.0.2+7"));
        String build = read("build.gradle");
        assertTrue(build.contains("'Built-On-Java': '16'"));
        assertTrue(build.contains("major != 60"));
    }

    @Test
    void saveFacingIdsConfigurationAndWorldStateRemainStable() throws Exception {
        String blocks = read("src/main/java/zone/moddev/mc/skysgrassslabs/init/ModBlocks.java");
        for (String id : Arrays.asList("dirt_slab", "grass_slab", "path_slab", "turf")) {
            assertTrue(blocks.contains("\"" + id + "\""), id);
        }
        String config = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/config/SkysGrassSlabsConfig.java");
        assertTrue(config.contains("generateGrassSlabs"));
        assertTrue(config.contains("forceReplaceBuildingBricksSlabs"));
        assertTrue(config.contains("skysgrassslabs-common.toml"));
        String state = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/world/ModWorldState.java");
        assertTrue(state.contains("skysgrassslabs_world_state"));
        assertTrue(state.contains("SCHEMA_VERSION = 1"));
        assertTrue(state.contains("MIGRATION_VERSION = 1"));
    }

    @Test
    void forgeThirtySevenMetadataAndPackFormatArePresent() throws Exception {
        assertTrue(new File("src/main/resources/META-INF/mods.toml").isFile());
        assertFalse(new File("src/main/resources/mcmod.info").exists());
        assertTrue(read("src/main/resources/META-INF/mods.toml")
                .contains("modId=\"skysgrassslabs\""));
        assertTrue(read("src/main/resources/pack.mcmeta").contains("\"pack_format\": 7"));
    }

    @Test
    void recipesUseDataPackIdsAndARealSerializer() throws Exception {
        List<String> recipes = Arrays.asList("dirt_slab", "grass_slab",
                "grass_block_from_seeds", "grass_slab_from_seeds", "turf");
        for (String recipe : recipes) {
            JsonObject json = json("src/main/resources/data/skysgrassslabs/recipes/" +
                    recipe + ".json");
            assertTrue(json.has("type"), recipe);
            assertTrue(new File("src/main/resources/data/skysgrassslabs/advancements/recipes/" +
                    recipe + ".json").isFile(), recipe);
        }
        assertEquals("skysgrassslabs:turf_cutting", json(
                "src/main/resources/data/skysgrassslabs/recipes/turf.json")
                .get("type").getAsString());
        String serializer = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/recipe/TurfCuttingRecipe.java");
        assertTrue(serializer.contains("implements CraftingRecipe"));
        assertTrue(serializer.contains("RecipeSerializer<TurfCuttingRecipe>"));
        assertTrue(serializer.contains("new ResourceLocation(SkysGrassSlabs.MOD_ID, \"turf_cutting\")"));
        assertTrue(serializer.contains("public boolean isSpecial()"));
        assertTrue(serializer.contains("return false;"));
        assertTrue(serializer.contains("RecipeType.CRAFTING"));
        assertTrue(serializer.contains("public NonNullList<Ingredient> getIngredients()"));
        assertTrue(serializer.contains("canPerformAction(ToolActions.SHOVEL_FLATTEN)"));

        JsonObject seeds = json("src/main/resources/data/forge/tags/items/seeds.json");
        assertEquals(4, seeds.getAsJsonArray("values").size());
    }

    @Test
    void nativeStateModelsCoverWaterSnowAndBothOrientations() throws Exception {
        for (String name : Arrays.asList("dirt_slab", "grass_slab")) {
            String states = read("src/main/resources/assets/skysgrassslabs/blockstates/" +
                    name + ".json");
            assertTrue(states.contains("snowy=false,type=bottom,waterlogged=false"));
            assertTrue(states.contains("snowy=true,type=top,waterlogged=true"));
            assertTrue(states.contains("type=double"));
            assertTrue(states.contains("skysgrassslabs:block/"));
        }
        String path = read(
                "src/main/resources/assets/skysgrassslabs/blockstates/path_slab.json");
        assertTrue(path.contains("type=bottom,waterlogged=false"));
        assertTrue(path.contains("type=top,waterlogged=true"));
        assertTrue(path.contains("minecraft:block/dirt_path"));

        for (String modelName : Arrays.asList("path_slab.json", "path_slab_top.json")) {
            String model = read("src/main/resources/assets/skysgrassslabs/models/block/" +
                    modelName);
            assertTrue(model.contains("minecraft:block/dirt_path_top"), modelName);
            assertTrue(model.contains("minecraft:block/dirt_path_side"), modelName);
            assertFalse(model.contains("minecraft:block/grass_path"), modelName);
        }

        for (String name : Arrays.asList("dirt_slab_snow.json", "dirt_slab_top_snow.json",
                "grass_slab_snow.json", "grass_slab_top_snow.json")) {
            String model = read("src/main/resources/assets/skysgrassslabs/models/block/" + name);
            assertTrue(model.contains("\"top\": \"minecraft:block/snow\""), name);
            assertTrue(model.contains("\"side\": \"minecraft:block/grass_block_snow\""), name);
            assertFalse(upFace(model).contains("tintindex"), name);
        }
    }

    @Test
    void legacyFlatteningHookIsStrictlyScopedAndPreservesTheSnapshot() throws Exception {
        String coremod = read("src/main/resources/coremods/skysgrassslabs_legacy_world.js");
        assertTrue(coremod.contains("net.minecraft.util.datafix.fixes.BlockStateData"));
        assertTrue(coremod.contains("net.minecraft.world.level.chunk.storage.ChunkStorage"));
        assertEquals(2, occurrences(coremod, "'type': 'CLASS'"));
        assertTrue(coremod.contains("65536"));
        assertTrue(coremod.contains("4096"));
        assertTrue(coremod.contains("throw new Error"));
        String bridge = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/compat/LegacyWorldDataHook.java");
        assertTrue(bridge.contains("skysgrassslabs_legacy_registry.dat"));
        assertTrue(bridge.contains("BlockStateData"));
        assertTrue(bridge.contains("indexLegacyChunks"));
        assertTrue(bridge.contains("SUPPORTED_BUILDINGBRICKS_IDS"));
        assertTrue(bridge.contains("WorldPersistenceHooks.addHook"));
        assertFalse(bridge.contains("import sun.misc"));
        assertFalse(bridge.contains("Unsafe.class"));
        assertFalse(bridge.contains("Field.class.getDeclaredField(\"modifiers\")"));
        assertTrue(coremod.contains("return new LdcInsnNode(size)"));
    }

    @Test
    void renamedVanillaPathAndSoundIdsAreRecoveredOnUpgrade() throws Exception {
        String handler = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/compat/LegacyMigrationHandler.java");
        assertTrue(handler.contains("new ResourceLocation(\"minecraft\", \"grass_path\")"));
        assertTrue(handler.contains("new ResourceLocation(\"minecraft\", \"dirt_path\")"));
        assertTrue(handler.contains("item.sweet_berries.pick_from_bush"));
        assertTrue(handler.contains("block.sweet_berry_bush.pick_berries"));
        String subscriber = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/compat/MissingMappingHandler.java");
        assertTrue(subscriber.contains("Bus.FORGE"));
        assertTrue(subscriber.contains("remapMissingBlocks"));
        assertTrue(subscriber.contains("remapMissingItems"));
        assertTrue(subscriber.contains("remapMissingSounds"));
    }

    @Test
    void eclipseClasspathCannotContainSealedLwjglTwoArtifacts() throws Exception {
        String build = read("build.gradle");
        assertFalse(build.contains("org.lwjgl.lwjgl:lwjgl"));
        assertTrue(build.contains("sealed LWJGL 2 artifacts"));
        assertTrue(build.contains("synchronizationTasks 'isolateEclipseProductionRuns'"));
    }

    @Test
    void clientHandlersUseForgeThirtySevenModBusAndRenderLayers() throws Exception {
        String events = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/init/ClientRegistryEvents.java");
        assertTrue(events.contains("bus = Mod.EventBusSubscriber.Bus.MOD"));
        assertFalse(events.contains("bus = Mod.EventBusSubscriber.Bus.FORGE"));
        assertTrue(events.contains("ColorHandlerEvent.Block"));
        assertTrue(events.contains("ColorHandlerEvent.Item"));
        assertTrue(events.contains("FMLClientSetupEvent"));
        String client = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/proxy/ClientProxy.java");
        assertTrue(client.contains("ItemBlockRenderTypes.setRenderLayer(ModBlocks.GRASS_SLAB"));
        assertTrue(client.contains("ItemBlockRenderTypes.setRenderLayer(ModBlocks.TURF"));
        assertTrue(client.contains("RenderType.cutoutMipped()"));
    }

    @Test
    void pathTextureAlignmentAndFenceSafeTurfRemainAccepted() throws Exception {
        for (String name : Arrays.asList("path_slab.json", "path_slab_top.json")) {
            String model = read("src/main/resources/assets/skysgrassslabs/models/block/" + name);
            for (String face : Arrays.asList("north", "south", "west", "east")) {
                assertTrue(model.contains("\"" + face + "\": { \"uv\": [0, 1, 16, 8]"));
            }
        }
        String turf = read("src/main/java/zone/moddev/mc/skysgrassslabs/block/TurfBlock.java");
        assertTrue(turf.contains("noOcclusion()"));
        assertTrue(turf.contains("public void onPlace"));
        String spread = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/block/GrassSpread.java");
        assertTrue(spread.contains("cover.getBlock() == ModBlocks.TURF"));
    }

    @Test
    void shovelFlatteningUsesTheForgeThirtySevenNativeHook() throws Exception {
        String slab = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/block/LegacySlabBlock.java");
        assertTrue(slab.contains("getToolModifiedState"));
        assertTrue(slab.contains("ToolActions.SHOVEL_FLATTEN"));
        assertTrue(slab.contains("stack.canPerformAction(action)"));
        assertFalse(slab.contains("RightClickBlock"));
        String events = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/event/CommonEvents.java");
        assertFalse(events.contains("RightClickBlock"));
        JsonObject shovelTag = json(
                "src/main/resources/data/minecraft/tags/blocks/mineable/shovel.json");
        assertFalse(shovelTag.get("replace").getAsBoolean());
        assertEquals(3, shovelTag.getAsJsonArray("values").size());
    }

    @Test
    void smoothingUsesTheForgeFeatureRegistryAndOwningChunk() throws Exception {
        String registry = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/init/ModRegistryEvents.java");
        assertTrue(registry.contains("RegistryEvent.Register<Feature<?>>"));
        assertTrue(registry.contains("register(GrassSlabSmoothingFeature.FEATURE)"));
        String feature = read(
                "src/main/java/zone/moddev/mc/skysgrassslabs/world/GrassSlabSmoothingFeature.java");
        assertTrue(feature.contains("extends Feature<NoneFeatureConfiguration>"));
        assertTrue(feature.contains("\"grass_slab_smoothing\""));
        assertTrue(feature.contains("region.getCenter()"));
        assertTrue(feature.contains("BuiltinRegistries.CONFIGURED_FEATURE"));
        assertTrue(feature.contains("features.add(0, () -> configuredFeature)"));
        assertTrue(feature.contains("world.hasChunk(chunkX, chunkZ)"));
        assertFalse(new File(
                "src/main/java/zone/moddev/mc/skysgrassslabs/world/ContextFeature.java").exists());
    }

    @Test
    void blockLootTablesPreserveTheDropContract() throws Exception {
        String root = "src/main/resources/data/skysgrassslabs/loot_tables/blocks/";
        for (String block : Arrays.asList("dirt_slab", "grass_slab", "path_slab", "turf")) {
            JsonObject table = json(root + block + ".json");
            assertEquals("minecraft:block", table.get("type").getAsString(), block);
            assertTrue(read(root + block + ".json").contains("minecraft:survives_explosion"),
                    block);
        }
        assertTrue(read(root + "dirt_slab.json").contains("skysgrassslabs:dirt_slab"));
        assertTrue(read(root + "turf.json").contains("skysgrassslabs:turf"));
        assertTrue(read(root + "path_slab.json").contains("skysgrassslabs:dirt_slab"));
        String grass = read(root + "grass_slab.json");
        assertTrue(grass.contains("minecraft:match_tool"));
        assertTrue(grass.contains("minecraft:silk_touch"));
        assertTrue(grass.contains("skysgrassslabs:grass_slab"));
        assertTrue(grass.contains("skysgrassslabs:dirt_slab"));
    }

    @Test
    void adjacentForwardFixtureIsLockedToTheAcceptedOneSixteenJar() throws Exception {
        String manifest = read(
                "src/test/resources/fixtures/skysgrassslabs-1.16.5-forward-world.manifest");
        assertTrue(manifest.contains("fixture_sha256="));
        assertTrue(manifest.contains(
                "source_jar_sha256=54388D358581579723EFBEF6B06DDEF775CEB6FF8DD9289DFDDE82B951A9A2D7"));
        String build = read("build.gradle");
        assertTrue(build.contains("oneSixteenForwardUpgradeTest"));
        assertTrue(build.contains("upgrade-116-first"));
        assertTrue(build.contains("upgrade-116-reload"));
    }

    @Test
    void hiddenLegacyAliasesHaveSafeFallbackModels() throws Exception {
        for (String path : Arrays.asList(
                "assets/buildingbricks/blockstates/grass_slab.json",
                "assets/buildingbricks/blockstates/dirt_slab.json",
                "assets/buildingbrickscompatvanilla/blockstates/grass_slab.json",
                "assets/buildingbricks/models/item/grass_slab.json",
                "assets/buildingbricks/models/item/dirt_slab.json",
                "assets/buildingbrickscompatvanilla/models/item/grass_slab.json")) {
            assertTrue(new File("src/main/resources", path).isFile(), path);
        }
    }

    @Test
    void continuousIntegrationTargetsThisVersionBranch() throws Exception {
        for (String name : Arrays.asList("ci.yml", "codeql-analysis.yml",
                "validate-gradle-build.yml")) {
            String workflow = read(".github/workflows/" + name);
            assertTrue(workflow.contains("master-1.17.1"), name);
            assertFalse(workflow.contains("master-1.16.5"), name);
        }
        String ci = read(".github/workflows/ci.yml");
        assertTrue(ci.contains("SkysGrassSlabs-1.0.1.117011.jar"));
        assertTrue(ci.contains("SkysGrassSlabs-1.0.1.117011-sources.jar"));
        assertTrue(ci.contains("SkysGrassSlabs-1.0.1.117011-javadoc.jar"));
        assertTrue(ci.contains("if-no-files-found: error"));
        assertTrue(ci.contains("java-version: '8.0.502+7'"));
        assertTrue(ci.contains("JAVA_HOME_8_X64"));
        assertTrue(ci.contains("java-version: '25.0.3+9.0.LTS'"));
        assertTrue(ci.contains("JAVA_HOME_25_X64"));
        String codeql = read(".github/workflows/codeql-analysis.yml");
        assertTrue(codeql.contains("java-version: '8.0.502+7'"));
        assertTrue(codeql.contains("JAVA_HOME_8_X64"));
        assertTrue(codeql.contains("java-version: '25.0.3+9.0.LTS'"));
        assertTrue(codeql.contains("JAVA_HOME_25_X64"));
    }

    @Test
    void localContextIsIgnoredAndUntracked() throws Exception {
        String ignore = read(".gitignore");
        for (String path : Arrays.asList("AGENTS.md", "agent-notes/", ".codex/", ".claude/")) {
            assertTrue(ignore.contains(path), path);
        }
        Process process = new ProcessBuilder("git", "ls-files", "-z")
                .redirectErrorStream(true).start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = process.getInputStream()) {
            byte[] buffer = new byte[4096];
            for (int read = input.read(buffer); read >= 0; read = input.read(buffer)) {
                if (read > 0) output.write(buffer, 0, read);
            }
        }
        assertEquals(0, process.waitFor(), output.toString(StandardCharsets.UTF_8.name()));
        for (String item : new String(output.toByteArray(), StandardCharsets.UTF_8).split("\u0000")) {
            String lower = ("/" + item.replace('\\', '/')).toLowerCase(Locale.ROOT);
            assertFalse(lower.endsWith("/agents.md") || lower.contains("/agent-notes/") ||
                    lower.contains("/.codex/") || lower.contains("/.claude/"), item);
        }
    }

    @Test
    void noLegacyLanguageOrRecipeLocationsRemain() throws Exception {
        try (Stream<Path> paths = Files.walk(new File("src/main/resources").toPath())) {
            assertFalse(paths.filter(Files::isRegularFile)
                    .anyMatch(path -> path.toString().endsWith(".lang")));
        }
        assertFalse(new File("src/main/resources/assets/skysgrassslabs/recipes").exists());
        File oldAdvancements = new File("src/main/resources/assets/skysgrassslabs/advancements");
        if (oldAdvancements.isDirectory()) {
            try (Stream<Path> paths = Files.walk(oldAdvancements.toPath())) {
                assertFalse(paths.anyMatch(Files::isRegularFile));
            }
        }
    }

    private static JsonObject json(String path) throws Exception {
        return new JsonParser().parse(read(path)).getAsJsonObject();
    }

    private static String upFace(String model) {
        int start = model.indexOf("\"up\"");
        int end = model.indexOf('\n', start);
        return model.substring(start, end < 0 ? model.length() : end);
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        for (int index = value.indexOf(needle); index >= 0;
                index = value.indexOf(needle, index + needle.length())) {
            ++count;
        }
        return count;
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }
}
