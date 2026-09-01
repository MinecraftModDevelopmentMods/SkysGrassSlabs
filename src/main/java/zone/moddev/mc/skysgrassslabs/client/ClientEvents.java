package zone.moddev.mc.skysgrassslabs.client;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.GrassColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Client side biome tint registration. */
@Mod.EventBusSubscriber(modid = SkysGrassSlabs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.GRASS_SLAB.get(), RenderType.cutoutMipped()));
    }

    @SubscribeEvent
    public static void registerBlockColors(ColorHandlerEvent.Block event) {
        event.getBlockColors().register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }

            return level != null && pos != null
                    ? BiomeColors.getAverageGrassColor(level, pos)
                    : GrassColor.get(0.5D, 1.0D);
        }, ModBlocks.GRASS_SLAB.get(), ModBlocks.TURF.get());
    }

    @SubscribeEvent
    public static void registerItemColors(ColorHandlerEvent.Item event) {
        BlockColors colors = event.getBlockColors();
        event.getItemColors().register((stack, tintIndex) ->
                colors.getColor(ModBlocks.GRASS_SLAB.get().defaultBlockState(), null, null, tintIndex),
                ModBlocks.GRASS_SLAB_ITEM.get());

        event.getItemColors().register((stack, tintIndex) ->
                colors.getColor(ModBlocks.TURF.get().defaultBlockState(), null, null, tintIndex),
                ModBlocks.TURF_ITEM.get());
    }
}
