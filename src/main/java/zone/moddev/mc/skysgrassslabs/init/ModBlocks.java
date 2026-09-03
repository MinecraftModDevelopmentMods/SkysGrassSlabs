package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirtSnowy;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.state.properties.SlabType;
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
                item(new NormalizingSlabItem(DIRT_SLAB, Blocks.DIRT,
                        new Item.Properties().group(ItemGroup.BUILDING_BLOCKS)), DIRT_SLAB),
                item(new NormalizingSlabItem(GRASS_SLAB, Blocks.GRASS_BLOCK,
                        new Item.Properties().group(ItemGroup.BUILDING_BLOCKS)), GRASS_SLAB),
                item(new NormalizingSlabItem(PATH_SLAB, Blocks.GRASS_PATH,
                        new Item.Properties().group(ItemGroup.BUILDING_BLOCKS)), PATH_SLAB),
                item(new TurfBlockItem(TURF,
                        new Item.Properties().group(ItemGroup.BUILDING_BLOCKS)), TURF));
    }

    public static IBlockState dirtStateLike(IBlockState source) {
        IBlockState state = DIRT_SLAB.getDefaultState()
                .with(BlockSlab.TYPE, source.get(BlockSlab.TYPE))
                .with(BlockSlab.WATERLOGGED, source.get(BlockSlab.WATERLOGGED));
        return state.with(BlockDirtSnowy.SNOWY,
                source.has(BlockDirtSnowy.SNOWY) && source.get(BlockDirtSnowy.SNOWY));
    }

    public static IBlockState grassStateLike(IBlockState source) {
        IBlockState state = GRASS_SLAB.getDefaultState()
                .with(BlockSlab.TYPE, source.get(BlockSlab.TYPE))
                .with(BlockSlab.WATERLOGGED, source.get(BlockSlab.WATERLOGGED));
        return state.with(BlockDirtSnowy.SNOWY,
                source.has(BlockDirtSnowy.SNOWY) && source.get(BlockDirtSnowy.SNOWY));
    }

    public static IBlockState legacySlabState(boolean grass, int metadata) {
        return (grass ? GRASS_SLAB : DIRT_SLAB).getDefaultState()
                .with(BlockSlab.TYPE, (metadata & 1) == 0 ? SlabType.TOP : SlabType.BOTTOM)
                .with(BlockSlab.WATERLOGGED, Boolean.FALSE)
                .with(BlockDirtSnowy.SNOWY, Boolean.FALSE);
    }

    private static <T extends Block> T configure(T block, String name) {
        block.setRegistryName(new ResourceLocation(SkysGrassSlabs.MOD_ID, name));
        return block;
    }

    private static Item item(Item item, Block block) {
        return item.setRegistryName(block.getRegistryName());
    }

    private ModBlocks() {
    }
}
