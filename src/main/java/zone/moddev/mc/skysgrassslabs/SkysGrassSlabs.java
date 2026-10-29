package zone.moddev.mc.skysgrassslabs;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.compat.LegacyMigrationHandler;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.event.CommonEvents;
import zone.moddev.mc.skysgrassslabs.proxy.CommonProxy;
import zone.moddev.mc.skysgrassslabs.world.GrassSlabSmoothingHandler;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

@Mod(
        modid = SkysGrassSlabs.MOD_ID,
        name = SkysGrassSlabs.NAME,
        version = SkysGrassSlabs.VERSION,
        dependencies = "before:buildingbricks",
        guiFactory = "zone.moddev.mc.skysgrassslabs.client.GrassSlabsGuiFactory")
public final class SkysGrassSlabs {
    public static final String MOD_ID = "skysgrassslabs";
    public static final String NAME = "Sky's Grass Slabs";
    public static final String VERSION = "1.1.1.112021";

    @SidedProxy(
            clientSide = "zone.moddev.mc.skysgrassslabs.proxy.ClientProxy",
            serverSide = "zone.moddev.mc.skysgrassslabs.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        SkysGrassSlabsConfig.load(event.getSuggestedConfigurationFile());
        BuildingBricksCompat.preInit(event.getModConfigurationDirectory());

        MinecraftForge.EVENT_BUS.register(new CommonEvents());

        LegacyMigrationHandler migration = new LegacyMigrationHandler();

        MinecraftForge.EVENT_BUS.register(migration);
        FMLCommonHandler.instance().bus().register(migration);

        if (!GrassSlabsApi.hasSmoothingOwner() && SkysGrassSlabsConfig.isSmoothingActive()) {
            MinecraftForge.EVENT_BUS.register(new GrassSlabSmoothingHandler());
        }

        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        for (int metadata = 0; metadata < 2; ++metadata) {
            GrassSlabsApi.registerGrassForm(ModBlocks.DIRT_SLAB.getStateFromMeta(metadata),
                    ModBlocks.GRASS_SLAB.getStateFromMeta(metadata));
        }

        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        BuildingBricksCompat.resolveBlocks();
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        GrassSlabsApi.freeze();
    }

    public SkysGrassSlabs() {
    }
}
