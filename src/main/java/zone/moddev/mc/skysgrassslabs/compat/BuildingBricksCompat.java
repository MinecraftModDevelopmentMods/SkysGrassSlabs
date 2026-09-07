package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

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
        modBus.addGenericListener(Block.class, BuildingBricksCompat::registerAliasBlocks);
        modBus.addGenericListener(Item.class, BuildingBricksCompat::registerAliasItems);
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

    private static void registerAliasBlocks(RegistryEvent.Register<Block> event) {
        if (isInstalled()) {
            return;
        }
        grassSlab = alias(GRASS_SLAB_ID, true);
        dirtSlab = alias(DIRT_SLAB_ID, false);
        historicalGrassSlab = alias(HISTORICAL_GRASS_SLAB_ID, true);
        event.getRegistry().registerAll(grassSlab, dirtSlab, historicalGrassSlab);
        legacyAliasesRegistered = true;
    }

    private static void registerAliasItems(RegistryEvent.Register<Item> event) {
        if (!legacyAliasesRegistered) {
            return;
        }
        event.getRegistry().registerAll(aliasItem(grassSlab), aliasItem(dirtSlab),
                aliasItem(historicalGrassSlab));
    }

    private static Block alias(ResourceLocation id, boolean grass) {
        return new LegacySlabAliasBlock(grass).setRegistryName(id);
    }

    private static Item aliasItem(Block block) {
        return new BlockItem(block, new Item.Properties()).setRegistryName(block.getRegistryName());
    }

    private static Block resolve(Block cached, ResourceLocation id) {
        return cached != null ? cached : ForgeRegistries.BLOCKS.getValue(id);
    }

    private BuildingBricksCompat() {
    }
}
