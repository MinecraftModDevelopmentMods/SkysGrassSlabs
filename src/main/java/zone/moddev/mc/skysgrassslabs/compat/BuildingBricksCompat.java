package zone.moddev.mc.skysgrassslabs.compat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.init.ModRecipes;

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
    private static final Map<Block, String> buildingBricksBlocks =
            Collections.synchronizedMap(new IdentityHashMap<Block, String>());

    public static void preInit(File configDirectory) {
        if (!Loader.isModLoaded(MOD_ID) || !SkysGrassSlabsConfig.generateGrassSlabs()) {
            return;
        }
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
        grassSlab = Block.REGISTRY.containsKey(GRASS_SLAB_ID)
                ? Block.REGISTRY.getObject(GRASS_SLAB_ID) : null;
        dirtSlab = Block.REGISTRY.containsKey(DIRT_SLAB_ID)
                ? Block.REGISTRY.getObject(DIRT_SLAB_ID) : null;
        buildingBricksBlocks.clear();
        for (ResourceLocation id : Block.REGISTRY.getKeys()) {
            if (MOD_ID.equals(id.getResourceDomain())) {
                buildingBricksBlocks.put(Block.REGISTRY.getObject(id), id.toString());
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

    public static boolean isInstalled() {
        return Loader.isModLoaded(MOD_ID);
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
        return stack != null && grassSlab() != null &&
                stack.getItem() == Item.getItemFromBlock(grassSlab());
    }

    public static void registerBridgeRecipes() {
        if (!isInstalled()) {
            return;
        }
        resolveBlocks();
        if (dirtSlab != null) {
            GameRegistry.addRecipe(new ShapelessOreRecipe(new ItemStack(ModBlocks.GRASS_SLAB),
                    new ItemStack(dirtSlab), ModRecipes.SEED_ORE));
        }
    }

    private BuildingBricksCompat() {
    }
}
