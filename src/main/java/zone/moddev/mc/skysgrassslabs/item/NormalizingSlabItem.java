package zone.moddev.mc.skysgrassslabs.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class NormalizingSlabItem extends ItemBlock {
    private final BlockSlab slab;
    private final Block combinedBlock;

    public NormalizingSlabItem(Block block, Block combinedBlock, Item.Properties properties) {
        super(block, properties);
        slab = (BlockSlab) block;
        this.combinedBlock = combinedBlock;
    }

    @Override
    public EnumActionResult onItemUse(ItemUseContext context) {
        EntityPlayer player = context.getPlayer();
        if (player == null || context.getItem().isEmpty()) {
            return EnumActionResult.FAIL;
        }
        World world = context.getWorld();
        BlockPos clicked = context.getPos();
        IBlockState state = world.getBlockState(clicked);
        if (state.getBlock() == slab) {
            SlabType type = state.get(BlockSlab.TYPE);
            if ((context.getFace() == EnumFacing.UP && type == SlabType.BOTTOM) ||
                    (context.getFace() == EnumFacing.DOWN && type == SlabType.TOP)) {
                return combine(context, clicked);
            }
        }

        BlockPos placement = new BlockItemUseContext(context).getPos();
        if (world.getBlockState(placement).getBlock() == slab) {
            return combine(context, placement);
        }
        return super.onItemUse(context);
    }

    private EnumActionResult combine(ItemUseContext context, BlockPos pos) {
        EntityPlayer player = context.getPlayer();
        World world = context.getWorld();
        ItemStack stack = context.getItem();
        if (player == null || !player.canPlayerEdit(pos, context.getFace(), stack)) {
            return EnumActionResult.FAIL;
        }
        IBlockState combined = combinedBlock.getDefaultState();
        if (!world.checkNoEntityCollision(combined, pos) ||
                !world.setBlockState(pos, combined, 11)) {
            return EnumActionResult.FAIL;
        }
        SoundType sound = combined.getSoundType(world, pos, player);
        world.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (!player.abilities.isCreativeMode) {
            stack.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }
}
