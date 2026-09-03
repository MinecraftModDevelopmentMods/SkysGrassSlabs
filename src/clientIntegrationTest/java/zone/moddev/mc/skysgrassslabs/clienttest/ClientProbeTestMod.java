package zone.moddev.mc.skysgrassslabs.clienttest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.recipebook.RecipeList;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.util.RecipeBookClient;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Build-only rendered-client probe. This class is never included in release artifacts. */
@Mod(modid = ClientProbeTestMod.MOD_ID, name = "Sky's Grass Slabs Client Probe",
        version = "1", acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:skysgrassslabs")
public final class ClientProbeTestMod {
    public static final String MOD_ID = "skysgrassslabsclienttest";
    private static final String WORLD_DIRECTORY = "client-smoke-world";
    private int state;
    private int stateTicks;
    private int firstWorldFrames;
    private int reloadWorldFrames;
    private boolean modelsVerified;
    private boolean recipeBookVerified;

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        if (Boolean.getBoolean("skysgrassslabs.clientProbe")) {
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    @SubscribeEvent
    public void onWorldRendered(RenderWorldLastEvent event) {
        if (state == 1) ++firstWorldFrames;
        if (state == 3) ++reloadWorldFrames;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END ||
                !Boolean.getBoolean("skysgrassslabs.clientProbe")) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (++stateTicks > 3600) fail(minecraft, "Timed out in client probe state " + state);
        try {
            switch (state) {
                case 0:
                    if (minecraft.currentScreen instanceof GuiMainMenu) {
                        minecraft.launchIntegratedServer(WORLD_DIRECTORY,
                                "Sky's Grass Slabs Client Smoke",
                                new WorldSettings(81726354L, GameType.CREATIVE, false,
                                        false, WorldType.DEFAULT));
                        nextState(1);
                    }
                    break;
                case 1:
                    if (minecraft.world != null && minecraft.player != null &&
                            firstWorldFrames >= 8 && stateTicks >= 100) {
                        verifyModels(minecraft);
                        verifyRecipeBook();
                        stopIntegratedServer(minecraft);
                        nextState(2);
                    }
                    break;
                case 2:
                    if (minecraft.world == null && stateTicks >= 100) {
                        minecraft.launchIntegratedServer(WORLD_DIRECTORY,
                                "Sky's Grass Slabs Client Smoke",
                                new WorldSettings(81726354L, GameType.CREATIVE, false,
                                        false, WorldType.DEFAULT));
                        nextState(3);
                    }
                    break;
                case 3:
                    if (minecraft.world != null && minecraft.player != null &&
                            reloadWorldFrames >= 8 && stateTicks >= 100) {
                        verifyModels(minecraft);
                        verifyRecipeBook();
                        stopIntegratedServer(minecraft);
                        nextState(4);
                    }
                    break;
                case 4:
                    if (minecraft.world == null && stateTicks >= 40) {
                        writeMarker();
                        minecraft.shutdown();
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
        Block dirtSlab = ModBlocks.DIRT_SLAB;
        Block grassSlab = ModBlocks.GRASS_SLAB;
        Block pathSlab = ModBlocks.PATH_SLAB;
        Block turf = ModBlocks.TURF;
        IBlockState[] states = {
                dirtSlab.getDefaultState().withProperty(
                        BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP),
                dirtSlab.getDefaultState().withProperty(
                        BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM),
                grassSlab.getDefaultState().withProperty(
                        BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP),
                grassSlab.getDefaultState().withProperty(
                        BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM),
                pathSlab.getDefaultState().withProperty(
                        BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP),
                pathSlab.getDefaultState().withProperty(
                        BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM),
                turf.getDefaultState()
        };
        IBakedModel missing = minecraft.getBlockRendererDispatcher().getBlockModelShapes()
                .getModelManager().getMissingModel();
        for (IBlockState blockState : states) {
            IBakedModel model = minecraft.getBlockRendererDispatcher().getModelForState(blockState);
            ResourceLocation registryName = blockState.getBlock().getRegistryName();
            if (model == null || model == missing || model.getParticleTexture() == null ||
                    "missingno".equals(model.getParticleTexture().getIconName())) {
                throw new IllegalStateException("Missing baked model for " + registryName +
                        " state " + blockState);
            }
        }
        modelsVerified = true;
    }

    private void verifyRecipeBook() {
        IRecipe turfRecipe = CraftingManager.REGISTRY.getObject(
                new ResourceLocation("skysgrassslabs", "turf"));
        if (turfRecipe == null || turfRecipe.isDynamic() ||
                turfRecipe.getIngredients().size() != 2) {
            throw new IllegalStateException("Turf recipe is not recipe-book compatible");
        }
        for (RecipeList recipeList : RecipeBookClient.ALL_RECIPES) {
            if (recipeList.getRecipes().contains(turfRecipe)) {
                recipeBookVerified = true;
                return;
            }
        }
        throw new IllegalStateException("Turf recipe is absent from the client recipe book");
    }

    private static void stopIntegratedServer(Minecraft minecraft) {
        if (minecraft.world != null) minecraft.world.sendQuittingDisconnectingPacket();
        minecraft.loadWorld(null);
        minecraft.displayGuiScreen(new GuiMainMenu());
    }

    private void writeMarker() throws IOException {
        Properties values = new Properties();
        values.setProperty("models_verified", Boolean.toString(modelsVerified));
        values.setProperty("recipe_book_verified", Boolean.toString(recipeBookVerified));
        values.setProperty("first_world_rendered", Boolean.toString(firstWorldFrames >= 8));
        values.setProperty("reload_rendered", Boolean.toString(reloadWorldFrames >= 8));
        values.setProperty("world_directory", WORLD_DIRECTORY);
        try (FileOutputStream output = new FileOutputStream(
                new File("client-smoke-pass.properties"))) {
            values.store(output, "Sky's Grass Slabs Forge 1.12.2 client gate");
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
        minecraft.shutdown();
        throw new IllegalStateException(message);
    }
}
