package zone.moddev.mc.skysgrassslabs.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class TurfBlockItem extends BlockItem {
    public TurfBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (context.getClickedFace() == Direction.UP && state.getBlock() == ModBlocks.DIRT_SLAB) {
            Player player = context.getPlayer();
            if (player == null || !player.mayUseItemAt(
                    context.getClickedPos(), context.getClickedFace(), stack) ||
                    state.getValue(SlabBlock.WATERLOGGED)) {
                return InteractionResult.FAIL;
            }
            BlockState replacement = state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE
                    ? Blocks.GRASS_BLOCK.defaultBlockState() : ModBlocks.grassStateLike(state);
            if (context.getLevel().setBlock(context.getClickedPos(), replacement, 3)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        return super.useOn(context);
    }
}
