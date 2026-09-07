package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Narrow compatibility boundary for supported historical slab IDs. */
public final class BuildingBricksCompat {
    public static final String MOD_ID = "buildingbricks";
    public static final ResourceLocation GRASS_SLAB_ID =
            new ResourceLocation(MOD_ID, "grass_slab");
    public static final ResourceLocation DIRT_SLAB_ID =
            new ResourceLocation(MOD_ID, "dirt_slab");
    public static final ResourceLocation HISTORICAL_GRASS_SLAB_ID =
            new ResourceLocation("buildingbrickscompatvanilla", "grass_slab");

    private static Block grassSlab;
    private static Block dirtSlab;
    private static Block historicalGrassSlab;
    private static boolean legacyAliasesRegistered;

    public static void register(IEventBus modBus) {
        modBus.addListener(BuildingBricksCompat::registerAliases);
    }

    public static boolean isInstalled() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean hasLegacyAliases() {
        return legacyAliasesRegistered;
    }

    public static Block grassSlab() {
        return resolve(grassSlab, GRASS_SLAB_ID);
    }

    public static Block dirtSlab() {
        return resolve(dirtSlab, DIRT_SLAB_ID);
    }

    public static Block historicalGrassSlab() {
        return resolve(historicalGrassSlab, HISTORICAL_GRASS_SLAB_ID);
    }

    public static boolean isGrassSlabItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Block grass = grassSlab();
        Block historical = historicalGrassSlab();
        return grass != null && stack.is(grass.asItem())
                || historical != null && stack.is(historical.asItem());
    }

    static boolean isDirtSlabItem(ItemStack stack) {
        Block dirt = dirtSlab();
        return stack != null && !stack.isEmpty() && dirt != null && stack.is(dirt.asItem());
    }

    private static void registerAliases(RegisterEvent event) {
        if (isInstalled()) {
            return;
        }
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            event.register(Registries.BLOCK, GRASS_SLAB_ID,
                    () -> grassSlab = alias(true));
            event.register(Registries.BLOCK, DIRT_SLAB_ID,
                    () -> dirtSlab = alias(false));
            event.register(Registries.BLOCK, HISTORICAL_GRASS_SLAB_ID,
                    () -> historicalGrassSlab = alias(true));
            legacyAliasesRegistered = true;
        } else if (event.getRegistryKey().equals(Registries.ITEM) && legacyAliasesRegistered) {
            event.register(Registries.ITEM, GRASS_SLAB_ID, () -> aliasItem(grassSlab));
            event.register(Registries.ITEM, DIRT_SLAB_ID, () -> aliasItem(dirtSlab));
            event.register(Registries.ITEM, HISTORICAL_GRASS_SLAB_ID,
                    () -> aliasItem(historicalGrassSlab));
        }
    }

    private static Block alias(boolean grass) {
        return new LegacySlabAliasBlock(grass);
    }

    private static Item aliasItem(Block block) {
        return new BlockItem(block, new Item.Properties());
    }

    private static Block resolve(Block cached, ResourceLocation id) {
        return cached != null ? cached : ForgeRegistries.BLOCKS.getValue(id);
    }

    private BuildingBricksCompat() {
    }
}
