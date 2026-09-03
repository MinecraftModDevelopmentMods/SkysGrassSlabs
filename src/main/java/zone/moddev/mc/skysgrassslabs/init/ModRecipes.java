package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksDirtSlabRecipe;
import zone.moddev.mc.skysgrassslabs.recipe.TurfCuttingRecipe;

public final class ModRecipes {
    public static final String SEED_ORE = "listAllseed";

    public static void register(IForgeRegistry<IRecipe> registry) {
        registerVanillaSeeds();
        RecipeSorter.register("skysgrassslabs:turf_cutting", TurfCuttingRecipe.class,
                RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");
        RecipeSorter.register("skysgrassslabs:buildingbricks_grass_slab_from_seeds",
                BuildingBricksDirtSlabRecipe.class, RecipeSorter.Category.SHAPELESS,
                "after:minecraft:shapeless");

        registry.registerAll(
                named(new ShapedOreRecipe(null, new ItemStack(ModBlocks.DIRT_SLAB, 6),
                        "DDD", 'D', Blocks.DIRT), "dirt_slab"),
                named(new ShapedOreRecipe(null, new ItemStack(ModBlocks.GRASS_SLAB, 6),
                        "GGG", 'G', Blocks.GRASS), "grass_slab"),
                named(new ShapelessOreRecipe(null, new ItemStack(Blocks.GRASS),
                        new ItemStack(Blocks.DIRT), SEED_ORE), "grass_block_from_seeds"),
                named(new ShapelessOreRecipe(null, new ItemStack(ModBlocks.GRASS_SLAB),
                        new ItemStack(ModBlocks.DIRT_SLAB), SEED_ORE),
                        "grass_slab_from_seeds"),
                named(new TurfCuttingRecipe(), "turf"));

        if (BuildingBricksCompat.isInstalled()) {
            registry.register(named(BuildingBricksCompat.bridgeRecipe(),
                    "buildingbricks_grass_slab_from_seeds"));
        }
    }

    private static IRecipe named(IRecipe recipe, String path) {
        recipe.setRegistryName(new ResourceLocation(SkysGrassSlabs.MOD_ID, path));
        return recipe;
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
