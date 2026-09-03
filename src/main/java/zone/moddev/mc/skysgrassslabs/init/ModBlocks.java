package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.block.DirtSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.GrassSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.PathSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.TurfBlock;
import zone.moddev.mc.skysgrassslabs.item.NormalizingSlabItem;
import zone.moddev.mc.skysgrassslabs.item.TurfBlockItem;

public final class ModBlocks {
    public static final DirtSlabBlock DIRT_SLAB = configure(new DirtSlabBlock(), "dirt_slab");
    public static final GrassSlabBlock GRASS_SLAB = configure(new GrassSlabBlock(), "grass_slab");
    public static final PathSlabBlock PATH_SLAB = configure(new PathSlabBlock(), "path_slab");
    public static final TurfBlock TURF = configure(new TurfBlock(), "turf");

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        registry.registerAll(DIRT_SLAB, GRASS_SLAB, PATH_SLAB, TURF);
    }

    public static void registerItems(IForgeRegistry<Item> registry) {
        registry.registerAll(
                item(new NormalizingSlabItem(DIRT_SLAB, net.minecraft.init.Blocks.DIRT), DIRT_SLAB),
                item(new NormalizingSlabItem(GRASS_SLAB, net.minecraft.init.Blocks.GRASS), GRASS_SLAB),
                item(new NormalizingSlabItem(PATH_SLAB, net.minecraft.init.Blocks.GRASS_PATH), PATH_SLAB),
                item(new TurfBlockItem(TURF), TURF));
    }

    private static <T extends Block> T configure(T block, String name) {
        ResourceLocation id = new ResourceLocation(SkysGrassSlabs.MOD_ID, name);
        block.setRegistryName(id).setTranslationKey(SkysGrassSlabs.MOD_ID + "." + name)
                .setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        return block;
    }

    private static Item item(ItemBlock item, Block block) {
        return item.setRegistryName(block.getRegistryName());
    }

    private ModBlocks() {
    }
}
