package zone.moddev.mc.skysgrassslabs.client;

import java.util.Collections;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;

/** Connects the Mods list's Config button to Forge's standard settings screen. */
public final class GrassSlabsGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return GrassSlabsConfigScreen.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    @SubscribeEvent
    public void configChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if ("skysgrassslabs".equals(event.getModID())) {
            // Save for the next restart; do not rerun generation or compatibility arbitration.
            SkysGrassSlabsConfig.saveChanges();
        }
    }
}
