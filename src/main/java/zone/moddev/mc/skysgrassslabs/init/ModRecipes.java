package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.item.crafting.RecipeSerializers;
import zone.moddev.mc.skysgrassslabs.recipe.TurfCuttingRecipe;

public final class ModRecipes {
    private static boolean registered;

    public static synchronized void registerSerializer() {
        if (!registered) {
            RecipeSerializers.register(TurfCuttingRecipe.SERIALIZER);
            registered = true;
        }
    }

    private ModRecipes() {
    }
}
