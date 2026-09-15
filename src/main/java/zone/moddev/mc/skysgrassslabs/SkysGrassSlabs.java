package zone.moddev.mc.skysgrassslabs;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.compat.GrassSlabsCompat;
import zone.moddev.mc.skysgrassslabs.compat.GrassSlabsMigrationHandler;
import zone.moddev.mc.skysgrassslabs.compat.LegacyMigrationHandler;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.event.CommonEvents;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.init.ModRecipes;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;
import zone.moddev.mc.skysgrassslabs.world.WorldgenBootstrap;

/** NeoForge entry point for the standalone Sky's Grass Slabs mod. */
@Mod(SkysGrassSlabs.MOD_ID)
public final class SkysGrassSlabs {
    /** Stable mod identifier and resource namespace. */
    public static final String MOD_ID = "skysgrassslabs";
    public static final String NAME = "Sky's Grass Slabs";
    public static final String VERSION = "1.1.1.120062";
    public static final Logger LOGGER = LogManager.getLogger();

    /** Registers content, configuration, world generation, and persistent state. */
    public SkysGrassSlabs(IEventBus modBus, ModContainer modContainer) {
        SkysGrassSlabsConfig.migrateLegacyConfig();
        SkysGrassSlabsConfig.register(modContainer);
        ModBlocks.register(modBus);
        ModRecipes.register(modBus);
        WorldgenBootstrap.register(modBus);
        BuildingBricksCompat.register(modBus);
        GrassSlabsCompat.register(modBus);
        LegacyWorldDataHook.register();
        LegacyMigrationHandler.register();
        GrassSlabsMigrationHandler.register();
        CommonEvents.register();
        registerGameTests(modBus);

        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private static void registerGameTests(IEventBus modBus) {
        try {
            Class<?> bootstrap = Class.forName(
                    "zone.moddev.mc.skysgrassslabs.gametest.GameTestBootstrap");
            bootstrap.getMethod("register", IEventBus.class).invoke(null, modBus);
        } catch (ClassNotFoundException exception) {
            String enabledNamespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
            if (Arrays.stream(enabledNamespaces.split(","))
                    .map(String::trim)
                    .anyMatch(MOD_ID::equals)) {
                throw new IllegalStateException("GameTest source set is missing from the development run", exception);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not register Sky's Grass Slabs GameTests", exception);
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        ModWorldState.get(event.getServer().overworld());
    }
}
