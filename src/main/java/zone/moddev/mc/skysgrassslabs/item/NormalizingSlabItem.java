package zone.moddev.mc.skysgrassslabs.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class NormalizingSlabItem extends ItemBlock {
    private final BlockSlab slab;
    private final Block combinedBlock;

    public NormalizingSlabItem(Block block, Block combinedBlock) {
        super(block);
        this.slab = (BlockSlab) block;
        this.combinedBlock = combinedBlock;
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (stack == null || stack.stackSize == 0 ||
                !player.canPlayerEdit(pos.offset(facing), facing, stack)) {
            return EnumActionResult.FAIL;
        }

        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == slab) {
            BlockSlab.EnumBlockHalf half = state.getValue(BlockSlab.HALF);
            if ((facing == EnumFacing.UP && half == BlockSlab.EnumBlockHalf.BOTTOM) ||
                    (facing == EnumFacing.DOWN && half == BlockSlab.EnumBlockHalf.TOP)) {
                return combine(stack, player, world, pos);
            }
        }

        BlockPos adjacent = pos.offset(facing);
        if (world.getBlockState(adjacent).getBlock() == slab) {
            return combine(stack, player, world, adjacent);
        }
        return super.onItemUse(stack, player, world, pos, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side,
            EntityPlayer player, ItemStack stack) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == slab) {
            BlockSlab.EnumBlockHalf half = state.getValue(BlockSlab.HALF);
            if ((side == EnumFacing.UP && half == BlockSlab.EnumBlockHalf.BOTTOM) ||
                    (side == EnumFacing.DOWN && half == BlockSlab.EnumBlockHalf.TOP)) {
                return true;
            }
        }
        if (world.getBlockState(pos.offset(side)).getBlock() == slab) {
            return true;
        }
        return super.canPlaceBlockOnSide(world, pos, side, player, stack);
    }

    private EnumActionResult combine(ItemStack stack, EntityPlayer player, World world, BlockPos pos) {
        IBlockState combined = combinedBlock.getDefaultState();
        AxisAlignedBB box = combined.getCollisionBoundingBox(world, pos);
        if (box == Block.NULL_AABB || !world.checkNoEntityCollision(box.offset(pos))) {
            return EnumActionResult.FAIL;
        }
        if (!world.setBlockState(pos, combined, 11)) {
            return EnumActionResult.FAIL;
        }
        SoundType sound = combinedBlock.getSoundType(combined, world, pos, player);
        world.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (!player.capabilities.isCreativeMode) {
            --stack.stackSize;
        }
        return EnumActionResult.SUCCESS;
    }
}
