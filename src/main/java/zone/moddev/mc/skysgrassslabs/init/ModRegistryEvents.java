package zone.moddev.mc.skysgrassslabs.init;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraft.world.item.CreativeModeTabs;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.recipe.TurfCuttingRecipe;

@Mod.EventBusSubscriber(modid = SkysGrassSlabs.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModRegistryEvents {
    @SubscribeEvent
    public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.BLOCKS)) {
            ModBlocks.registerBlocks(ForgeRegistries.BLOCKS);
            BuildingBricksCompat.registerLegacyAliasBlocks(ForgeRegistries.BLOCKS);
        } else if (event.getRegistryKey().equals(ForgeRegistries.Keys.ITEMS)) {
            ModBlocks.registerItems(ForgeRegistries.ITEMS);
            BuildingBricksCompat.registerLegacyAliasItems(ForgeRegistries.ITEMS);
        } else if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_SERIALIZERS)) {
            event.register(ForgeRegistries.Keys.RECIPE_SERIALIZERS,
                    TurfCuttingRecipe.SERIALIZER_ID, () -> TurfCuttingRecipe.SERIALIZER);
        }
    }

    @SubscribeEvent
    public static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.BUILDING_BLOCKS.equals(event.getTabKey())) {
            event.accept(ModBlocks.DIRT_SLAB);
            event.accept(ModBlocks.GRASS_SLAB);
            event.accept(ModBlocks.PATH_SLAB);
            event.accept(ModBlocks.TURF);
        }
    }

    private ModRegistryEvents() {
    }
}
