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
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Build-only stand-in for exercising explicitly enabled installed-mod replacement. */
@Mod(InstalledGrassSlabsTestMod.MOD_ID)
public final class InstalledGrassSlabsTestMod {
    public static final String MOD_ID = "grassslabs";
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    private static final RegistryObject<Block> GRASS_SLAB = slab(
            "grass_slab", Blocks.GRASS_BLOCK);
    private static final RegistryObject<Block> DIRT_SLAB = slab(
            "dirt_slab", Blocks.DIRT);
    private static final RegistryObject<Block> DIRT_PATH_SLAB = slab(
            "dirt_path_slab", Blocks.DIRT_PATH);
    private static final RegistryObject<Block> GRASS_CARPET = BLOCKS.register(
            "grass_carpet", () -> new CarpetBlock(blockProperties(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GREEN_CARPET),
                    "grass_carpet")));

    static {
        item("grass_slab", GRASS_SLAB);
        item("dirt_slab", DIRT_SLAB);
        item("dirt_path_slab", DIRT_PATH_SLAB);
        item("grass_carpet", GRASS_CARPET);
    }

    public InstalledGrassSlabsTestMod(FMLJavaModLoadingContext context) {
        BusGroup group = context.getModBusGroup();
        BLOCKS.register(group);
        ITEMS.register(group);
    }

    private static RegistryObject<Block> slab(String path, Block source) {
        return BLOCKS.register(path, () -> new SlabBlock(blockProperties(
                BlockBehaviour.Properties.ofFullCopy(source), path)));
    }

    private static void item(String path, RegistryObject<Block> block) {
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
