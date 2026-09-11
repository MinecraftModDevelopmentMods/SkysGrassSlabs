package zone.moddev.mc.skysgrassslabs.client;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.GrassColor;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.GrassSlabsCompat;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Client side biome tint registration. */
@EventBusSubscriber(modid = SkysGrassSlabs.MOD_ID)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }

            return level != null && pos != null
                    ? BiomeColors.getAverageGrassColor(level, pos)
                    : GrassColor.get(0.5D, 1.0D);
        }, ModBlocks.GRASS_SLAB.get(), ModBlocks.TURF.get());

        if (GrassSlabsCompat.grassSlab() != null && GrassSlabsCompat.grassCarpet() != null) {
            event.register((state, level, pos, tintIndex) -> {
                if (tintIndex != 0) {
                    return -1;
                }
                return level != null && pos != null
                        ? BiomeColors.getAverageGrassColor(level, pos)
                        : GrassColor.get(0.5D, 1.0D);
            }, GrassSlabsCompat.grassSlab(), GrassSlabsCompat.grassCarpet());
        }
    }

}
