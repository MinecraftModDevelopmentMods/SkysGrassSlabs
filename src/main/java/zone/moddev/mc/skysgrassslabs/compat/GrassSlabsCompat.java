package zone.moddev.mc.skysgrassslabs.compat;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** Compatibility boundary for the supported Grass Slabs source IDs. */
public final class GrassSlabsCompat {
    public static final String MOD_ID = "grassslabs";
    public static final ResourceLocation GRASS_SLAB_ID = id("grass_slab");
    public static final ResourceLocation DIRT_SLAB_ID = id("dirt_slab");
    public static final ResourceLocation DIRT_PATH_SLAB_ID = id("dirt_path_slab");
    public static final ResourceLocation GRASS_CARPET_ID = id("grass_carpet");
    static final List<ResourceLocation> UNSUPPORTED_1_18_IDS = List.of(
            id("grass_stairs"),
            id("dirt_stairs"),
            id("dirt_carpet"),
            id("dirt_path_stairs"),
            id("dirt_path_carpet"),
            id("mycelium_slab"),
            id("mycelium_stairs"),
            id("mycelium_carpet"));

    private static Block grassSlab;
    private static Block dirtSlab;
    private static Block pathSlab;
    private static Block grassCarpet;
    private static boolean legacyAliasesRegistered;

    public static void register(IEventBus modBus) {
        modBus.addGenericListener(Block.class, GrassSlabsCompat::registerAliasBlocks);
        modBus.addGenericListener(Item.class, GrassSlabsCompat::registerAliasItems);
    }

    public static boolean isInstalled() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean hasLegacyAliases() {
        return legacyAliasesRegistered;
    }

    public static Block grassSlab() {
        return resolve(grassSlab, GRASS_SLAB_ID);
    }

    public static Block dirtSlab() {
        return resolve(dirtSlab, DIRT_SLAB_ID);
    }

    public static Block pathSlab() {
        return resolve(pathSlab, DIRT_PATH_SLAB_ID);
    }

    public static Block grassCarpet() {
        return resolve(grassCarpet, GRASS_CARPET_ID);
    }

    public static boolean isSupportedItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return isBlockItem(stack, grassSlab()) || isBlockItem(stack, dirtSlab())
                || isBlockItem(stack, pathSlab()) || isBlockItem(stack, grassCarpet());
    }

    private static void registerAliasBlocks(RegistryEvent.Register<Block> event) {
        if (isInstalled()) {
            return;
        }
        grassSlab = alias(new LegacySlabAliasBlock(Blocks.GRASS_BLOCK), GRASS_SLAB_ID);
        dirtSlab = alias(new LegacySlabAliasBlock(Blocks.DIRT), DIRT_SLAB_ID);
        pathSlab = alias(new LegacySlabAliasBlock(Blocks.DIRT_PATH), DIRT_PATH_SLAB_ID);
        grassCarpet = alias(new LegacyCarpetAliasBlock(), GRASS_CARPET_ID);
        event.getRegistry().registerAll(grassSlab, dirtSlab, pathSlab, grassCarpet);
        legacyAliasesRegistered = true;
    }

    private static void registerAliasItems(RegistryEvent.Register<Item> event) {
        if (!legacyAliasesRegistered) {
            return;
        }
        event.getRegistry().registerAll(aliasItem(grassSlab), aliasItem(dirtSlab),
                aliasItem(pathSlab), aliasItem(grassCarpet));
    }

    private static Block alias(Block block, ResourceLocation id) {
        return block.setRegistryName(id);
    }

    private static Item aliasItem(Block block) {
        return new BlockItem(block, new Item.Properties()).setRegistryName(block.getRegistryName());
    }

    private static Block resolve(Block cached, ResourceLocation id) {
        return cached != null ? cached : ForgeRegistries.BLOCKS.getValue(id);
    }

    private static boolean isBlockItem(ItemStack stack, Block block) {
        return block != null && stack.is(block.asItem());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    private GrassSlabsCompat() {
    }
}
