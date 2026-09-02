package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.block.DirtSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.GrassSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.PathSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.TurfBlock;
import zone.moddev.mc.skysgrassslabs.item.NormalizingSlabItem;
import zone.moddev.mc.skysgrassslabs.item.TurfBlockItem;

public final class ModBlocks {
    public static final DirtSlabBlock DIRT_SLAB = new DirtSlabBlock();
    public static final GrassSlabBlock GRASS_SLAB = new GrassSlabBlock();
    public static final PathSlabBlock PATH_SLAB = new PathSlabBlock();
    public static final TurfBlock TURF = new TurfBlock();

    public static void register() {
        registerSlab(DIRT_SLAB, "dirt_slab", net.minecraft.init.Blocks.DIRT);
        registerSlab(GRASS_SLAB, "grass_slab", net.minecraft.init.Blocks.GRASS);
        registerSlab(PATH_SLAB, "path_slab", net.minecraft.init.Blocks.GRASS_PATH);
        registerBlock(TURF, "turf", new TurfBlockItem(TURF));
    }

    private static void registerSlab(Block block, String name, Block combinedBlock) {
        registerBlock(block, name, new NormalizingSlabItem(block, combinedBlock));
    }

    private static void registerBlock(Block block, String name, ItemBlock item) {
        ResourceLocation id = new ResourceLocation(SkysGrassSlabs.MOD_ID, name);
        block.setRegistryName(id).setUnlocalizedName(SkysGrassSlabs.MOD_ID + "." + name)
                .setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        item.setRegistryName(id);
        GameRegistry.register(block);
        GameRegistry.register(item);
    }

    private ModBlocks() {
    }
}
