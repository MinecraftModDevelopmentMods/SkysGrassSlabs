package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.world.gen.feature.Feature;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.compat.LegacyMigrationHandler;
import zone.moddev.mc.skysgrassslabs.recipe.TurfCuttingRecipe;
import zone.moddev.mc.skysgrassslabs.world.GrassSlabSmoothingFeature;

@Mod.EventBusSubscriber(modid = SkysGrassSlabs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModRegistryEvents {
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        ModBlocks.registerBlocks(event.getRegistry());
        BuildingBricksCompat.registerLegacyAliasBlocks(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        ModBlocks.registerItems(event.getRegistry());
        BuildingBricksCompat.registerLegacyAliasItems(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerRecipeSerializers(RegistryEvent.Register<IRecipeSerializer<?>> event) {
        event.getRegistry().register(TurfCuttingRecipe.SERIALIZER);
    }

    @SubscribeEvent
    public static void registerFeatures(RegistryEvent.Register<Feature<?>> event) {
        event.getRegistry().register(GrassSlabSmoothingFeature.FEATURE);
    }

    @SubscribeEvent
    public static void remapMissingBlocks(RegistryEvent.MissingMappings<Block> event) {
        LegacyMigrationHandler.remapMissingBlocks(event);
    }

    @SubscribeEvent
    public static void remapMissingItems(RegistryEvent.MissingMappings<Item> event) {
        LegacyMigrationHandler.remapMissingItems(event);
    }

    private ModRegistryEvents() {
    }
}
