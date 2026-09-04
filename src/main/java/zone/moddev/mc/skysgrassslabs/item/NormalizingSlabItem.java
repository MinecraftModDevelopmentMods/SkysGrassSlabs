package zone.moddev.mc.skysgrassslabs.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class NormalizingSlabItem extends BlockItem {
    private final SlabBlock slab;
    private final Block combinedBlock;

    public NormalizingSlabItem(Block block, Block combinedBlock, Item.Properties properties) {
        super(block, properties);
        slab = (SlabBlock) block;
        this.combinedBlock = combinedBlock;
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || context.getItemInHand().isEmpty()) {
            return ActionResultType.FAIL;
        }
        World world = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState state = world.getBlockState(clicked);
        if (state.getBlock() == slab) {
            SlabType type = state.getValue(SlabBlock.TYPE);
            if ((context.getClickedFace() == Direction.UP && type == SlabType.BOTTOM) ||
                    (context.getClickedFace() == Direction.DOWN && type == SlabType.TOP)) {
                return combine(context, clicked);
            }
        }

        BlockPos placement = new BlockItemUseContext(context).getClickedPos();
        if (world.getBlockState(placement).getBlock() == slab) {
            return combine(context, placement);
        }
        return super.useOn(context);
    }

    private ActionResultType combine(ItemUseContext context, BlockPos pos) {
        PlayerEntity player = context.getPlayer();
        World world = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) {
            return ActionResultType.FAIL;
        }
        BlockState combined = combinedBlock.defaultBlockState();
        if (!combined.isFaceSturdy(world, pos, Direction.UP) ||
                !world.setBlock(pos, combined, 11)) {
            return ActionResultType.FAIL;
        }
        SoundType sound = combined.getSoundType(world, pos, player);
        world.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (!player.abilities.instabuild) {
            stack.shrink(1);
        }
        return ActionResultType.SUCCESS;
    }
}
