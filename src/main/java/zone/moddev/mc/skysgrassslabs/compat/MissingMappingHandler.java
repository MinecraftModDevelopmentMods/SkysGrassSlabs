package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;

/** Remaps supported names that changed while an older world was not opened. */
@Mod.EventBusSubscriber(modid = SkysGrassSlabs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MissingMappingHandler {
    @SubscribeEvent
    public static void remapMissingContent(MissingMappingsEvent event) {
        LegacyMigrationHandler.remapMissingContent(event);
    }

    private MissingMappingHandler() {
    }
}
