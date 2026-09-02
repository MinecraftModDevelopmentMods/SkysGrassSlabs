package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.recipe.TurfCuttingRecipe;

public final class ModRecipes {
    public static final String SEED_ORE = "listAllseed";

    public static void register() {
        registerVanillaSeeds();
        GameRegistry.addShapedRecipe(new ItemStack(ModBlocks.DIRT_SLAB, 6),
                "DDD", 'D', Blocks.DIRT);
        GameRegistry.addShapedRecipe(new ItemStack(ModBlocks.GRASS_SLAB, 6),
                "GGG", 'G', Blocks.GRASS);
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(Blocks.GRASS),
                new ItemStack(Blocks.DIRT), SEED_ORE));
        GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(ModBlocks.GRASS_SLAB),
                new ItemStack(ModBlocks.DIRT_SLAB), SEED_ORE));
        BuildingBricksCompat.registerBridgeRecipes();

        RecipeSorter.register("skysgrassslabs:turf_cutting", TurfCuttingRecipe.class,
                RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");
        CraftingManager.getInstance().getRecipeList().add(new TurfCuttingRecipe());
    }

    private static void registerVanillaSeeds() {
        OreDictionary.registerOre(SEED_ORE, new ItemStack(Items.WHEAT_SEEDS));
        OreDictionary.registerOre(SEED_ORE, new ItemStack(Items.MELON_SEEDS));
        OreDictionary.registerOre(SEED_ORE, new ItemStack(Items.PUMPKIN_SEEDS));
        OreDictionary.registerOre(SEED_ORE, new ItemStack(Items.BEETROOT_SEEDS));
    }

    private ModRecipes() {
    }
}
