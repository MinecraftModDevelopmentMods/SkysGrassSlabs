package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraftforge.registries.MissingMappingsEvent;

/** Remaps supported names that changed while an older world was not opened. */
public final class MissingMappingHandler {
    public static void register() {
        MissingMappingsEvent.BUS.addListener(MissingMappingHandler::remapMissingContent);
    }

    public static void remapMissingContent(MissingMappingsEvent event) {
        LegacyMigrationHandler.remapMissingContent(event);
    }

    private MissingMappingHandler() {
    }
}
