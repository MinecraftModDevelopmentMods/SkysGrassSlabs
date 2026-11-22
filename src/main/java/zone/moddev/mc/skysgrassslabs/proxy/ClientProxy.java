package zone.moddev.mc.skysgrassslabs.proxy;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.world.level.GrassColor;
import net.minecraft.client.renderer.BiomeColors;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import zone.moddev.mc.skysgrassslabs.client.GrassSlabsConfigScreen;
import zone.moddev.mc.skysgrassslabs.compat.GrassSlabsCompat;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import net.minecraft.world.level.block.Block;

public final class ClientProxy {
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new GrassSlabsConfigScreen(parent)));
    }

    public static void registerModels() {
        // Forge resolves item models from each registered item's name.
    }

    public static void registerRenderLayers() {
        RenderType cutoutMipped = RenderType.cutoutMipped();
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.GRASS_SLAB, cutoutMipped);
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.TURF, cutoutMipped);
        for (Block block : grassBlocks()) {
            ItemBlockRenderTypes.setRenderLayer(block, cutoutMipped);
        }
    }

    public static void registerBlockColors(BlockColors colors) {
        colors.register((state, world, pos, tintIndex) -> tintIndex != 0 ? -1
                        : world == null || pos == null ? GrassColor.get(0.5D, 1.0D)
                                : BiomeColors.getAverageGrassColor(world, pos), grassBlocks());
    }

    public static void registerItemColors(ItemColors colors) {
        colors.register((stack, tintIndex) -> tintIndex == 0 ? GrassColor.get(0.5D, 1.0D) : -1,
                grassBlocks());
    }

    private static Block[] grassBlocks() {
        java.util.List<Block> blocks = new java.util.ArrayList<>();
        blocks.add(ModBlocks.GRASS_SLAB);
        blocks.add(ModBlocks.TURF);
        if (BuildingBricksCompat.hasLegacyAliases()) {
            blocks.add(BuildingBricksCompat.grassSlab());
            blocks.add(BuildingBricksCompat.historicalGrassSlab());
        }
        if (GrassSlabsCompat.hasLegacyAliases()) {
            blocks.add(GrassSlabsCompat.grassSlab());
            blocks.add(GrassSlabsCompat.grassCarpet());
        }
        return blocks.toArray(new Block[0]);
    }

    private ClientProxy() {
    }
}
