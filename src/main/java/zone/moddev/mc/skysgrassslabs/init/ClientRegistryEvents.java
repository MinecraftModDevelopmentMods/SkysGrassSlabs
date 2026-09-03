package zone.moddev.mc.skysgrassslabs.init;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.proxy.ClientProxy;

@Mod.EventBusSubscriber(modid = SkysGrassSlabs.MOD_ID, value = Side.CLIENT)
public final class ClientRegistryEvents {
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ClientProxy.registerModels();
    }

    private ClientRegistryEvents() {
    }
}
