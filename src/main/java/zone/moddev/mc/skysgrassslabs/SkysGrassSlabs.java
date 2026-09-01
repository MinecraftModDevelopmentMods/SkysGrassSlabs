package zone.moddev.mc.skysgrassslabs;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import zone.moddev.mc.skysgrassslabs.config.BetaConfig;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.init.ModRecipes;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;
import zone.moddev.mc.skysgrassslabs.world.WorldgenBootstrap;

/** Forge entry point for the standalone Sky's Grass Slabs mod. */
@Mod(SkysGrassSlabs.MOD_ID)
public final class SkysGrassSlabs {
    /** Stable Forge mod identifier and resource namespace. */
    public static final String MOD_ID = "skysgrassslabs";

    /** Registers content, configuration, world generation, and persistent state. */
    public SkysGrassSlabs() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModRecipes.register(modBus);
        WorldgenBootstrap.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BetaConfig.SPEC);

        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,
                WorldgenBootstrap::onBiomeLoading);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void onServerStarted(ServerStartedEvent event) {
        ModWorldState.get(event.getServer().overworld());
    }
}
