package zone.moddev.mc.skysgrassslabs.init;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.proxy.ClientProxy;

@Mod.EventBusSubscriber(modid = SkysGrassSlabs.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientRegistryEvents {
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ClientProxy.registerModels();
    }

    @SubscribeEvent
    public static void registerBlockColors(ColorHandlerEvent.Block event) {
        ClientProxy.registerBlockColors(event.getBlockColors());
    }

    @SubscribeEvent
    public static void registerItemColors(ColorHandlerEvent.Item event) {
        ClientProxy.registerItemColors(event.getItemColors());
    }

    private ClientRegistryEvents() {
    }
}
