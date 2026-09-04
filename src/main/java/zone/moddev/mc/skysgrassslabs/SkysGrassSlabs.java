package zone.moddev.mc.skysgrassslabs;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.moddev.mc.skysgrassslabs.compat.LegacyMigrationHandler;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.event.CommonEvents;
import zone.moddev.mc.skysgrassslabs.world.GrassSlabSmoothingFeature;

@Mod(SkysGrassSlabs.MOD_ID)
public final class SkysGrassSlabs {
    public static final String MOD_ID = "skysgrassslabs";
    public static final String NAME = "Sky's Grass Slabs";
    public static final String VERSION = "1.0.1.114041";
    public static final Logger LOGGER = LogManager.getLogger();

    public SkysGrassSlabs() {
        SkysGrassSlabsConfig.migrateLegacyConfig();
        SkysGrassSlabsConfig.register();
        LegacyWorldDataHook.register();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        CommonEvents.register();
        LegacyMigrationHandler.register();
        DistExecutor.runWhenOn(Dist.CLIENT,
                () -> zone.moddev.mc.skysgrassslabs.proxy.ClientProxy::register);
    }

    private void setup(FMLCommonSetupEvent event) {
        GrassSlabSmoothingFeature.install();
    }
}
