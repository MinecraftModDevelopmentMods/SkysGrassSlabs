package zone.moddev.mc.skysgrassslabs.recipe;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class TurfCuttingRecipe implements CraftingRecipe {
    public static final ResourceLocation SERIALIZER_ID =
            new ResourceLocation(SkysGrassSlabs.MOD_ID, "turf_cutting");
    public static final RecipeSerializer<TurfCuttingRecipe> SERIALIZER = createSerializer();

    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;

    public TurfCuttingRecipe(ResourceLocation id) {
        this.id = id;
        ingredients = createIngredients();
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        int grassInputs = 0;
        int shovels = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); ++slot) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
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
    public ItemStack assemble(CraftingContainer inventory) {
        return matches(inventory, null) ? new ItemStack(ModBlocks.TURF) : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem() {
        return new ItemStack(ModBlocks.TURF);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inventory) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(
                inventory.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.getContainerSize(); ++slot) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
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
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    private static boolean isShovel(ItemStack stack) {
        return stack.canPerformAction(ToolActions.SHOVEL_FLATTEN);
    }

    private static RecipeSerializer<TurfCuttingRecipe> createSerializer() {
        Serializer serializer = new Serializer();
        serializer.setRegistryName(SERIALIZER_ID);
        return serializer;
    }

    private static NonNullList<Ingredient> createIngredients() {
        NonNullList<Ingredient> result = NonNullList.create();
        result.add(Ingredient.of(Blocks.GRASS_BLOCK, ModBlocks.GRASS_SLAB));
        List<ItemStack> shovels = new ArrayList<ItemStack>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ItemStack candidate = new ItemStack(item);
            if (isShovel(candidate)) shovels.add(candidate);
        }
        result.add(Ingredient.of(shovels.toArray(new ItemStack[shovels.size()])));
        return result;
    }

    private static ItemStack soilRemainder(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Blocks.GRASS_BLOCK.asItem()) {
            return new ItemStack(Blocks.DIRT);
        }
        if (item == ModBlocks.GRASS_SLAB.asItem() ||
                BuildingBricksCompat.isGrassSlabItem(stack)) {
            return new ItemStack(ModBlocks.DIRT_SLAB);
        }
        return ItemStack.EMPTY;
    }

    private static final class Serializer
            extends ForgeRegistryEntry<RecipeSerializer<?>>
            implements RecipeSerializer<TurfCuttingRecipe> {
        @Override
        public TurfCuttingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            return new TurfCuttingRecipe(recipeId);
        }

        @Override
        public TurfCuttingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new TurfCuttingRecipe(recipeId);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, TurfCuttingRecipe recipe) {
            // The JSON and network forms contain no variable recipe data.
        }
    }
}
