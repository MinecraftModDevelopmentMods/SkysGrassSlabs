package zone.moddev.mc.skysgrassslabs.compat;

import java.util.List;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.oredict.OreDictionary;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.init.ModRecipes;

/** Matches BuildingBricks material items after its late material registry is available. */
final class BuildingBricksDirtSlabRecipe implements IRecipe {
    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        boolean dirtSlab = false;
        boolean seed = false;
        List<ItemStack> seeds = OreDictionary.getOres(ModRecipes.SEED_ORE);
        for (int slot = 0; slot < inventory.getSizeInventory(); ++slot) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null) continue;
            if (!dirtSlab && BuildingBricksCompat.isDirtSlabItem(stack)) {
                dirtSlab = true;
            } else if (!seed && OreDictionary.containsMatch(false, seeds, stack)) {
                seed = true;
            } else {
                return false;
            }
        }
        return dirtSlab && seed;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        return matches(inventory, null) ? new ItemStack(ModBlocks.GRASS_SLAB) : null;
    }

    @Override
    public int getRecipeSize() {
        return 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(ModBlocks.GRASS_SLAB);
    }

    @Override
    public ItemStack[] getRemainingItems(InventoryCrafting inventory) {
        return ForgeHooks.defaultRecipeGetRemainingItems(inventory);
    }
}
