package zone.moddev.mc.skysgrassslabs.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Slab item that normalizes a matching pair to its vanilla full block. */
public final class NormalizingSlabItem extends BlockItem {
    private final SlabBlock slab;
    private final Block combinedBlock;

    public NormalizingSlabItem(Block block, Block combinedBlock, Properties properties) {
        super(block, properties);
        slab = (SlabBlock) block;
        this.combinedBlock = combinedBlock;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getItemInHand().isEmpty()) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        if (state.is(slab)) {
            SlabType type = state.getValue(SlabBlock.TYPE);
            if (context.getClickedFace() == Direction.UP && type == SlabType.BOTTOM
                    || context.getClickedFace() == Direction.DOWN && type == SlabType.TOP) {
                return combine(context, clicked);
            }
        }
        BlockPos placement = new BlockPlaceContext(context).getClickedPos();
        return level.getBlockState(placement).is(slab)
                ? combine(context, placement) : super.useOn(context);
    }

    private InteractionResult combine(UseOnContext context, BlockPos pos) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        BlockState combined = combinedBlock.defaultBlockState();
        if (!level.setBlock(pos, combined, Block.UPDATE_ALL_IMMEDIATE)) {
            return InteractionResult.FAIL;
        }
        SoundType sound = combined.getSoundType(level, pos, player);
        level.playSound(player, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
