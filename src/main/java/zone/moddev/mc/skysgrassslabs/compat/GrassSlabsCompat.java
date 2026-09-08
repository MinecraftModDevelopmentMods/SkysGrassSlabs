package zone.moddev.mc.skysgrassslabs.compat;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

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
        modBus.addListener(GrassSlabsCompat::registerAliases);
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

    private static void registerAliases(RegisterEvent event) {
        if (isInstalled()) {
            return;
        }
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            event.register(Registries.BLOCK, GRASS_SLAB_ID,
                    () -> grassSlab = new LegacySlabAliasBlock(Blocks.GRASS_BLOCK));
            event.register(Registries.BLOCK, DIRT_SLAB_ID,
                    () -> dirtSlab = new LegacySlabAliasBlock(Blocks.DIRT));
            event.register(Registries.BLOCK, DIRT_PATH_SLAB_ID,
                    () -> pathSlab = new LegacySlabAliasBlock(Blocks.DIRT_PATH));
            event.register(Registries.BLOCK, GRASS_CARPET_ID,
                    () -> grassCarpet = new LegacyCarpetAliasBlock());
            legacyAliasesRegistered = true;
        } else if (event.getRegistryKey().equals(Registries.ITEM) && legacyAliasesRegistered) {
            event.register(Registries.ITEM, GRASS_SLAB_ID, () -> aliasItem(grassSlab));
            event.register(Registries.ITEM, DIRT_SLAB_ID, () -> aliasItem(dirtSlab));
            event.register(Registries.ITEM, DIRT_PATH_SLAB_ID, () -> aliasItem(pathSlab));
            event.register(Registries.ITEM, GRASS_CARPET_ID, () -> aliasItem(grassCarpet));
        }
    }

    private static Item aliasItem(Block block) {
        return new BlockItem(block, new Item.Properties());
    }

    private static Block resolve(Block cached, ResourceLocation id) {
        return cached != null ? cached : ForgeRegistries.BLOCKS.getValue(id);
    }

    private static boolean isBlockItem(ItemStack stack, Block block) {
        return block != null && stack.is(block.asItem());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private GrassSlabsCompat() {
    }
}
