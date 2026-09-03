package zone.moddev.mc.skysgrassslabs.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class TurfCuttingRecipe extends IForgeRegistryEntry.Impl<IRecipe>
        implements IRecipe {
    private final NonNullList<Ingredient> ingredients = createIngredients();

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        int grassInputs = 0;
        int shovels = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); ++slot) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (soilRemainder(stack) != null) {
                ++grassInputs;
            } else if (isShovel(stack)) {
                ++shovels;
            } else {
                return false;
            }
        }
        return grassInputs == 1 && shovels == 1;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        return matches(inventory, null) ? new ItemStack(ModBlocks.TURF) : ItemStack.EMPTY;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(ModBlocks.TURF);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(
                inventory.getSizeInventory(), ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.getSizeInventory(); ++slot) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack soil = soilRemainder(stack);
            if (soil != null) {
                remaining.set(slot, soil);
            } else if (isShovel(stack)) {
                ItemStack shovel = stack.copy();
                shovel.setCount(1);
                remaining.set(slot, shovel);
            }
        }
        return remaining;
    }

    private static boolean isShovel(ItemStack stack) {
        Set<String> toolClasses = stack.getItem().getToolClasses(stack);
        return toolClasses != null && toolClasses.contains("shovel");
    }

    private static NonNullList<Ingredient> createIngredients() {
        NonNullList<Ingredient> result = NonNullList.create();
        result.add(Ingredient.fromStacks(new ItemStack(Blocks.GRASS),
                new ItemStack(ModBlocks.GRASS_SLAB)));

        List<ItemStack> shovels = new ArrayList<ItemStack>();
        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            ItemStack candidate = new ItemStack(item);
            if (isShovel(candidate)) shovels.add(candidate);
        }
        result.add(new ShovelIngredient(shovels.toArray(new ItemStack[shovels.size()])));
        return result;
    }

    private static ItemStack soilRemainder(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Item.getItemFromBlock(Blocks.GRASS)) {
            return new ItemStack(Blocks.DIRT);
        }
        if (item == Item.getItemFromBlock(ModBlocks.GRASS_SLAB) ||
                BuildingBricksCompat.isGrassSlabItem(stack)) {
            return new ItemStack(ModBlocks.DIRT_SLAB);
        }
        return null;
    }

    private static final class ShovelIngredient extends Ingredient {
        private ShovelIngredient(ItemStack... displayStacks) {
            super(displayStacks);
        }

        @Override
        public boolean apply(@Nullable ItemStack stack) {
            return stack != null && !stack.isEmpty() && isShovel(stack);
        }
    }
}
