package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.block.DirtSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.GrassSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.PathSlabBlock;
import zone.moddev.mc.skysgrassslabs.block.TurfBlock;
import zone.moddev.mc.skysgrassslabs.block.TurfBlockItem;

/** Stable block and item registrations. */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SkysGrassSlabs.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SkysGrassSlabs.MOD_ID);

    public static final RegistryObject<Block> DIRT_SLAB = BLOCKS.register("dirt_slab",
            () -> new DirtSlabBlock(BlockBehaviour.Properties.copy(Blocks.DIRT).randomTicks()));
    public static final RegistryObject<Block> GRASS_SLAB = BLOCKS.register("grass_slab",
            () -> new GrassSlabBlock(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK).randomTicks()));
    public static final RegistryObject<Block> PATH_SLAB = BLOCKS.register("path_slab",
            () -> new PathSlabBlock(BlockBehaviour.Properties.copy(Blocks.DIRT_PATH)));
    public static final RegistryObject<Block> TURF = BLOCKS.register("turf",
            () -> new TurfBlock(BlockBehaviour.Properties.copy(Blocks.GREEN_CARPET).randomTicks()));

    public static final RegistryObject<Item> DIRT_SLAB_ITEM = item("dirt_slab", DIRT_SLAB);
    public static final RegistryObject<Item> GRASS_SLAB_ITEM = item("grass_slab", GRASS_SLAB);
    public static final RegistryObject<Item> PATH_SLAB_ITEM = item("path_slab", PATH_SLAB);
    public static final RegistryObject<Item> TURF_ITEM = ITEMS.register("turf",
            () -> new TurfBlockItem(TURF.get(),
                    new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }

    private static RegistryObject<Item> item(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(),
                new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
    }
}
