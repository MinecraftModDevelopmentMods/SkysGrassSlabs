package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.recipe.TurfCuttingRecipe;

/** Stable recipe serializer registrations. */
public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SkysGrassSlabs.MOD_ID);

    public static final RegistryObject<RecipeSerializer<TurfCuttingRecipe>> TURF_CUTTING =
            SERIALIZERS.register("turf_cutting",
                    () -> new SimpleRecipeSerializer<>(TurfCuttingRecipe::new));

    private ModRecipes() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
