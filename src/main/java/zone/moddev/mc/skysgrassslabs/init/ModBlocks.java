package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.block.DirtSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.GrassSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.PathSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.TurfBlock;
import zone.moddev.mc.skysgrassslabs.block.TurfBlockItem;
import zone.moddev.mc.skysgrassslabs.item.NormalizingSlabItem;

/** Stable block and item registrations. */
@EventBusSubscriber(modid = SkysGrassSlabs.MOD_ID)
public final class ModBlocks {
    private static final Identifier BUILDING_BLOCKS_TAB =
            Identifier.withDefaultNamespace("building_blocks");
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, SkysGrassSlabs.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, SkysGrassSlabs.MOD_ID);

    public static final DeferredHolder<Block, Block> DIRT_SLAB = BLOCKS.register("dirt_slab",
            () -> new DirtSlabBlock(RegistrationProperties.block(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).randomTicks(), "dirt_slab")));
    public static final DeferredHolder<Block, Block> GRASS_SLAB = BLOCKS.register("grass_slab",
            () -> new GrassSlabBlock(RegistrationProperties.block(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).randomTicks(), "grass_slab")));
    public static final DeferredHolder<Block, Block> PATH_SLAB = BLOCKS.register("path_slab",
            () -> new PathSlabBlock(RegistrationProperties.block(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT_PATH), "path_slab")));
    public static final DeferredHolder<Block, Block> TURF = BLOCKS.register("turf",
            () -> new TurfBlock(RegistrationProperties.block(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CARPET.green()).randomTicks(), "turf")));

    public static final DeferredHolder<Item, Item> DIRT_SLAB_ITEM = slabItem(
            "dirt_slab", DIRT_SLAB, Blocks.DIRT);
    public static final DeferredHolder<Item, Item> GRASS_SLAB_ITEM = slabItem(
            "grass_slab", GRASS_SLAB, Blocks.GRASS_BLOCK);
    public static final DeferredHolder<Item, Item> PATH_SLAB_ITEM = slabItem(
            "path_slab", PATH_SLAB, Blocks.DIRT_PATH);
    public static final DeferredHolder<Item, Item> TURF_ITEM = ITEMS.register("turf",
            () -> new TurfBlockItem(TURF.get(), RegistrationProperties.item(
                    new Item.Properties(), "turf")));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
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

    private static DeferredHolder<Item, Item> slabItem(String name, DeferredHolder<Block, Block> block,
            Block combinedBlock) {
        return ITEMS.register(name, () -> new NormalizingSlabItem(block.get(), combinedBlock,
                RegistrationProperties.item(new Item.Properties(), name)));
    }

    @SubscribeEvent
    public static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (BUILDING_BLOCKS_TAB.equals(event.getTabKey().identifier())) {
            event.accept(DIRT_SLAB_ITEM.get());
            event.accept(GRASS_SLAB_ITEM.get());
            event.accept(PATH_SLAB_ITEM.get());
            event.accept(TURF_ITEM.get());
        }
    }
}
