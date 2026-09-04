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
    public ActionResultType onItemUse(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || context.getItem().isEmpty()) {
            return ActionResultType.FAIL;
        }
        World world = context.getWorld();
        BlockPos clicked = context.getPos();
        BlockState state = world.getBlockState(clicked);
        if (state.getBlock() == slab) {
            SlabType type = state.get(SlabBlock.TYPE);
            if ((context.getFace() == Direction.UP && type == SlabType.BOTTOM) ||
                    (context.getFace() == Direction.DOWN && type == SlabType.TOP)) {
                return combine(context, clicked);
            }
        }

        BlockPos placement = new BlockItemUseContext(context).getPos();
        if (world.getBlockState(placement).getBlock() == slab) {
            return combine(context, placement);
        }
        return super.onItemUse(context);
    }

    private ActionResultType combine(ItemUseContext context, BlockPos pos) {
        PlayerEntity player = context.getPlayer();
        World world = context.getWorld();
        ItemStack stack = context.getItem();
        if (player == null || !player.canPlayerEdit(pos, context.getFace(), stack)) {
            return ActionResultType.FAIL;
        }
        BlockState combined = combinedBlock.getDefaultState();
        if (!combined.func_215682_a(world, pos, player) ||
                !world.setBlockState(pos, combined, 11)) {
            return ActionResultType.FAIL;
        }
        SoundType sound = combined.getSoundType(world, pos, player);
        world.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (!player.abilities.isCreativeMode) {
            stack.shrink(1);
        }
        return ActionResultType.SUCCESS;
    }
}
