package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistryEntry;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.init.ModRecipes;

/** Matches BuildingBricks material items after its late material registry is available. */
public final class BuildingBricksDirtSlabRecipe extends IForgeRegistryEntry.Impl<IRecipe>
        implements IRecipe {
    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        boolean dirtSlab = false;
        boolean seed = false;
        NonNullList<ItemStack> seeds = OreDictionary.getOres(ModRecipes.SEED_ORE);
        for (int slot = 0; slot < inventory.getSizeInventory(); ++slot) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
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
        return matches(inventory, null) ? new ItemStack(ModBlocks.GRASS_SLAB) : ItemStack.EMPTY;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(ModBlocks.GRASS_SLAB);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        return ForgeHooks.defaultRecipeGetRemainingItems(inventory);
    }
}
