package zone.moddev.mc.skysgrassslabs;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.moddev.mc.skysgrassslabs.compat.LegacyMigrationHandler;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.event.CommonEvents;
import zone.moddev.mc.skysgrassslabs.world.GrassSlabSmoothingFeature;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;

@Mod(SkysGrassSlabs.MOD_ID)
public final class SkysGrassSlabs {
    public static final String MOD_ID = "skysgrassslabs";
    public static final String NAME = "Sky's Grass Slabs";
    public static final String VERSION = "1.2.0.120012";
    public static final Logger LOGGER = LogManager.getLogger();

    public SkysGrassSlabs(IEventBus modBus) {
        SkysGrassSlabsConfig.migrateLegacyConfig();
        SkysGrassSlabsConfig.register(modBus);

        modBus.addListener(this::setup);
        modBus.addListener(this::loadComplete);
        zone.moddev.mc.skysgrassslabs.world.WorldgenBootstrap.register(
                modBus);
        CommonEvents.register();
        LegacyMigrationHandler.register();
        zone.moddev.mc.skysgrassslabs.compat.GrassSlabsCompat.register(
                modBus);
        zone.moddev.mc.skysgrassslabs.compat.GrassSlabsMigrationHandler.register();
        DistExecutor.runWhenOn(Dist.CLIENT,
                () -> zone.moddev.mc.skysgrassslabs.proxy.ClientProxy::register);
    }

    private void setup(FMLCommonSetupEvent event) {
        // Forge's shared save-hook list must not be changed by parallel mod constructors.
        event.enqueueWork(LegacyWorldDataHook::register);
        SkysGrassSlabsConfig.activateSettingsForThisRun();
        GrassSlabsApi.configurationLoaded();
        for (SlabType half : new SlabType[] {SlabType.BOTTOM, SlabType.TOP}) {
            for (boolean waterlogged : new boolean[] {false, true}) {
                GrassSlabsApi.registerGrassForm(ModBlocks.DIRT_SLAB.defaultBlockState()
                                .setValue(SlabBlock.TYPE, half).setValue(SlabBlock.WATERLOGGED, waterlogged),
                        ModBlocks.GRASS_SLAB.defaultBlockState()
                                .setValue(SlabBlock.TYPE, half).setValue(SlabBlock.WATERLOGGED, waterlogged));
            }
        }
    }

    private void loadComplete(FMLLoadCompleteEvent event) {
        // Other content mods finish registering their grass forms before this point.
        event.enqueueWork(() -> {
            GrassSlabsApi.freeze();
        });
    }
}
