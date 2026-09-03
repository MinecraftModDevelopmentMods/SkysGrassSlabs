package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReaderBase;
import net.minecraft.world.World;

public final class TurfBlock extends Block {
    public static final VoxelShape TURF_SHAPE =
            Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public TurfBlock() {
        super(Block.Properties.create(Material.CARPET).hardnessAndResistance(0.1F)
                .sound(SoundType.CLOTH).needsRandomTick().variableOpacity());
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockReader world, BlockPos pos) {
        return TURF_SHAPE;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockReader world, IBlockState state, BlockPos pos,
            EnumFacing face) {
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean isValidPosition(IBlockState state, IWorldReaderBase world, BlockPos pos) {
        return hasFullSupport(world, pos);
    }

    @Override
    public void onBlockAdded(IBlockState state, World world, BlockPos pos, IBlockState oldState) {
        super.onBlockAdded(state, world, pos, oldState);
        dirtifyGrassSupport(world, pos);
    }

    @Override
    public IBlockState updatePostPlacement(IBlockState state, EnumFacing facing,
            IBlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        if (world instanceof World) {
            dirtifyGrassSupport((World) world, pos);
        }
        return !state.isValidPosition(world, pos)
                ? Blocks.AIR.getDefaultState()
                : super.updatePostPlacement(state, facing, facingState, world, pos, facingPos);
    }

    @Override
    public void tick(IBlockState state, World world, BlockPos pos, Random random) {
        if (world.isRemote) {
            return;
        }
        BlockPos support = pos.down();
        if (world.getBlockState(support).getBlock() != Blocks.DIRT) {
            world.destroyBlock(pos, true);
            return;
        }
        GrassSpread.spreadFrom(world, pos, random, support);
    }

    @Override
    public int getFlammability(IBlockState state, IBlockReader world, BlockPos pos,
            EnumFacing face) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(IBlockState state, IBlockReader world, BlockPos pos,
            EnumFacing face) {
        return 30;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    private static boolean hasFullSupport(IWorldReaderBase world, BlockPos pos) {
        return world.getBlockState(pos.down()).isFullCube();
    }

    private static void dirtifyGrassSupport(World world, BlockPos pos) {
        if (!world.isRemote && world.getBlockState(pos.down()).getBlock() == Blocks.GRASS_BLOCK) {
            world.setBlockState(pos.down(), Blocks.DIRT.getDefaultState(), 2);
        }
    }
}
