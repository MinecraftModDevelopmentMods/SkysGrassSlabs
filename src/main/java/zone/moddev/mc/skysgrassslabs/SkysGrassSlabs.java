package zone.moddev.mc.skysgrassslabs;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.compat.LegacyMigrationHandler;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.event.CommonEvents;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.init.ModRecipes;
import zone.moddev.mc.skysgrassslabs.proxy.CommonProxy;
import zone.moddev.mc.skysgrassslabs.world.GrassSlabSmoothingHandler;

@Mod(modid = SkysGrassSlabs.MOD_ID, name = SkysGrassSlabs.NAME,
        version = SkysGrassSlabs.VERSION, dependencies = "before:buildingbricks")
public final class SkysGrassSlabs {
    public static final String MOD_ID = "skysgrassslabs";
    public static final String NAME = "Sky's Grass Slabs";
    public static final String VERSION = "1.0.1.111021";

    @SidedProxy(
            clientSide = "zone.moddev.mc.skysgrassslabs.proxy.ClientProxy",
            serverSide = "zone.moddev.mc.skysgrassslabs.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        SkysGrassSlabsConfig.load(event.getSuggestedConfigurationFile());
        ModBlocks.register();
        BuildingBricksCompat.preInit(event.getModConfigurationDirectory());

        MinecraftForge.EVENT_BUS.register(new CommonEvents());
        LegacyMigrationHandler migration = new LegacyMigrationHandler();
        MinecraftForge.EVENT_BUS.register(migration);
        FMLCommonHandler.instance().bus().register(migration);
        if (SkysGrassSlabsConfig.isSmoothingActive()) {
            MinecraftForge.EVENT_BUS.register(new GrassSlabSmoothingHandler());
        }
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModRecipes.register();
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        BuildingBricksCompat.resolveBlocks();
    }

    @Mod.EventHandler
    public void missingMappings(FMLMissingMappingsEvent event) {
        LegacyMigrationHandler.remapMissingMappings(event);
    }

    public SkysGrassSlabs() {
    }
}
