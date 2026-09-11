package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.block.DirtSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.GrassSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.PathSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.TurfBlock;
import zone.moddev.mc.skysgrassslabs.block.TurfBlockItem;
import zone.moddev.mc.skysgrassslabs.item.NormalizingSlabItem;

/** Stable block and item registrations. */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SkysGrassSlabs.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SkysGrassSlabs.MOD_ID);

    public static final RegistryObject<Block> DIRT_SLAB = BLOCKS.register("dirt_slab",
            () -> new DirtSlabBlock(RegistrationProperties.block(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).randomTicks(), "dirt_slab")));
    public static final RegistryObject<Block> GRASS_SLAB = BLOCKS.register("grass_slab",
            () -> new GrassSlabBlock(RegistrationProperties.block(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).randomTicks(), "grass_slab")));
    public static final RegistryObject<Block> PATH_SLAB = BLOCKS.register("path_slab",
            () -> new PathSlabBlock(RegistrationProperties.block(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT_PATH), "path_slab")));
    public static final RegistryObject<Block> TURF = BLOCKS.register("turf",
            () -> new TurfBlock(RegistrationProperties.block(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CARPET.green()).randomTicks(), "turf")));

    public static final RegistryObject<Item> DIRT_SLAB_ITEM = slabItem(
            "dirt_slab", DIRT_SLAB, Blocks.DIRT);
    public static final RegistryObject<Item> GRASS_SLAB_ITEM = slabItem(
            "grass_slab", GRASS_SLAB, Blocks.GRASS_BLOCK);
    public static final RegistryObject<Item> PATH_SLAB_ITEM = slabItem(
            "path_slab", PATH_SLAB, Blocks.DIRT_PATH);
    public static final RegistryObject<Item> TURF_ITEM = ITEMS.register("turf",
            () -> new TurfBlockItem(TURF.get(), RegistrationProperties.item(
                    new Item.Properties(), "turf")));

    private ModBlocks() {
    }

    public static void register(BusGroup modBusGroup) {
        BLOCKS.register(modBusGroup);
        ITEMS.register(modBusGroup);
        BuildCreativeModeTabContentsEvent.BUS.addListener(ModBlocks::buildCreativeTab);
    }

    public static BlockState dirtStateLike(BlockState source) {
        BlockState state = DIRT_SLAB.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                        source.getValue(net.minecraft.world.level.block.SlabBlock.TYPE))
                .setValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED,
                        source.getValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED));
        return state.setValue(net.minecraft.world.level.block.SnowyBlock.SNOWY,
                source.hasProperty(net.minecraft.world.level.block.SnowyBlock.SNOWY)
                        && source.getValue(net.minecraft.world.level.block.SnowyBlock.SNOWY));
    }

    public static BlockState grassStateLike(BlockState source) {
        BlockState state = GRASS_SLAB.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                        source.getValue(net.minecraft.world.level.block.SlabBlock.TYPE))
                .setValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED,
                        source.getValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED));
        return state.setValue(net.minecraft.world.level.block.SnowyBlock.SNOWY,
                source.hasProperty(net.minecraft.world.level.block.SnowyBlock.SNOWY)
                        && source.getValue(net.minecraft.world.level.block.SnowyBlock.SNOWY));
    }

    private static RegistryObject<Item> slabItem(String name, RegistryObject<Block> block,
            Block combinedBlock) {
        return ITEMS.register(name, () -> new NormalizingSlabItem(block.get(), combinedBlock,
                RegistrationProperties.item(new Item.Properties(), name)));
    }

    private static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.BUILDING_BLOCKS.equals(event.getTabKey())) {
            event.accept(DIRT_SLAB_ITEM);
            event.accept(GRASS_SLAB_ITEM);
            event.accept(PATH_SLAB_ITEM);
            event.accept(TURF_ITEM);
        }
    }
}
