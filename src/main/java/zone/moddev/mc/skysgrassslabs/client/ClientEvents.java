package zone.moddev.mc.skysgrassslabs.client;

import java.util.List;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.GrassSlabsCompat;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Client side biome tint registration. */
@Mod.EventBusSubscriber(modid = SkysGrassSlabs.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        RegisterColorHandlersEvent.Block.BUS.addListener(colorEvent -> {
            colorEvent.register(List.of(BlockTintSources.grassBlock()),
                    ModBlocks.GRASS_SLAB.get(), ModBlocks.TURF.get());

            if (GrassSlabsCompat.grassSlab() != null && GrassSlabsCompat.grassCarpet() != null) {
                colorEvent.register(List.of(BlockTintSources.grassBlock()),
                        GrassSlabsCompat.grassSlab(), GrassSlabsCompat.grassCarpet());
            }
        });
    }

}
