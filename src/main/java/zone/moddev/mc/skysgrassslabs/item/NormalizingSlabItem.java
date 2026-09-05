package zone.moddev.mc.skysgrassslabs.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class NormalizingSlabItem extends BlockItem {
    private final SlabBlock slab;
    private final Block combinedBlock;

    public NormalizingSlabItem(Block block, Block combinedBlock, Item.Properties properties) {
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
        Level world = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState state = world.getBlockState(clicked);
        if (state.getBlock() == slab) {
            SlabType type = state.getValue(SlabBlock.TYPE);
            if ((context.getClickedFace() == Direction.UP && type == SlabType.BOTTOM) ||
                    (context.getClickedFace() == Direction.DOWN && type == SlabType.TOP)) {
                return combine(context, clicked);
            }
        }

        BlockPos placement = new BlockPlaceContext(context).getClickedPos();
        if (world.getBlockState(placement).getBlock() == slab) {
            return combine(context, placement);
        }
        return super.useOn(context);
    }

    private InteractionResult combine(UseOnContext context, BlockPos pos) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        BlockState combined = combinedBlock.defaultBlockState();
        if (!combined.isFaceSturdy(world, pos, Direction.UP) ||
                !world.setBlock(pos, combined, 11)) {
            return InteractionResult.FAIL;
        }
        SoundType sound = combined.getSoundType(world, pos, player);
        world.playSound(player, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
