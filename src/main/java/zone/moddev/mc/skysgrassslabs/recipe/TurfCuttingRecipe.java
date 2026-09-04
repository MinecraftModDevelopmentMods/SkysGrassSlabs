package zone.moddev.mc.skysgrassslabs.recipe;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class TurfCuttingRecipe implements ICraftingRecipe {
    public static final ResourceLocation SERIALIZER_ID =
            new ResourceLocation(SkysGrassSlabs.MOD_ID, "turf_cutting");
    public static final IRecipeSerializer<TurfCuttingRecipe> SERIALIZER = createSerializer();

    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;

    public TurfCuttingRecipe(ResourceLocation id) {
        this.id = id;
        ingredients = createIngredients();
    }

    @Override
    public boolean matches(CraftingInventory inventory, World world) {
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
    public ItemStack assemble(CraftingInventory inventory) {
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
    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inventory) {
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
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public IRecipeType<?> getType() {
        return IRecipeType.CRAFTING;
    }

    private static boolean isShovel(ItemStack stack) {
        Set<ToolType> types = stack.getItem().getToolTypes(stack);
        return types != null && types.contains(ToolType.SHOVEL);
    }

    private static IRecipeSerializer<TurfCuttingRecipe> createSerializer() {
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
            extends ForgeRegistryEntry<IRecipeSerializer<?>>
            implements IRecipeSerializer<TurfCuttingRecipe> {
        @Override
        public TurfCuttingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            return new TurfCuttingRecipe(recipeId);
        }

        @Override
        public TurfCuttingRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            return new TurfCuttingRecipe(recipeId);
        }

        @Override
        public void toNetwork(PacketBuffer buffer, TurfCuttingRecipe recipe) {
            // The JSON and network forms contain no variable recipe data.
        }
    }
}
