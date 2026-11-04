package zone.moddev.mc.skysgrassslabs.clienttest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.OptionalLong;
import net.minecraft.util.RandomSource;
import java.util.function.BiFunction;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.TickEvent;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;

/** Build-only rendered-client probe. This class is never included in release artifacts. */
@Mod(ClientProbeTestMod.MOD_ID)
@Mod.EventBusSubscriber(modid = ClientProbeTestMod.MOD_ID, value = Dist.CLIENT)
public final class ClientProbeTestMod {
    public static final String MOD_ID = "skysgrassslabsclienttest";
    private static final String WORLD_DIRECTORY = "client-smoke-world";
    private static volatile ClientProbeTestMod instance;

    private int state;
    private int stateTicks;
    private int firstWorldFrames;
    private int reloadWorldFrames;
    private boolean colorsVerified;
    private boolean modelsVerified;
    private boolean recipeBookVerified;
    private boolean configScreenVerified;
    private int configCheckStage;
    private int configStableTicks;
    private int configWaitTicks;
    private boolean originalConfigChoice;
    private boolean activeConfigChoice;
    private Screen configParent;
    private boolean windowHidden;

    public ClientProbeTestMod() {
        instance = this;
    }

    @SubscribeEvent
    public static void onWorldRendered(RenderLevelStageEvent event) {
        ClientProbeTestMod probe = instance;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER
                || probe == null || !Boolean.getBoolean("skysgrassslabs.clientProbe")) return;
        if (probe.state == 1) ++probe.firstWorldFrames;
        if (probe.state == 3) ++probe.reloadWorldFrames;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        ClientProbeTestMod probe = instance;
        if (probe == null || event.phase != TickEvent.Phase.END
                || !Boolean.getBoolean("skysgrassslabs.clientProbe")) return;
        probe.handleClientTick();
    }

    private void handleClientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!windowHidden) {
            try {
                // GLFW is supplied by the client launcher, outside the common compilation classpath.
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                long window = (Long) glfw.getMethod("glfwGetCurrentContext").invoke(null);
                if (window != 0) {
                    glfw.getMethod("glfwHideWindow", long.class).invoke(null, window);
                    windowHidden = true;
                }
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not hide the automated client's window", exception);
            }
        }
        if (++stateTicks > 3600) {
            fail(minecraft, "Timed out in client probe state " + state + "; screen="
                    + (minecraft.screen == null ? "none" : minecraft.screen.getClass().getName()));
        }
        try {
            switch (state) {
                case 0:
                    if (minecraft.screen instanceof TitleScreen) {
                        createWorld(minecraft);
                        nextState(1);
                    }
                    break;
                case 1:
                    if (minecraft.level != null && minecraft.player != null
                            && firstWorldFrames >= 8 && stateTicks >= 100) {
                        if (configCheckStage == 0) {
                            verifyModels(minecraft);
                            verifyColors(minecraft);
                            verifyRecipeBook(minecraft);
                            verifyCreativeTab(minecraft);
                        }
                        if (verifyConfigScreen(minecraft)) {
                            minecraft.getSingleplayerServer().submit(() ->
                                    minecraft.getSingleplayerServer().overworld().setDayTime(123456L))
                                    .orTimeout(5, java.util.concurrent.TimeUnit.SECONDS).join();
                            stopIntegratedServer(minecraft);
                            nextState(2);
                        }
                    }
                    break;
                case 2:
                    if (minecraft.level == null && !minecraft.hasSingleplayerServer()
                            && stateTicks >= 20) {
                        createWorld(minecraft);
                        nextState(3);
                    }
                    break;
                case 3:
                    acceptPrivateWorldLoadPrompt(minecraft);
                    if (minecraft.level != null && minecraft.player != null
                            && reloadWorldFrames >= 8 && stateTicks >= 100) {
                        long savedTime = minecraft.getSingleplayerServer().submit(() ->
                                minecraft.getSingleplayerServer().overworld().getDayTime())
                                .orTimeout(5, java.util.concurrent.TimeUnit.SECONDS).join();
                        if (savedTime < 123456L || savedTime > 133456L) {
                            throw new IllegalStateException(
                                    "The client did not reopen its saved world: dayTime=" + savedTime);
                        }
                        verifyModels(minecraft);
                        verifyColors(minecraft);
                        verifyRecipeBook(minecraft);
                        verifyCreativeTab(minecraft);
                        stopIntegratedServer(minecraft);
                        nextState(4);
                    }
                    break;
                case 4:
                    if (minecraft.level == null && !minecraft.hasSingleplayerServer()
                            && stateTicks >= 20) {
                        writeMarker();
                        minecraft.stop();
                        nextState(5);
                    }
                    break;
                default:
                    break;
            }
        } catch (RuntimeException | IOException failure) {
            fail(minecraft, failure.toString());
        }
    }

    private void verifyModels(Minecraft minecraft) {
        BlockState[] states = {
                slabState(ModBlocks.DIRT_SLAB, SlabType.TOP),
                slabState(ModBlocks.DIRT_SLAB, SlabType.BOTTOM),
                slabState(ModBlocks.GRASS_SLAB, SlabType.TOP),
                slabState(ModBlocks.GRASS_SLAB, SlabType.BOTTOM),
                slabState(ModBlocks.GRASS_SLAB, SlabType.TOP).setValue(SnowyDirtBlock.SNOWY, true),
                slabState(ModBlocks.GRASS_SLAB, SlabType.BOTTOM).setValue(SnowyDirtBlock.SNOWY, true),
                slabState(ModBlocks.DIRT_SLAB, SlabType.TOP).setValue(SnowyDirtBlock.SNOWY, true),
                slabState(ModBlocks.DIRT_SLAB, SlabType.BOTTOM).setValue(SnowyDirtBlock.SNOWY, true),
                slabState(ModBlocks.PATH_SLAB, SlabType.TOP),
                slabState(ModBlocks.PATH_SLAB, SlabType.BOTTOM),
                ((Block) ModBlocks.TURF).defaultBlockState()
        };
        BakedModel missing = minecraft.getBlockRenderer().getBlockModelShaper()
                .getModelManager().getMissingModel();
        for (BlockState stateToCheck : states) {
            BakedModel model = minecraft.getBlockRenderer().getBlockModel(stateToCheck);
            var renderTypes = model.getRenderTypes(stateToCheck, RandomSource.create(0),
                    net.minecraftforge.client.model.data.ModelData.EMPTY);
            if ((stateToCheck.is(ModBlocks.GRASS_SLAB) || stateToCheck.is(ModBlocks.TURF))
                    && (!renderTypes.contains(net.minecraft.client.renderer.RenderType.cutoutMipped())
                            || renderTypes.contains(net.minecraft.client.renderer.RenderType.solid()))) {
                throw new IllegalStateException(
                        "Grass and turf lost their cutout-mipped render layer: " + stateToCheck);
            }
            ResourceLocation registryName = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(stateToCheck.getBlock());
            if (model == null || model == missing ||
                    model.getParticleIcon() == null ||
                    "missingno".equals(model.getParticleIcon()
                            .contents().name().getPath())) {
                throw new IllegalStateException("Missing baked model for " + registryName
                        + " state " + stateToCheck);
            }
            Direction exposedFace = stateToCheck.getBlock() == ModBlocks.TURF
                    || stateToCheck.getValue(SlabBlock.TYPE) == SlabType.BOTTOM
                    ? Direction.UP : Direction.DOWN;
            if (model.getQuads(stateToCheck, null, RandomSource.create(0)).stream()
                    .noneMatch(quad -> quad.getDirection() == exposedFace)) {
                throw new IllegalStateException("A neighbour can hide the exposed "
                        + exposedFace + " face of " + stateToCheck);
            }
        }
        modelsVerified = true;
    }

    private static BlockState slabState(Block block, SlabType type) {
        return block.defaultBlockState() .setValue(SlabBlock.TYPE, type)
                 .setValue(SlabBlock.WATERLOGGED, Boolean.FALSE);
    }

    private void verifyColors(Minecraft minecraft) {
        BlockPos pos = minecraft.player.blockPosition();
        int expectedBlockColor = BiomeColors.getAverageGrassColor(minecraft.level, pos);
        for (BlockState stateToCheck : new BlockState[] {
                slabState(ModBlocks.GRASS_SLAB, SlabType.TOP),
                slabState(ModBlocks.GRASS_SLAB, SlabType.BOTTOM),
                ((Block) ModBlocks.TURF).defaultBlockState()
        }) {
            int actual = minecraft.getBlockColors().getColor(
                    stateToCheck, minecraft.level, pos, 0);
            if (actual != expectedBlockColor) {
                throw new IllegalStateException("Missing biome grass tint for "
                        + net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(stateToCheck.getBlock()) + ": expected "
                        + expectedBlockColor + " but got " + actual);
            }
        }

        int expectedItemColor = GrassColor.get(0.5D, 1.0D);
        for (Block block : new Block[] {ModBlocks.GRASS_SLAB, ModBlocks.TURF}) {
            int actual = minecraft.getItemColors().getColor(new ItemStack(block), 0);
            if (actual != expectedItemColor) {
                throw new IllegalStateException("Missing inventory grass tint for "
                        + net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block) + ": expected " + expectedItemColor
                        + " but got " + actual);
            }
        }
        colorsVerified = true;
    }

    private void verifyCreativeTab(Minecraft minecraft) {
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(
                minecraft.level.enabledFeatures(), true, minecraft.level.registryAccess());
        var contents = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(
                net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS).getDisplayItems();
        for (Block block : new Block[] {
                ModBlocks.DIRT_SLAB, ModBlocks.GRASS_SLAB, ModBlocks.PATH_SLAB, ModBlocks.TURF}) {
            long matches = contents.stream().filter(stack -> stack.is(block.asItem())).count();
            if (matches != 1) {
                throw new IllegalStateException("Creative tab must contain each playable piece once: "
                        + net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block));
            }
        }
        long skyItems = contents.stream().filter(stack -> "skysgrassslabs".equals(
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace()))
                .count();
        if (skyItems != 4) {
            throw new IllegalStateException("Hidden recovery items appeared in the creative tab");
        }
    }

    private void verifyRecipeBook(Minecraft minecraft) {
        Recipe turfRecipe = minecraft.level.getRecipeManager().byKey(
                new ResourceLocation("skysgrassslabs", "turf")).orElse(null);
        if (!(turfRecipe instanceof CraftingRecipe) || turfRecipe.isSpecial()
                || turfRecipe.getIngredients().size() != 2) {
            throw new IllegalStateException("Turf recipe is not recipe book compatible");
        }
        for (RecipeCollection recipeList : minecraft.player.getRecipeBook().getCollections()) {
            if (recipeList.getRecipes().contains(turfRecipe)) {
                recipeBookVerified = true;
                return;
            }
        }
        throw new IllegalStateException("Turf recipe is absent from the client recipe book");
    }

    private static void stopIntegratedServer(Minecraft minecraft) {
        if (minecraft.level != null) minecraft.level.disconnect();
        minecraft.clearLevel(new TitleScreen());
    }

    private static void createWorld(Minecraft minecraft) {
        java.nio.file.Path save = minecraft.gameDirectory.toPath().resolve("saves")
                .resolve(WORLD_DIRECTORY).resolve("level.dat");
        if (java.nio.file.Files.isRegularFile(save)) {
            minecraft.createWorldOpenFlows().loadLevel(new TitleScreen(), WORLD_DIRECTORY);
            return;
        }
        LevelSettings settings = new LevelSettings("Sky's Grass Slabs Client Smoke",
                GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(),
                net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD_DIRECTORY, settings,
                new net.minecraft.world.level.levelgen.WorldOptions(81726354L, true, false),
                net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions);
    }

    private static void acceptPrivateWorldLoadPrompt(Minecraft minecraft) {
        boolean backup = minecraft.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen;
        boolean confirmation = minecraft.screen != null
                && minecraft.screen.getClass() == net.minecraft.client.gui.screens.ConfirmScreen.class;
        if (!backup && !confirmation) {
            return;
        }
        java.nio.file.Path save = minecraft.gameDirectory.toPath().resolve("saves")
                .resolve(WORLD_DIRECTORY).resolve("level.dat");
        if (!java.nio.file.Files.isRegularFile(save)) {
            throw new IllegalStateException("The load prompt has no saved probe world");
        }
        String experimentalTitle = net.minecraft.network.chat.Component
                .translatable("selectWorld.backupQuestion.experimental").getString();
        if (confirmation && !experimentalTitle.equals(minecraft.screen.getTitle().getString())) {
            throw new IllegalStateException("Unexpected world-load confirmation: "
                    + minecraft.screen.getTitle().getString());
        }
        String proceed = backup
                ? net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString()
                : net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString();
        for (var child : minecraft.screen.children()) {
            if (child instanceof net.minecraft.client.gui.components.Button button
                    && proceed.equals(button.getMessage().getString())) {
                if (button.active) {
                    // Confirm the known load prompt for this generated fixture.
                    button.onPress();
                    System.out.println("GRASS_CLIENT_LOAD_PROMPT_CONFIRMED owned_world=true");
                }
                return;
            }
        }
        throw new IllegalStateException("The expected world-load confirmation button is missing");
    }

    private boolean verifyConfigScreen(Minecraft minecraft) throws IOException {
        BiFunction<Minecraft, Screen, Screen> factory = ModList.get()
                .getModContainerById("skysgrassslabs")
                .orElseThrow(() -> new IllegalStateException("Grass Slabs has no mod container"))
                .getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class)
                .map(ConfigScreenHandler.ConfigScreenFactory::screenFunction)
                .orElseThrow(() -> new IllegalStateException("The Mods Config button has no screen"));
        if (configCheckStage == 1) {
            if (!configChoiceHasSettled(!originalConfigChoice)) {
                return false;
            }
            SkysGrassSlabsConfig.saveForNextStart(originalConfigChoice,
                    SkysGrassSlabsConfig.configuredLegacyReplacement());
            nextConfigStage(2);
            return false;
        }
        if (configCheckStage == 2) {
            if (!configChoiceHasSettled(originalConfigChoice)) {
                return false;
            }
            Screen cancelled = factory.apply(minecraft, configParent);
            minecraft.setScreen(cancelled);
            cancelled.mouseClicked(cancelled.width / 2, 70, 0);
            cancelled.onClose();
            if (SkysGrassSlabsConfig.configuredGrassGeneration() != originalConfigChoice) {
                throw new IllegalStateException("Escape saved an unconfirmed Config choice");
            }
            nextConfigStage(3);
            return false;
        }
        if (configCheckStage == 3) {
            if (!configChoiceHasSettled(originalConfigChoice)) {
                return false;
            }
            configScreenVerified = true;
            return true;
        }

        boolean savedChoice = SkysGrassSlabsConfig.configuredGrassGeneration();
        boolean activeChoice = SkysGrassSlabsConfig.generateGrassSlabs();
        originalConfigChoice = savedChoice;
        activeConfigChoice = activeChoice;
        Screen parent = minecraft.screen;
        configParent = parent;
        Screen screen = factory.apply(minecraft, parent);
        minecraft.setScreen(screen);
        if (screen.children().size() != 4 || !screen.mouseClicked(screen.width / 2, 70, 0)) {
            throw new IllegalStateException("The Config screen's generation control is unavailable");
        }
        if (SkysGrassSlabsConfig.configuredGrassGeneration() != savedChoice) {
            throw new IllegalStateException("A pending GUI choice was saved before Done");
        }
        for (String key : new String[] {"generateGrassSlabs", "forceReplaceBuildingBricksSlabs",
                "forceReplaceGrassSlabsModContent"}) {
            String tooltip = "config.skysgrassslabs." + key + ".tooltip";
            if (I18n.get(tooltip).equals(tooltip) || I18n.get(tooltip).trim().isEmpty()) {
                throw new IllegalStateException("Missing translated Config help: " + tooltip);
            }
        }
        screen.mouseClicked(screen.width / 2, screen.height - 25, 0);
        if (SkysGrassSlabsConfig.configuredGrassGeneration() == savedChoice
                || SkysGrassSlabsConfig.generateGrassSlabs() != activeChoice) {
            throw new IllegalStateException("Config saving did not retain the restart-only policy");
        }

        // Forge watches config files asynchronously. Finish each saved edit before
        // restoring it and testing a separate, unconfirmed edit.
        nextConfigStage(1);
        return false;
    }

    private void nextConfigStage(int stage) {
        configCheckStage = stage;
        configStableTicks = 0;
        configWaitTicks = 0;
    }

    private boolean configChoiceHasSettled(boolean expected) throws IOException {
        if (++configWaitTicks > 200) {
            throw new IllegalStateException("Config file did not settle during stage " + configCheckStage);
        }
        if (SkysGrassSlabsConfig.generateGrassSlabs() != activeConfigChoice) {
            throw new IllegalStateException("A saved Config choice changed the running world's setting");
        }
        String contents = java.nio.file.Files.readString(java.nio.file.Paths.get(
                "config/skysgrassslabs-common.toml"), java.nio.charset.StandardCharsets.UTF_8);
        java.util.regex.Matcher value = java.util.regex.Pattern.compile(
                "(?m)^\\h*generateGrassSlabs\\h*=\\h*(true|false)\\h*$").matcher(contents);
        boolean persisted = value.find() && Boolean.parseBoolean(value.group(1)) == expected && !value.find();
        if (persisted && SkysGrassSlabsConfig.configuredGrassGeneration() == expected) {
            configStableTicks++;
        } else {
            configStableTicks = 0;
        }
        return configStableTicks >= 20;
    }

    private void writeMarker() throws IOException {
        Properties values = new Properties();
        values.setProperty("colors_verified", Boolean.toString(colorsVerified));
        values.setProperty("models_verified", Boolean.toString(modelsVerified));
        values.setProperty("recipe_book_verified", Boolean.toString(recipeBookVerified));
        values.setProperty("config_screen_verified", Boolean.toString(configScreenVerified));
        values.setProperty("first_world_rendered", Boolean.toString(firstWorldFrames >= 8));
        values.setProperty("reload_rendered", Boolean.toString(reloadWorldFrames >= 8));
        values.setProperty("saved_world_reopened", "true");
        values.setProperty("creative_tab_verified", "true");
        values.setProperty("world_directory", WORLD_DIRECTORY);
        try (FileOutputStream output = new FileOutputStream(
                new File("client-smoke-pass.properties"))) {
            values.store(output, "Sky's Grass Slabs client gate");
        }
    }

    private void nextState(int next) {
        state = next;
        stateTicks = 0;
    }

    private static void fail(Minecraft minecraft, String message) {
        try {
            Properties values = new Properties();
            values.setProperty("failure", message);
            try (FileOutputStream output = new FileOutputStream(
                    new File("client-smoke-failure.properties"))) {
                values.store(output, "Sky's Grass Slabs client probe failure");
            }
        } catch (IOException ignored) {
        }
        minecraft.stop();
        throw new IllegalStateException(message);
    }
}
