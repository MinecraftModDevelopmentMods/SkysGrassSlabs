package zone.moddev.mc.skysgrassslabs.legacytest;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Registers the saved source shapes for the installed-mod policy checks. */
@Mod(LegacyContentFixture.MOD_ID)
public final class LegacyContentFixture {
    public static final String MOD_ID = "grassslabs";
    private static final Map<ResourceLocation, Block> BLOCKS = new LinkedHashMap<>();

    public LegacyContentFixture() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::register);
    }

    private void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.BLOCKS)) {
            addSlab("grass_slab", Blocks.GRASS_BLOCK);
            addSlab("dirt_slab", Blocks.DIRT);
            addSlab("dirt_path_slab", Blocks.DIRT_PATH);
            addCarpet("grass_carpet", Blocks.GRASS_BLOCK);
            addStairs("grass_stairs", Blocks.GRASS_BLOCK);
            addStairs("dirt_stairs", Blocks.DIRT);
            addStairs("dirt_path_stairs", Blocks.DIRT_PATH);
            addCarpet("dirt_carpet", Blocks.DIRT);
            addCarpet("dirt_path_carpet", Blocks.DIRT_PATH);
            addSlab("mycelium_slab", Blocks.MYCELIUM);
            addStairs("mycelium_stairs", Blocks.MYCELIUM);
            addCarpet("mycelium_carpet", Blocks.MYCELIUM);
            BLOCKS.forEach((name, block) -> event.register(
                    ForgeRegistries.Keys.BLOCKS, name, () -> block));
        } else if (event.getRegistryKey().equals(ForgeRegistries.Keys.ITEMS)) {
            BLOCKS.forEach((name, block) -> event.register(ForgeRegistries.Keys.ITEMS,
                    name, () -> new BlockItem(block, new Item.Properties())));
        }
    }

    private static void addSlab(String name, Block material) {
        BLOCKS.put(new ResourceLocation(MOD_ID, name),
                new SlabBlock(BlockBehaviour.Properties.copy(material).noOcclusion()));
    }

    private static void addCarpet(String name, Block material) {
        BLOCKS.put(new ResourceLocation(MOD_ID, name), new FixtureCarpet(material));
    }

    private static void addStairs(String name, Block material) {
        BLOCKS.put(new ResourceLocation(MOD_ID, name), new FixtureStairs(material));
    }

    private static final class FixtureCarpet extends CarpetBlock {
        private FixtureCarpet(Block material) {
            super(BlockBehaviour.Properties.copy(material).noOcclusion());
        }
    }

    private static final class FixtureStairs extends StairBlock {
        private FixtureStairs(Block material) {
            super(material.defaultBlockState(), BlockBehaviour.Properties.copy(material).noOcclusion());
        }
    }
}
