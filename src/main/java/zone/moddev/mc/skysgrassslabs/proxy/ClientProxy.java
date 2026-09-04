package zone.moddev.mc.skysgrassslabs.proxy;

import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.world.GrassColors;
import net.minecraft.world.biome.BiomeColors;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class ClientProxy {
    public static void register() {
        // Static event subscribers perform client registration at the correct stages.
    }

    public static void registerModels() {
        // Forge resolves item models from each registered item's name.
    }

    public static void registerBlockColors(BlockColors colors) {
        colors.register((state, world, pos, tintIndex) -> world == null || pos == null
                        ? GrassColors.get(0.5D, 1.0D) : BiomeColors.getGrassColor(world, pos),
                ModBlocks.GRASS_SLAB, ModBlocks.TURF);
    }

    public static void registerItemColors(ItemColors colors) {
        colors.register((stack, tintIndex) -> GrassColors.get(0.5D, 1.0D),
                ModBlocks.GRASS_SLAB, ModBlocks.TURF);
    }

    private ClientProxy() {
    }
}
