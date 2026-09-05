package zone.moddev.mc.skysgrassslabs.clienttest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.OptionalLong;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

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

    public ClientProbeTestMod() {
        instance = this;
    }

    @SubscribeEvent
    public static void onWorldRendered(RenderWorldLastEvent event) {
        ClientProbeTestMod probe = instance;
        if (probe == null || !Boolean.getBoolean("skysgrassslabs.clientProbe")) return;
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
        if (++stateTicks > 3600) fail(minecraft, "Timed out in client probe state " + state);
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
                        verifyModels(minecraft);
                        verifyColors(minecraft);
                        verifyRecipeBook(minecraft);
                        stopIntegratedServer(minecraft);
                        nextState(2);
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
                    if (minecraft.level != null && minecraft.player != null
                            && reloadWorldFrames >= 8 && stateTicks >= 100) {
                        verifyModels(minecraft);
                        verifyColors(minecraft);
                        verifyRecipeBook(minecraft);
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
                slabState(ModBlocks.PATH_SLAB, SlabType.TOP),
                slabState(ModBlocks.PATH_SLAB, SlabType.BOTTOM),
                ((Block) ModBlocks.TURF).defaultBlockState()
        };
        BakedModel missing = minecraft.getBlockRenderer().getBlockModelShaper()
                .getModelManager().getMissingModel();
        for (BlockState stateToCheck : states) {
            BakedModel model = minecraft.getBlockRenderer().getBlockModel(stateToCheck);
            ResourceLocation registryName = stateToCheck.getBlock().getRegistryName();
            if (model == null || model == missing ||
                    model.getParticleIcon() == null ||
                    "missingno".equals(model.getParticleIcon()
                            .getName().getPath())) {
                throw new IllegalStateException("Missing baked model for " + registryName
                        + " state " + stateToCheck);
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
                        + stateToCheck.getBlock().getRegistryName() + ": expected "
                        + expectedBlockColor + " but got " + actual);
            }
        }

        int expectedItemColor = GrassColor.get(0.5D, 1.0D);
        for (Block block : new Block[] {ModBlocks.GRASS_SLAB, ModBlocks.TURF}) {
            int actual = minecraft.getItemColors().getColor(new ItemStack(block), 0);
            if (actual != expectedItemColor) {
                throw new IllegalStateException("Missing inventory grass tint for "
                        + block.getRegistryName() + ": expected " + expectedItemColor
                        + " but got " + actual);
            }
        }
        colorsVerified = true;
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
        RegistryAccess.RegistryHolder registries = RegistryAccess.builtin();
        WorldGenSettings generator = WorldGenSettings.makeDefault(
                registries.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY),
                registries.registryOrThrow(Registry.BIOME_REGISTRY),
                registries.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY))
                .withSeed(false, OptionalLong.of(81726354L));
        LevelSettings settings = new LevelSettings("Sky's Grass Slabs Client Smoke",
                GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(),
                DataPackConfig.DEFAULT);
        minecraft.createLevel(WORLD_DIRECTORY, settings, registries, generator);
    }

    private void writeMarker() throws IOException {
        Properties values = new Properties();
        values.setProperty("colors_verified", Boolean.toString(colorsVerified));
        values.setProperty("models_verified", Boolean.toString(modelsVerified));
        values.setProperty("recipe_book_verified", Boolean.toString(recipeBookVerified));
        values.setProperty("first_world_rendered", Boolean.toString(firstWorldFrames >= 8));
        values.setProperty("reload_rendered", Boolean.toString(reloadWorldFrames >= 8));
        values.setProperty("world_directory", WORLD_DIRECTORY);
        try (FileOutputStream output = new FileOutputStream(
                new File("client-smoke-pass.properties"))) {
            values.store(output, "Sky's Grass Slabs Forge 1.17.1 client gate");
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
