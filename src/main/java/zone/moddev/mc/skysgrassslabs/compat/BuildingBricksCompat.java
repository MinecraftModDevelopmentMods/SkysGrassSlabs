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
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;

public final class BuildingBricksCompat {
    public static final String MOD_ID = "buildingbricks";
    public static final ResourceLocation GRASS_SLAB_ID = new ResourceLocation(MOD_ID, "grass_slab");
    public static final ResourceLocation DIRT_SLAB_ID = new ResourceLocation(MOD_ID, "dirt_slab");
    public static final ResourceLocation HISTORICAL_GRASS_SLAB_ID = new ResourceLocation("buildingbrickscompatvanilla",
            "grass_slab");

    private static Block grassSlab;
    private static Block dirtSlab;
    private static Block historicalGrassSlab;
    private static boolean legacyAliasesRegistered;
    private static boolean bridgeRecipesRegistered;
    private static final Map<Block, String> buildingBricksBlocks = Collections
            .synchronizedMap(new IdentityHashMap<Block, String>());

    public static void preInit(File configDirectory) {
        boolean installed = Loader.isModLoaded(MOD_ID);

        if (!installed) {
            registerLegacyAliases();
            return;
        }

        SkysGrassSlabs.logger.info("BuildingBricks slab replacement is {}",
                SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs() ? "enabled" : "disabled");

        if (!BuildingBricksPolicy.shouldArbitrateWorldgen(installed, SkysGrassSlabsConfig.isSmoothingActive())) {
            return;
        }

        File configFile = new File(configDirectory, "BuildingBricks/general.cfg");

        try {
            if (!BuildingBricksConfigArbitrator.disable(configFile)) {
                SkysGrassSlabs.logger.info("BuildingBricks grass-slab generation is already disabled");
                return;
            }

            SkysGrassSlabs.logger
                    .info("Disabled BuildingBricks grass-slab generation; Sky's Grass Slabs owns smoothing");
        } catch (IOException | RuntimeException exception) {
            SkysGrassSlabsConfig.suppressSmoothingForThisRun();
            SkysGrassSlabs.logger.error(
                    "Could not disable BuildingBricks grass-slab generation; suppressing Sky smoothing for this run",
                    exception);
        }
    }

    public static void resolveBlocks() {
        grassSlab = Block.REGISTRY.containsKey(GRASS_SLAB_ID) ? Block.REGISTRY.getObject(GRASS_SLAB_ID) : null;
        dirtSlab = Block.REGISTRY.containsKey(DIRT_SLAB_ID) ? Block.REGISTRY.getObject(DIRT_SLAB_ID) : null;
        historicalGrassSlab = Block.REGISTRY.containsKey(HISTORICAL_GRASS_SLAB_ID)
                ? Block.REGISTRY.getObject(HISTORICAL_GRASS_SLAB_ID)
                : null;
        buildingBricksBlocks.clear();

        for (ResourceLocation registryId : Block.REGISTRY.getKeys()) {
            if (MOD_ID.equals(registryId.getResourceDomain())) {
                buildingBricksBlocks.put(Block.REGISTRY.getObject(registryId), registryId.toString());
            }
        }
    }

    public static Block grassSlab() {
        if (grassSlab == null) {
            resolveBlocks();
        }

        return grassSlab;
    }

    public static Block dirtSlab() {
        if (dirtSlab == null) {
            resolveBlocks();
        }

        return dirtSlab;
    }

    public static boolean isInstalled() {
        return Loader.isModLoaded(MOD_ID);
    }

    public static Block historicalGrassSlab() {
        if (historicalGrassSlab == null) {
            resolveBlocks();
        }

        return historicalGrassSlab;
    }

    public static boolean hasLegacyAliases() {
        return legacyAliasesRegistered;
    }

    public static Block[] legacyAliases() {
        return legacyAliasesRegistered ? new Block[] { grassSlab(), dirtSlab(), historicalGrassSlab() } : new Block[0];
    }

    private static void registerLegacyAliases() {
        registerLegacyAlias(GRASS_SLAB_ID, true);
        registerLegacyAlias(DIRT_SLAB_ID, false);
        registerLegacyAlias(HISTORICAL_GRASS_SLAB_ID, true);
        legacyAliasesRegistered = true;
        resolveBlocks();
        SkysGrassSlabs.logger.info("Legacy slab recovery enabled for {}, {} and {}. "
                + "Saved blocks and items will be converted as they load.",
                GRASS_SLAB_ID, DIRT_SLAB_ID, HISTORICAL_GRASS_SLAB_ID);
    }

    private static void registerLegacyAlias(ResourceLocation registryId, boolean grass) {
        LegacySlabAliasBlock block = new LegacySlabAliasBlock(grass);

        LegacyRegistryWarnings.setName(block, registryId);
        block.setUnlocalizedName(SkysGrassSlabs.MOD_ID + ".legacy_" + registryId.getResourcePath());

        ItemBlock item = new ItemBlock(block) {
            @Override
            public int getMetadata(int damage) {
                return damage & 1;
            }
        };

        item.setHasSubtypes(true);
        LegacyRegistryWarnings.setName(item, registryId);
        GameRegistry.register(block);
        GameRegistry.register(item);
    }

    static boolean shouldReplaceSlabs() {
        return BuildingBricksPolicy.shouldReplaceSlabs(isInstalled(),
                SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs());
    }

    public static String buildingBricksBlockId(Block block) {
        return buildingBricksBlocks.get(block);
    }

    public static Map<Integer, String> buildingBricksBlockIdsByNumericId() {
        Map<Integer, String> registryIds = new HashMap<Integer, String>();

        synchronized (buildingBricksBlocks) {
            for (Map.Entry<Block, String> entry : buildingBricksBlocks.entrySet()) {
                registryIds.put(Block.getIdFromBlock(entry.getKey()), entry.getValue());
            }
        }

        return registryIds;
    }

    public static boolean isGrassSlabItem(ItemStack stack) {
        return stack != null && ((grassSlab() != null && stack.getItem() == Item.getItemFromBlock(grassSlab()))
                || (historicalGrassSlab() != null && stack.getItem() == Item.getItemFromBlock(historicalGrassSlab())));
    }

    static boolean isDirtSlabItem(ItemStack stack) {
        return stack != null && dirtSlab() != null && stack.getItem() == Item.getItemFromBlock(dirtSlab());
    }

    public static void registerBridgeRecipes() {
        if (!isInstalled() || bridgeRecipesRegistered) {
            return;
        }

        GameRegistry.addRecipe(new BuildingBricksDirtSlabRecipe());
        bridgeRecipesRegistered = true;
    }

    private BuildingBricksCompat() {
    }
}
