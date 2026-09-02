package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class TurfBlock extends Block {
    public static final AxisAlignedBB TURF_AABB =
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D / 16.0D, 1.0D);

    public TurfBlock() {
        super(Material.CARPET);
        setHardness(0.1F);
        setSoundType(SoundType.CLOTH);
        setTickRandomly(true);
        setLightOpacity(0);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return TURF_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && hasFullSupport(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block changedBlock) {
        if (!world.isRemote && world.getBlockState(pos.down()).getBlock() == Blocks.GRASS) {
            world.setBlockState(pos.down(), Blocks.DIRT.getDefaultState(), 2);
            return;
        }
        if (!hasFullSupport(world, pos)) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (world.isRemote) {
            return;
        }
        BlockPos support = pos.down();
        if (world.getBlockState(support).getBlock() != Blocks.DIRT) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
            return;
        }
        GrassSpread.spreadFrom(world, pos, random, support);
    }

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 20;
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 60;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    private static boolean hasFullSupport(World world, BlockPos pos) {
        return world.getBlockState(pos.down()).isFullCube();
    }
}
