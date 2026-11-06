package zone.moddev.mc.skysgrassslabs.upgradeprobe;

import java.nio.file.Files;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import org.lwjgl.sdl.SDLVideo;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Checks the real client without putting the test code in the release jar. */
final class ClientRuntimeProbe {
    private static final String WORLD = "qualification";
    private final String phase = System.getProperty("skysgrassslabs.clientProbePhase");
    private boolean windowHidden;
    private boolean finished;
    private boolean confirmedExperimentalWorld;
    private int worldTicks;

    static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(new ClientRuntimeProbe()::tick);
    }

    private void tick(TickEvent.ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (!windowHidden) {
            SDLVideo.SDL_HideWindow(client.getWindow().handle());
            windowHidden = true;
        }
        if (finished) {
            return;
        }
        if (!Files.isRegularFile(client.gameDirectory.toPath().resolve("saves").resolve(WORLD).resolve("level.dat"))) {
            throw new IllegalStateException("The disposable client fixture is missing");
        }
        confirmExperimentalWorld(client.gui.screen());
        if (client.level == null || client.player == null || ++worldTicks < 20) {
            return;
        }
        checkTint(client);
        checkConfig(client);
        finished = true;
        SkysGrassSlabs.LOGGER.info("GRASS_SLABS_CLIENT_WORLD_PASS phase={}", phase);
        client.stop();
    }

    private void confirmExperimentalWorld(Screen screen) {
        if (confirmedExperimentalWorld || screen == null || !(screen.getTitle().getContents() instanceof TranslatableContents title)
                || !title.getKey().equals("selectWorld.backupQuestion.experimental")) {
            return;
        }
        Component proceed;
        if (screen instanceof BackupConfirmScreen) {
            proceed = Component.translatable("selectWorld.backupJoinSkipButton");
        } else if (screen instanceof ConfirmScreen) {
            proceed = CommonComponents.GUI_PROCEED;
        } else {
            return;
        }
        Button button = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(child -> child.getMessage().equals(proceed)).findFirst().orElseThrow();
        // Opening the world can tick the client again before this callback returns.
        confirmedExperimentalWorld = true;
        button.onPress(null);
    }

    private static void checkTint(Minecraft client) {
        var position = client.player.blockPosition();
        int grass = tint(client, position, Blocks.GRASS_BLOCK.defaultBlockState());
        int slab = tint(client, position, ModBlocks.GRASS_SLAB.get().defaultBlockState());
        int turf = tint(client, position, ModBlocks.TURF.get().defaultBlockState());
        require(grass != -1 && slab == grass && turf == grass, "Slab and turf should use the biome's grass colour");
    }

    private static int tint(Minecraft client, BlockPos position,
            BlockState state) {
        var source = client.getBlockColors().getTintSource(state, 0);
        if (source == null) {
            throw new IllegalStateException("No grass tint source for " + state);
        }
        return source.colorInWorld(state, client.level, position);
    }

    private void checkConfig(Minecraft client) {
        require("fresh".equals(phase) || "reload".equals(phase), "Unexpected client test phase");
        boolean first = phase.equals("fresh");
        require(SkysGrassSlabsConfig.generateGrassSlabs() == first, "The saved generation setting was not loaded");
        Screen parent = client.gui.screen();
        var mod = ModList.getModContainerById(SkysGrassSlabs.MOD_ID).orElseThrow().getModInfo();
        Screen config = ConfigScreenHandler.getScreenFactoryFor(mod).orElseThrow().apply(client, parent);
        client.gui.setScreen(config);
        List<Button> buttons = config.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
        require(buttons.size() == 4, "The settings screen should have three controls and a Done button");
        for (String setting : List.of("generateGrassSlabs", "forceReplaceBuildingBricksSlabs",
                "forceReplaceGrassSlabsModContent")) {
            String tooltip = "config.skysgrassslabs." + setting + ".tooltip";
            require(!I18n.get(tooltip).equals(tooltip), "Missing setting tooltip: " + setting);
        }
        if (first) {
            buttons.getFirst().onPress(null);
            buttons.getLast().onPress(null);
            require(!SkysGrassSlabsConfig.configuredGrassGeneration(), "Done should save the new generation setting");
            require(SkysGrassSlabsConfig.generateGrassSlabs(), "Changing the setting should require a restart");
        } else {
            config.onClose();
        }
        SkysGrassSlabs.LOGGER.info("GRASS_SLABS_CONFIG_SCREEN_PASS phase={}", phase);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
