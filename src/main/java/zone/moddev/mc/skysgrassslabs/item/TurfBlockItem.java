package zone.moddev.mc.skysgrassslabs.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class TurfBlockItem extends BlockItem {
    public TurfBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        ItemStack stack = context.getItem();
        BlockState state = context.getWorld().getBlockState(context.getPos());
        if (context.getFace() == Direction.UP && state.getBlock() == ModBlocks.DIRT_SLAB) {
            PlayerEntity player = context.getPlayer();
            if (player == null || !player.canPlayerEdit(context.getPos(), context.getFace(), stack) ||
                    state.get(SlabBlock.WATERLOGGED)) {
                return ActionResultType.FAIL;
            }
            BlockState replacement = state.get(SlabBlock.TYPE) == SlabType.DOUBLE
                    ? Blocks.GRASS_BLOCK.getDefaultState() : ModBlocks.grassStateLike(state);
            if (context.getWorld().setBlockState(context.getPos(), replacement, 3)) {
                if (!player.abilities.isCreativeMode) {
                    stack.shrink(1);
                }
                return ActionResultType.SUCCESS;
            }
            return ActionResultType.FAIL;
        }
        return super.onItemUse(context);
    }
}
