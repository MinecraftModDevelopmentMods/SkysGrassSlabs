package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

/** Narrow compatibility boundary for the supported historical slab IDs. */
public final class BuildingBricksCompat {
    public static final String MOD_ID = "buildingbricks";
    public static final ResourceLocation GRASS_SLAB_ID =
            new ResourceLocation(MOD_ID, "grass_slab");
    public static final ResourceLocation DIRT_SLAB_ID =
            new ResourceLocation(MOD_ID, "dirt_slab");
    public static final ResourceLocation HISTORICAL_GRASS_SLAB_ID =
            new ResourceLocation("buildingbrickscompatvanilla", "grass_slab");

    private static Block grassSlab;
    private static Block dirtSlab;
    private static Block historicalGrassSlab;
    private static boolean legacyAliasesRegistered;

    public static boolean isInstalled() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean hasLegacyAliases() {
        return legacyAliasesRegistered;
    }

    public static Block grassSlab() {
        return grassSlab;
    }

    public static Block dirtSlab() {
        return dirtSlab;
    }

    public static Block historicalGrassSlab() {
        return historicalGrassSlab;
    }

    public static boolean isGrassSlabItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return grassSlab != null && stack.getItem() == grassSlab.asItem() ||
                historicalGrassSlab != null && stack.getItem() == historicalGrassSlab.asItem();
    }

    static boolean isDirtSlabItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && dirtSlab != null &&
                stack.getItem() == dirtSlab.asItem();
    }

    public static void registerLegacyAliasBlocks(IForgeRegistry<Block> registry) {
        if (isInstalled()) return;
        grassSlab = new LegacySlabAliasBlock(true);
        dirtSlab = new LegacySlabAliasBlock(false);
        historicalGrassSlab = new LegacySlabAliasBlock(true);
        registry.register(GRASS_SLAB_ID, grassSlab);
        registry.register(DIRT_SLAB_ID, dirtSlab);
        registry.register(HISTORICAL_GRASS_SLAB_ID, historicalGrassSlab);
        legacyAliasesRegistered = true;
    }

    public static void registerLegacyAliasItems(IForgeRegistry<Item> registry) {
        if (!legacyAliasesRegistered) return;
        registry.register(GRASS_SLAB_ID, aliasItem(grassSlab));
        registry.register(DIRT_SLAB_ID, aliasItem(dirtSlab));
        registry.register(HISTORICAL_GRASS_SLAB_ID, aliasItem(historicalGrassSlab));
    }

    private static Item aliasItem(Block block) {
        return new BlockItem(block, new Item.Properties());
    }

    private BuildingBricksCompat() {
    }
}
