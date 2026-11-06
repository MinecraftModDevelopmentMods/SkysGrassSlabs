package zone.moddev.mc.skysgrassslabs.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ClientConfig {
    private ClientConfig() {
    }

    public static void register(FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new GrassSlabsConfigScreen(parent)));
    }
}
