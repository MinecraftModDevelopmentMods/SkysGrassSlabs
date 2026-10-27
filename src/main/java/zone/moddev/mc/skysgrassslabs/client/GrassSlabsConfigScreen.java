package zone.moddev.mc.skysgrassslabs.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;

/** These generation and migration choices take effect after restarting Minecraft. */
public final class GrassSlabsConfigScreen extends GuiConfig {
    public GrassSlabsConfigScreen(GuiScreen parent) {
        super(parent, elements(), "skysgrassslabs", false, true, "Sky's Grass Slabs");
    }

    private static List<IConfigElement> elements() {
        List<IConfigElement> elements = new ArrayList<>();
        elements.addAll(new ConfigElement(SkysGrassSlabsConfig.configuration().getCategory("worldgen")).getChildElements());
        elements.addAll(new ConfigElement(SkysGrassSlabsConfig.configuration().getCategory("compat")).getChildElements());
        return elements;
    }
}
