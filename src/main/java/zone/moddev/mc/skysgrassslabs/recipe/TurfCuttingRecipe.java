package zone.moddev.mc.skysgrassslabs.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ToolActions;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.init.ModRecipes;

/** Cuts grass from a full block or slab while returning both source and tool remnants. */
public final class TurfCuttingRecipe extends CustomRecipe {
    public TurfCuttingRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        boolean foundGrass = false;
        boolean foundShovel = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (isGrassSource(stack)) {
                if (foundGrass) {
                    return false;
                }
                foundGrass = true;
            } else if (stack.canPerformAction(ToolActions.SHOVEL_FLATTEN)) {
                if (foundShovel) {
                    return false;
                }
                foundShovel = true;
            } else {
                return false;
            }
        }
        return foundGrass && foundShovel;
    }

    @Override
    public ItemStack assemble(CraftingContainer container) {
        return new ItemStack(ModBlocks.TURF_ITEM.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(
                container.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(Blocks.GRASS_BLOCK.asItem())) {
                remaining.set(slot, new ItemStack(Blocks.DIRT));
            } else if (stack.is(ModBlocks.GRASS_SLAB_ITEM.get())) {
                remaining.set(slot, new ItemStack(ModBlocks.DIRT_SLAB_ITEM.get()));
            } else if (stack.canPerformAction(ToolActions.SHOVEL_FLATTEN)) {
                ItemStack tool = stack.copy();
                tool.setCount(1);
                remaining.set(slot, tool);
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem() {
        return new ItemStack(ModBlocks.TURF_ITEM.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TURF_CUTTING.get();
    }

    private static boolean isGrassSource(ItemStack stack) {
        return stack.is(Blocks.GRASS_BLOCK.asItem())
                || stack.is(ModBlocks.GRASS_SLAB_ITEM.get());
    }
}
