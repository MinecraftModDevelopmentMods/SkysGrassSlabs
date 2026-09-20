package zone.moddev.mc.skysgrassslabs.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockTransformers;

/** Target-native shovel detection shared by crafting and slab flattening. */
public final class ShovelSupport {
    private ShovelSupport() {
    }

    public static boolean isShovel(ItemStack stack) {
        Holder<BlockTransformer> transformer = stack.get(DataComponents.BLOCK_TRANSFORMER);
        return transformer != null && transformer.is(BlockTransformers.SHOVEL);
    }
}
