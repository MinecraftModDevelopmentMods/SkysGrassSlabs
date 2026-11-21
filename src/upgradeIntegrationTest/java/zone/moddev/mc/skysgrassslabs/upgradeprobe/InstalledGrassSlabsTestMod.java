package zone.moddev.mc.skysgrassslabs.upgradeprobe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Build-only stand-in for exercising explicitly enabled installed-mod replacement. */
@Mod(InstalledGrassSlabsTestMod.MOD_ID)
public final class InstalledGrassSlabsTestMod {
    public static final String MOD_ID = "grassslabs";
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, MOD_ID);

    private static final DeferredHolder<Block, Block> GRASS_SLAB = slab(
            "grass_slab", Blocks.GRASS_BLOCK);
    private static final DeferredHolder<Block, Block> DIRT_SLAB = slab(
            "dirt_slab", Blocks.DIRT);
    private static final DeferredHolder<Block, Block> DIRT_PATH_SLAB = slab(
            "dirt_path_slab", Blocks.DIRT_PATH);
    private static final DeferredHolder<Block, Block> GRASS_CARPET = BLOCKS.register(
            "grass_carpet", () -> new CarpetBlock(blockProperties(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GREEN_CARPET),
                    "grass_carpet")));

    static {
        item("grass_slab", GRASS_SLAB);
        item("dirt_slab", DIRT_SLAB);
        item("dirt_path_slab", DIRT_PATH_SLAB);
        item("grass_carpet", GRASS_CARPET);
    }

    public InstalledGrassSlabsTestMod(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private static DeferredHolder<Block, Block> slab(String path, Block source) {
        return BLOCKS.register(path, () -> new SlabBlock(blockProperties(
                BlockBehaviour.Properties.ofFullCopy(source), path)));
    }

    private static void item(String path, DeferredHolder<Block, Block> block) {
        ITEMS.register(path, () -> new BlockItem(block.get(), new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id(path)))));
    }

    private static BlockBehaviour.Properties blockProperties(
            BlockBehaviour.Properties properties, String path) {
        return properties.setId(ResourceKey.create(Registries.BLOCK, id(path)));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
