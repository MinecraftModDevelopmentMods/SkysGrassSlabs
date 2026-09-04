package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
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
        grassSlab = legacyAlias(GRASS_SLAB_ID, true);
        dirtSlab = legacyAlias(DIRT_SLAB_ID, false);
        historicalGrassSlab = legacyAlias(HISTORICAL_GRASS_SLAB_ID, true);
        registry.registerAll(grassSlab, dirtSlab, historicalGrassSlab);
        legacyAliasesRegistered = true;
    }

    public static void registerLegacyAliasItems(IForgeRegistry<Item> registry) {
        if (!legacyAliasesRegistered) return;
        registry.registerAll(aliasItem(grassSlab), aliasItem(dirtSlab),
                aliasItem(historicalGrassSlab));
    }

    private static Block legacyAlias(ResourceLocation id, boolean grass) {
        return new LegacySlabAliasBlock(grass).setRegistryName(id);
    }

    private static Item aliasItem(Block block) {
        return new BlockItem(block, new Item.Properties()).setRegistryName(block.getRegistryName());
    }

    private BuildingBricksCompat() {
    }
}
