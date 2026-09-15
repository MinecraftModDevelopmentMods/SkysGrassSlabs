package zone.moddev.mc.skysgrassslabs;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

/** Forge entry point for the standalone Sky's Grass Slabs mod. */
@Mod(SkysGrassSlabs.MOD_ID)
public final class SkysGrassSlabs {
    /** Stable Forge mod identifier and resource namespace. */
    public static final String MOD_ID = "skysgrassslabs";
    public static final String NAME = "Sky's Grass Slabs";
    public static final String VERSION = "1.1.1.120011";
    public static final Logger LOGGER = LogManager.getLogger();

    /** Registers content, configuration, world generation, and persistent state. */
    public SkysGrassSlabs(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        SkysGrassSlabsConfig.migrateLegacyConfig();
        SkysGrassSlabsConfig.register(context);
        ModBlocks.register(modBus);
        ModRecipes.register(modBus);
        WorldgenBootstrap.register(modBus);
        BuildingBricksCompat.register(modBus);
        GrassSlabsCompat.register(modBus);
        LegacyWorldDataHook.register();
        LegacyMigrationHandler.register();
        GrassSlabsMigrationHandler.register();
        CommonEvents.register();

        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void onServerStarted(ServerStartedEvent event) {
        ModWorldState.get(event.getServer().overworld());
    }
}
