package zone.moddev.mc.skysgrassslabs.proxy;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.world.level.GrassColor;
import net.minecraft.client.renderer.BiomeColors;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class ClientProxy {
    public static void register() {
        // Static event subscribers perform client registration at the correct stages.
    }

    public static void registerModels() {
        // Forge resolves item models from each registered item's name.
    }

    public static void registerRenderLayers() {
        RenderType cutoutMipped = RenderType.cutoutMipped();
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.GRASS_SLAB, cutoutMipped);
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.TURF, cutoutMipped);
    }

    public static void registerBlockColors(BlockColors colors) {
        colors.register((state, world, pos, tintIndex) -> world == null || pos == null
                        ? GrassColor.get(0.5D, 1.0D)
                        : BiomeColors.getAverageGrassColor(world, pos),
                ModBlocks.GRASS_SLAB, ModBlocks.TURF);
    }

    public static void registerItemColors(ItemColors colors) {
        colors.register((stack, tintIndex) -> GrassColor.get(0.5D, 1.0D),
                ModBlocks.GRASS_SLAB, ModBlocks.TURF);
    }

    private ClientProxy() {
    }
}
