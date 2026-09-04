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
    public ActionResultType useOn(ItemUseContext context) {
        ItemStack stack = context.getItemInHand();
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (context.getClickedFace() == Direction.UP && state.getBlock() == ModBlocks.DIRT_SLAB) {
            PlayerEntity player = context.getPlayer();
            if (player == null || !player.mayUseItemAt(
                    context.getClickedPos(), context.getClickedFace(), stack) ||
                    state.getValue(SlabBlock.WATERLOGGED)) {
                return ActionResultType.FAIL;
            }
            BlockState replacement = state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE
                    ? Blocks.GRASS_BLOCK.defaultBlockState() : ModBlocks.grassStateLike(state);
            if (context.getLevel().setBlock(context.getClickedPos(), replacement, 3)) {
                if (!player.abilities.instabuild) {
                    stack.shrink(1);
                }
                return ActionResultType.SUCCESS;
            }
            return ActionResultType.FAIL;
        }
        return super.useOn(context);
    }
}
