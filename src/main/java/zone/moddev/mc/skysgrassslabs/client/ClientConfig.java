package zone.moddev.mc.skysgrassslabs.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class ClientConfig {
    private ClientConfig() {
    }

    public static void register(ModContainer container) {
        IConfigScreenFactory factory = (minecraft, parent) -> new GrassSlabsConfigScreen(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }
}
