package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.recipe.TurfCuttingRecipe;

/** Stable recipe serializer registrations. */
public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, SkysGrassSlabs.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TurfCuttingRecipe>> TURF_CUTTING =
            SERIALIZERS.register("turf_cutting",
                    () -> new CustomRecipe.Serializer<>(TurfCuttingRecipe::new));

    private ModRecipes() {
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
