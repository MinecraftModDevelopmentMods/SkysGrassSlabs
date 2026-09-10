package zone.moddev.mc.skysgrassslabs.compat;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Compatibility boundary for the supported Grass Slabs source IDs. */
public final class GrassSlabsCompat {
    public static final String MOD_ID = "grassslabs";
    public static final Identifier GRASS_SLAB_ID = id("grass_slab");
    public static final Identifier DIRT_SLAB_ID = id("dirt_slab");
    public static final Identifier DIRT_PATH_SLAB_ID = id("dirt_path_slab");
    public static final Identifier GRASS_CARPET_ID = id("grass_carpet");
    static final List<Identifier> UNSUPPORTED_1_18_IDS = List.of(
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

    public static void register(BusGroup modBusGroup) {
        RegisterEvent.getBus(modBusGroup).addListener(GrassSlabsCompat::registerAliases);
    }

    public static boolean isInstalled() {
        return ModList.isLoaded(MOD_ID);
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
                    () -> grassSlab = new LegacySlabAliasBlock(Blocks.GRASS_BLOCK,
                            ResourceKey.create(Registries.BLOCK, GRASS_SLAB_ID)));
            event.register(Registries.BLOCK, DIRT_SLAB_ID,
                    () -> dirtSlab = new LegacySlabAliasBlock(Blocks.DIRT,
                            ResourceKey.create(Registries.BLOCK, DIRT_SLAB_ID)));
            event.register(Registries.BLOCK, DIRT_PATH_SLAB_ID,
                    () -> pathSlab = new LegacySlabAliasBlock(Blocks.DIRT_PATH,
                            ResourceKey.create(Registries.BLOCK, DIRT_PATH_SLAB_ID)));
            event.register(Registries.BLOCK, GRASS_CARPET_ID,
                    () -> grassCarpet = new LegacyCarpetAliasBlock(
                            ResourceKey.create(Registries.BLOCK, GRASS_CARPET_ID)));
            legacyAliasesRegistered = true;
        } else if (event.getRegistryKey().equals(Registries.ITEM) && legacyAliasesRegistered) {
            event.register(Registries.ITEM, GRASS_SLAB_ID,
                    () -> aliasItem(grassSlab, GRASS_SLAB_ID));
            event.register(Registries.ITEM, DIRT_SLAB_ID,
                    () -> aliasItem(dirtSlab, DIRT_SLAB_ID));
            event.register(Registries.ITEM, DIRT_PATH_SLAB_ID,
                    () -> aliasItem(pathSlab, DIRT_PATH_SLAB_ID));
            event.register(Registries.ITEM, GRASS_CARPET_ID,
                    () -> aliasItem(grassCarpet, GRASS_CARPET_ID));
        }
    }

    private static Item aliasItem(Block block, Identifier id) {
        return new BlockItem(block,
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)));
    }

    private static Block resolve(Block cached, Identifier id) {
        return cached != null ? cached : ForgeRegistries.BLOCKS.getValue(id);
    }

    private static boolean isBlockItem(ItemStack stack, Block block) {
        return block != null && stack.is(block.asItem());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private GrassSlabsCompat() {
    }
}
