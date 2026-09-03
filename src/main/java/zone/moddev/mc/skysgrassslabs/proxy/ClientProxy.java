package zone.moddev.mc.skysgrassslabs.proxy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

@SideOnly(Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    public static void registerModels() {
        registerModel(ModBlocks.DIRT_SLAB, "dirt_slab");
        registerModel(ModBlocks.GRASS_SLAB, "grass_slab");
        registerModel(ModBlocks.PATH_SLAB, "path_slab");
        registerModel(ModBlocks.TURF, "turf");
        if (BuildingBricksCompat.hasLegacyAliases()) {
            registerLegacyAliasModel(BuildingBricksCompat.grassSlab(), "grass_slab");
            registerLegacyAliasModel(BuildingBricksCompat.dirtSlab(), "dirt_slab");
            registerLegacyAliasModel(BuildingBricksCompat.historicalGrassSlab(),
                    "grass_slab");
        }
    }

    @Override
    public void init() {
        BlockColors blockColors = Minecraft.getMinecraft().getBlockColors();
        ItemColors itemColors = Minecraft.getMinecraft().getItemColors();
        IBlockColor grassBlockColor = (state, world, pos, tintIndex) ->
                world == null || pos == null
                        ? ColorizerGrass.getGrassColor(0.5D, 1.0D)
                        : BiomeColorHelper.getGrassColorAtPos(world, pos);
        IItemColor grassItemColor = (stack, tintIndex) -> ColorizerGrass.getGrassColor(0.5D, 1.0D);
        blockColors.registerBlockColorHandler(grassBlockColor, ModBlocks.GRASS_SLAB, ModBlocks.TURF);
        itemColors.registerItemColorHandler(grassItemColor,
                Item.getItemFromBlock(ModBlocks.GRASS_SLAB), Item.getItemFromBlock(ModBlocks.TURF));
        if (BuildingBricksCompat.hasLegacyAliases()) {
            blockColors.registerBlockColorHandler(grassBlockColor,
                    BuildingBricksCompat.grassSlab(),
                    BuildingBricksCompat.historicalGrassSlab());
            itemColors.registerItemColorHandler(grassItemColor,
                    Item.getItemFromBlock(BuildingBricksCompat.grassSlab()),
                    Item.getItemFromBlock(BuildingBricksCompat.historicalGrassSlab()));
        }
    }

    private static void registerModel(Block block, String path) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
                new ModelResourceLocation(new ResourceLocation(SkysGrassSlabs.MOD_ID, path), "inventory"));
    }

    private static void registerLegacyAliasModel(Block block, String path) {
        final ResourceLocation model = new ResourceLocation(SkysGrassSlabs.MOD_ID, path);
        ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                String half = state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM
                        ? "bottom" : "top";
                return new ModelResourceLocation(model, "half=" + half + ",snowy=false");
            }
        });
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
                new ModelResourceLocation(model, "inventory"));
    }
}
