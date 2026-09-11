package zone.moddev.mc.skysgrassslabs.recipe;

import java.util.ArrayList;
import java.util.List;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ItemAbilities;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.init.ModRecipes;

/** Cuts turf while returning the matching dirt and an unchanged shovel. */
public final class TurfCuttingRecipe extends CustomRecipe {
    public static final TurfCuttingRecipe INSTANCE = new TurfCuttingRecipe();
    public static final MapCodec<TurfCuttingRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, TurfCuttingRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<TurfCuttingRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    private volatile NonNullList<Ingredient> ingredients;
    private volatile PlacementInfo placementInfo;

    private TurfCuttingRecipe() {
    }

    @Override
    public boolean matches(CraftingInput container, Level level) {
        int grassInputs = 0;
        int shovels = 0;
        for (int slot = 0; slot < container.size(); ++slot) {
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
    public ItemStack assemble(CraftingInput container) {
        return new ItemStack(ModBlocks.TURF_ITEM.get());
    }

    @Override
    public PlacementInfo placementInfo() {
        PlacementInfo result = placementInfo;
        if (result == null) {
            synchronized (this) {
                result = placementInfo;
                if (result == null) {
                    result = PlacementInfo.create(ingredients());
                    placementInfo = result;
                }
            }
        }
        return result;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.BUILDING;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new ShapelessCraftingRecipeDisplay(
                ingredients().stream().map(Ingredient::display).toList(),
                new SlotDisplay.ItemSlotDisplay(ModBlocks.TURF_ITEM.get()),
                new SlotDisplay.ItemSlotDisplay(Blocks.CRAFTING_TABLE.asItem())));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(
                container.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < container.size(); ++slot) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack soil = soilRemainder(stack);
            if (!soil.isEmpty()) {
                remaining.set(slot, soil);
            } else if (isShovel(stack)) {
                remaining.set(slot, stack.copyWithCount(1));
            }
        }
        return remaining;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public RecipeSerializer<TurfCuttingRecipe> getSerializer() {
        return ModRecipes.TURF_CUTTING.get();
    }

    private static boolean isShovel(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.SHOVEL_FLATTEN);
    }

    private static NonNullList<Ingredient> createIngredients() {
        NonNullList<Ingredient> result = NonNullList.create();
        result.add(Ingredient.of(Blocks.GRASS_BLOCK, ModBlocks.GRASS_SLAB.get()));
        List<Item> shovels = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM.stream().toList()) {
            ItemStack candidate = new ItemStack(item);
            if (isShovel(candidate)) {
                shovels.add(item);
            }
        }
        result.add(Ingredient.of(shovels.stream()));
        return result;
    }

    private NonNullList<Ingredient> ingredients() {
        NonNullList<Ingredient> result = ingredients;
        if (result == null) {
            synchronized (this) {
                result = ingredients;
                if (result == null) {
                    result = createIngredients();
                    ingredients = result;
                }
            }
        }
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
