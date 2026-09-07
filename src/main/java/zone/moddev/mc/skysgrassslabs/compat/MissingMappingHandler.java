package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;

/** Remaps supported names that changed while an older world was not opened. */
@Mod.EventBusSubscriber(modid = SkysGrassSlabs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MissingMappingHandler {
    @SubscribeEvent
    public static void remapMissingBlocks(RegistryEvent.MissingMappings<Block> event) {
        LegacyMigrationHandler.remapMissingBlocks(event);
    }

    @SubscribeEvent
    public static void remapMissingItems(RegistryEvent.MissingMappings<Item> event) {
        LegacyMigrationHandler.remapMissingItems(event);
    }

    @SubscribeEvent
    public static void remapMissingSounds(RegistryEvent.MissingMappings<SoundEvent> event) {
        LegacyMigrationHandler.remapMissingSounds(event);
    }

    private MissingMappingHandler() {
    }
}
