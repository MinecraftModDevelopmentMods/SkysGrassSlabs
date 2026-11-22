package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
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

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        registry.register(new ResourceLocation(SkysGrassSlabs.MOD_ID, "dirt_slab"), DIRT_SLAB);
        registry.register(new ResourceLocation(SkysGrassSlabs.MOD_ID, "grass_slab"), GRASS_SLAB);
        registry.register(new ResourceLocation(SkysGrassSlabs.MOD_ID, "path_slab"), PATH_SLAB);
        registry.register(new ResourceLocation(SkysGrassSlabs.MOD_ID, "turf"), TURF);
    }

    public static void registerItems(IForgeRegistry<Item> registry) {
        registerItem(registry, DIRT_SLAB,
                new NormalizingSlabItem(DIRT_SLAB, Blocks.DIRT, new Item.Properties()));
        registerItem(registry, GRASS_SLAB,
                new NormalizingSlabItem(GRASS_SLAB, Blocks.GRASS_BLOCK, new Item.Properties()));
        registerItem(registry, PATH_SLAB,
                new NormalizingSlabItem(PATH_SLAB, Blocks.DIRT_PATH, new Item.Properties()));
        registerItem(registry, TURF, new TurfBlockItem(TURF, new Item.Properties()));
    }

    private static void registerItem(IForgeRegistry<Item> registry, Block block, Item item) {
        ResourceLocation name = ForgeRegistries.BLOCKS.getKey(block);
        if (name == null) {
            throw new IllegalStateException("A slab block must be registered before its item");
        }
        registry.register(name, item);
    }

    public static BlockState dirtStateLike(BlockState source) {
        BlockState state = DIRT_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, source.getValue(SlabBlock.TYPE))
                .setValue(SlabBlock.WATERLOGGED, source.getValue(SlabBlock.WATERLOGGED));
        return state.setValue(SnowyDirtBlock.SNOWY,
                source.hasProperty(SnowyDirtBlock.SNOWY) &&
                        source.getValue(SnowyDirtBlock.SNOWY));
    }

    public static BlockState grassStateLike(BlockState source) {
        BlockState state = GRASS_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, source.getValue(SlabBlock.TYPE))
                .setValue(SlabBlock.WATERLOGGED, source.getValue(SlabBlock.WATERLOGGED));
        return state.setValue(SnowyDirtBlock.SNOWY,
                source.hasProperty(SnowyDirtBlock.SNOWY) &&
                        source.getValue(SnowyDirtBlock.SNOWY));
    }

    public static BlockState legacySlabState(boolean grass, int metadata) {
        return (grass ? GRASS_SLAB : DIRT_SLAB).defaultBlockState()
                 .setValue(SlabBlock.TYPE, (metadata & 1) == 0 ? SlabType.TOP : SlabType.BOTTOM)
                 .setValue(SlabBlock.WATERLOGGED, Boolean.FALSE)
                 .setValue(SnowyDirtBlock.SNOWY, Boolean.FALSE);
    }

    private ModBlocks() {
    }
}
