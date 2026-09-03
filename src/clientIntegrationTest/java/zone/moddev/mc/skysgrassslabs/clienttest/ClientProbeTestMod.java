package zone.moddev.mc.skysgrassslabs.clienttest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.recipebook.RecipeList;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.TickEvent;
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
                    if (minecraft.currentScreen instanceof GuiMainMenu) {
                        minecraft.launchIntegratedServer(WORLD_DIRECTORY,
                                "Sky's Grass Slabs Client Smoke",
                                new WorldSettings(81726354L, GameType.CREATIVE, false,
                                        false, WorldType.DEFAULT));
                        nextState(1);
                    }
                    break;
                case 1:
                    if (minecraft.world != null && minecraft.player != null
                            && firstWorldFrames >= 8 && stateTicks >= 100) {
                        verifyModels(minecraft);
                        verifyRecipeBook(minecraft);
                        stopIntegratedServer(minecraft);
                        nextState(2);
                    }
                    break;
                case 2:
                    if (minecraft.world == null && !minecraft.isIntegratedServerRunning()
                            && stateTicks >= 20) {
                        minecraft.launchIntegratedServer(WORLD_DIRECTORY,
                                "Sky's Grass Slabs Client Smoke",
                                new WorldSettings(81726354L, GameType.CREATIVE, false,
                                        false, WorldType.DEFAULT));
                        nextState(3);
                    }
                    break;
                case 3:
                    if (minecraft.world != null && minecraft.player != null
                            && reloadWorldFrames >= 8 && stateTicks >= 100) {
                        verifyModels(minecraft);
                        verifyRecipeBook(minecraft);
                        stopIntegratedServer(minecraft);
                        nextState(4);
                    }
                    break;
                case 4:
                    if (minecraft.world == null && !minecraft.isIntegratedServerRunning()
                            && stateTicks >= 20) {
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
        IBlockState[] states = {
                slabState(ModBlocks.DIRT_SLAB, SlabType.TOP),
                slabState(ModBlocks.DIRT_SLAB, SlabType.BOTTOM),
                slabState(ModBlocks.GRASS_SLAB, SlabType.TOP),
                slabState(ModBlocks.GRASS_SLAB, SlabType.BOTTOM),
                slabState(ModBlocks.PATH_SLAB, SlabType.TOP),
                slabState(ModBlocks.PATH_SLAB, SlabType.BOTTOM),
                ((Block) ModBlocks.TURF).getDefaultState()
        };
        IBakedModel missing = minecraft.getBlockRendererDispatcher().getBlockModelShapes()
                .getModelManager().getMissingModel();
        for (IBlockState stateToCheck : states) {
            IBakedModel model = minecraft.getBlockRendererDispatcher().getModelForState(stateToCheck);
            ResourceLocation registryName = stateToCheck.getBlock().getRegistryName();
            if (model == null || model == missing || model.getParticleTexture() == null
                    || "missingno".equals(model.getParticleTexture().getName().getPath())) {
                throw new IllegalStateException("Missing baked model for " + registryName
                        + " state " + stateToCheck);
            }
        }
        modelsVerified = true;
    }

    private static IBlockState slabState(Block block, SlabType type) {
        return block.getDefaultState().with(BlockSlab.TYPE, type)
                .with(BlockSlab.WATERLOGGED, Boolean.FALSE);
    }

    private void verifyRecipeBook(Minecraft minecraft) {
        IRecipe turfRecipe = minecraft.world.getRecipeManager().getRecipe(
                new ResourceLocation("skysgrassslabs", "turf"));
        if (turfRecipe == null || turfRecipe.isDynamic()
                || turfRecipe.getIngredients().size() != 2) {
            throw new IllegalStateException("Turf recipe is not recipe book compatible");
        }
        for (RecipeList recipeList : minecraft.player.getRecipeBook().getRecipes()) {
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
            values.store(output, "Sky's Grass Slabs Forge 1.13.2 client gate");
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
