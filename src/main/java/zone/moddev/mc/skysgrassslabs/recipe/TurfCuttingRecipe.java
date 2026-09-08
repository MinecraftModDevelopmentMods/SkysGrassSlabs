package zone.moddev.mc.skysgrassslabs.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.init.ModRecipes;

/** Cuts turf while returning the matching dirt and an unchanged shovel. */
public final class TurfCuttingRecipe extends CustomRecipe {
    private final NonNullList<Ingredient> ingredients;

    public TurfCuttingRecipe(CraftingBookCategory category) {
        super(category);
        ingredients = createIngredients();
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        int grassInputs = 0;
        int shovels = 0;
        for (int slot = 0; slot < container.getContainerSize(); ++slot) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!soilRemainder(stack).isEmpty()) {
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
    public ItemStack assemble(CraftingContainer container, HolderLookup.Provider registries) {
        return new ItemStack(ModBlocks.TURF_ITEM.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModBlocks.TURF_ITEM.get());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(
                container.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < container.getContainerSize(); ++slot) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack soil = soilRemainder(stack);
            if (!soil.isEmpty()) {
                remaining.set(slot, soil);
            } else if (isShovel(stack)) {
                ItemStack shovel = stack.copy();
                shovel.setCount(1);
                remaining.set(slot, shovel);
            }
        }
        return remaining;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TURF_CUTTING.get();
    }

    private static boolean isShovel(ItemStack stack) {
        return stack.canPerformAction(ToolActions.SHOVEL_FLATTEN);
    }

    private static NonNullList<Ingredient> createIngredients() {
        NonNullList<Ingredient> result = NonNullList.create();
        result.add(Ingredient.of(Blocks.GRASS_BLOCK, ModBlocks.GRASS_SLAB.get()));
        List<ItemStack> shovels = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ItemStack candidate = new ItemStack(item);
            if (isShovel(candidate)) {
                shovels.add(candidate);
            }
        }
        result.add(Ingredient.of(shovels.stream()));
        return result;
    }

    private static ItemStack soilRemainder(ItemStack stack) {
        if (stack.is(Blocks.GRASS_BLOCK.asItem())) {
            return new ItemStack(Blocks.DIRT);
        }
        if (stack.is(ModBlocks.GRASS_SLAB_ITEM.get())
                || BuildingBricksCompat.isGrassSlabItem(stack)) {
            return new ItemStack(ModBlocks.DIRT_SLAB_ITEM.get());
        }
        return ItemStack.EMPTY;
    }
}
