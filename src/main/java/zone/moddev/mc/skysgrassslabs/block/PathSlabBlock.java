package zone.moddev.mc.skysgrassslabs.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class PathSlabBlock extends LegacySlabBlock {
    public static final AxisAlignedBB BOTTOM_PATH_AABB =
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 7.0D / 16.0D, 1.0D);
    public static final AxisAlignedBB TOP_PATH_AABB =
            new AxisAlignedBB(0.0D, 8.0D / 16.0D, 0.0D, 1.0D, 15.0D / 16.0D, 1.0D);

    public PathSlabBlock() {
        super(Material.GROUND);
        setHardness(0.65F);
        setSoundType(SoundType.PLANT);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(HALF) == EnumBlockHalf.TOP ? TOP_PATH_AABB : BOTTOM_PATH_AABB;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block changedBlock) {
        super.neighborChanged(state, world, pos, changedBlock);
        if (!world.isRemote && world.getBlockState(pos.up()).getMaterial().isSolid()) {
            world.setBlockState(pos, ModBlocks.DIRT_SLAB.getDefaultState()
                    .withProperty(HALF, state.getValue(HALF)), 3);
        }
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        List<ItemStack> drops = new ArrayList<ItemStack>();
        drops.add(new ItemStack(ModBlocks.DIRT_SLAB));
        return drops;
    }
}
