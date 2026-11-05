package zone.moddev.mc.skysgrassslabs;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;
import zone.moddev.mc.skysgrassslabs.client.ClientConfig;
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
    public static final String VERSION = "1.2.0.121011";
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
        registerGameTests(modBus);

        modBus.addListener(this::setupGrassForms);
        modBus.addListener(this::finishGrassRegistration);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> ClientConfig.register(context));

        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void setupGrassForms(FMLCommonSetupEvent event) {
        SkysGrassSlabsConfig.activateSettingsForThisRun();
        GrassSlabsApi.configurationLoaded();
        for (SlabType half : new SlabType[] {SlabType.BOTTOM, SlabType.TOP}) {
            for (boolean waterlogged : new boolean[] {false, true}) {
                GrassSlabsApi.registerGrassForm(
                        ModBlocks.DIRT_SLAB.get().defaultBlockState()
                                .setValue(SlabBlock.TYPE, half).setValue(SlabBlock.WATERLOGGED, waterlogged),
                        ModBlocks.GRASS_SLAB.get().defaultBlockState()
                                .setValue(SlabBlock.TYPE, half).setValue(SlabBlock.WATERLOGGED, waterlogged));
            }
        }
    }

    private void finishGrassRegistration(FMLLoadCompleteEvent event) {
        event.enqueueWork(GrassSlabsApi::freeze);
    }

    private static void registerGameTests(IEventBus modBus) {
        try {
            Class<?> bootstrap = Class.forName(
                    "zone.moddev.mc.skysgrassslabs.gametest.GameTestBootstrap");
            bootstrap.getMethod("register", IEventBus.class).invoke(null, modBus);
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
