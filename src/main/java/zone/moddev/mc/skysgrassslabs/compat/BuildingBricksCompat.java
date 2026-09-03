package zone.moddev.mc.skysgrassslabs.compat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;

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
    private static final Map<Block, String> buildingBricksBlocks =
            Collections.synchronizedMap(new IdentityHashMap<Block, String>());

    public static void preInit(File configDirectory) {
        boolean installed = Loader.isModLoaded(MOD_ID);
        if (!installed) return;
        SkysGrassSlabs.logger.info("BuildingBricks slab replacement is {}",
                SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs()
                        ? "enabled" : "disabled");
        if (!BuildingBricksPolicy.shouldArbitrateWorldgen(installed,
                SkysGrassSlabsConfig.generateGrassSlabs())) return;

        File configFile = new File(configDirectory, "BuildingBricks/general.cfg");
        try {
            if (!BuildingBricksConfigArbitrator.disable(configFile)) {
                SkysGrassSlabs.logger.info("BuildingBricks grass-slab generation is already disabled");
                return;
            }
            SkysGrassSlabs.logger.info(
                    "Disabled BuildingBricks grass-slab generation; Sky's Grass Slabs owns smoothing");
        } catch (IOException | RuntimeException exception) {
            SkysGrassSlabsConfig.suppressSmoothingForThisRun();
            SkysGrassSlabs.logger.error(
                    "Could not disable BuildingBricks grass-slab generation; suppressing Sky smoothing for this run",
                    exception);
        }
    }

    public static void resolveBlocks() {
        grassSlab = ForgeRegistries.BLOCKS.getValue(GRASS_SLAB_ID);
        dirtSlab = ForgeRegistries.BLOCKS.getValue(DIRT_SLAB_ID);
        historicalGrassSlab = ForgeRegistries.BLOCKS.getValue(HISTORICAL_GRASS_SLAB_ID);
        buildingBricksBlocks.clear();
        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
            if (MOD_ID.equals(id.getNamespace())) {
                buildingBricksBlocks.put(ForgeRegistries.BLOCKS.getValue(id), id.toString());
            }
        }
    }

    public static Block grassSlab() {
        if (grassSlab == null) resolveBlocks();
        return grassSlab;
    }

    public static Block dirtSlab() {
        if (dirtSlab == null) resolveBlocks();
        return dirtSlab;
    }

    public static Block historicalGrassSlab() {
        if (historicalGrassSlab == null) resolveBlocks();
        return historicalGrassSlab;
    }

    public static boolean isInstalled() {
        return Loader.isModLoaded(MOD_ID);
    }

    public static boolean hasLegacyAliases() {
        return legacyAliasesRegistered;
    }

    static boolean shouldReplaceSlabs() {
        return BuildingBricksPolicy.shouldReplaceSlabs(isInstalled(),
                SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs());
    }

    public static String buildingBricksBlockId(Block block) {
        return buildingBricksBlocks.get(block);
    }

    public static Map<Integer, String> buildingBricksBlockIdsByNumericId() {
        Map<Integer, String> ids = new HashMap<Integer, String>();
        synchronized (buildingBricksBlocks) {
            for (Map.Entry<Block, String> entry : buildingBricksBlocks.entrySet()) {
                ids.put(Block.getIdFromBlock(entry.getKey()), entry.getValue());
            }
        }
        return ids;
    }

    public static boolean isGrassSlabItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return (grassSlab() != null && stack.getItem() == Item.getItemFromBlock(grassSlab())) ||
                (historicalGrassSlab() != null &&
                stack.getItem() == Item.getItemFromBlock(historicalGrassSlab()));
    }

    static boolean isDirtSlabItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && dirtSlab() != null &&
                stack.getItem() == Item.getItemFromBlock(dirtSlab());
    }

    public static IRecipe bridgeRecipe() {
        return new BuildingBricksDirtSlabRecipe();
    }

    public static void registerLegacyAliasBlocks(IForgeRegistry<Block> registry) {
        if (isInstalled()) return;
        grassSlab = legacyAlias(GRASS_SLAB_ID, true);
        dirtSlab = legacyAlias(DIRT_SLAB_ID, false);
        historicalGrassSlab = legacyAlias(HISTORICAL_GRASS_SLAB_ID, true);
        registry.registerAll(grassSlab, dirtSlab, historicalGrassSlab);
        legacyAliasesRegistered = true;
    }

    public static void registerLegacyAliasItems(IForgeRegistry<Item> registry) {
        if (!legacyAliasesRegistered) return;
        registry.registerAll(aliasItem(grassSlab), aliasItem(dirtSlab),
                aliasItem(historicalGrassSlab));
    }

    private static Block legacyAlias(ResourceLocation id, boolean grass) {
        LegacySlabAliasBlock block = new LegacySlabAliasBlock(grass);
        block.setRegistryName(id).setTranslationKey(
                SkysGrassSlabs.MOD_ID + ".legacy_" + id.getPath());
        return block;
    }

    private static Item aliasItem(Block block) {
        ItemBlock item = new ItemBlock(block) {
            @Override
            public int getMetadata(int damage) {
                return damage & 1;
            }
        };
        return item.setRegistryName(block.getRegistryName());
    }

    private BuildingBricksCompat() {
    }
}
