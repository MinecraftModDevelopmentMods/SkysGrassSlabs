package zone.moddev.mc.skysgrassslabs.init;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.BuildingBricksCompat;
import zone.moddev.mc.skysgrassslabs.compat.LegacyMigrationHandler;

@Mod.EventBusSubscriber(modid = SkysGrassSlabs.MOD_ID)
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
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        ModRecipes.register(event.getRegistry());
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
