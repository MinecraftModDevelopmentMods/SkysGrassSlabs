package zone.moddev.mc.skysgrassslabs;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.compat.GrassSlabsCompat;
import zone.moddev.mc.skysgrassslabs.compat.GrassSlabsMigrationHandler;
import zone.moddev.mc.skysgrassslabs.compat.LegacyMigrationHandler;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;
import zone.moddev.mc.skysgrassslabs.compat.MissingMappingHandler;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.event.CommonEvents;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.init.ModRecipes;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;
import zone.moddev.mc.skysgrassslabs.world.WorldgenBootstrap;

/** Forge entry point for the standalone Sky's Grass Slabs mod. */
@Mod(SkysGrassSlabs.MOD_ID)
public final class SkysGrassSlabs {
    /** Stable Forge mod identifier and resource namespace. */
    public static final String MOD_ID = "skysgrassslabs";
    public static final String NAME = "Sky's Grass Slabs";
    public static final String VERSION = "1.1.0.2602001";
    public static final Logger LOGGER = LogManager.getLogger();

    /** Registers content, configuration, world generation, and persistent state. */
    public SkysGrassSlabs(FMLJavaModLoadingContext context) {
        BusGroup modBusGroup = context.getModBusGroup();

        SkysGrassSlabsConfig.migrateLegacyConfig();
        SkysGrassSlabsConfig.register(context);
        ModBlocks.register(modBusGroup);
        ModRecipes.register(modBusGroup);
        WorldgenBootstrap.register(modBusGroup);
        BuildingBricksCompat.register(modBusGroup);
        GrassSlabsCompat.register(modBusGroup);
        LegacyWorldDataHook.register();
        LegacyMigrationHandler.register();
        GrassSlabsMigrationHandler.register();
        CommonEvents.register();
        registerGameTests(modBusGroup);
        ServerStartedEvent.BUS.addListener(this::onServerStarted);
        MissingMappingHandler.register();
    }

    private static void registerGameTests(BusGroup modBusGroup) {
        try {
            Class<?> bootstrap = Class.forName(
                    "zone.moddev.mc.skysgrassslabs.gametest.GameTestBootstrap");
            bootstrap.getMethod("register", BusGroup.class).invoke(null, modBusGroup);
        } catch (ClassNotFoundException exception) {
            String enabledNamespaces = System.getProperty("forge.enabledGameTestNamespaces", "");
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
