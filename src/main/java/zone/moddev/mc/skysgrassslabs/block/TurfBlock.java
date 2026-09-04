package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public final class TurfBlock extends Block {
    public static final VoxelShape TURF_SHAPE =
            Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public TurfBlock() {
        super(Block.Properties.create(Material.CARPET).hardnessAndResistance(0.1F)
                .sound(SoundType.CLOTH).tickRandomly().variableOpacity());
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos,
            ISelectionContext context) {
        return TURF_SHAPE;
    }

    @Override
    public boolean isValidPosition(BlockState state, IWorldReader world, BlockPos pos) {
        return hasFullSupport(world, pos);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState,
            boolean isMoving) {
        super.onBlockAdded(state, world, pos, oldState, isMoving);
        dirtifyGrassSupport(world, pos);
    }

    @Override
    public BlockState updatePostPlacement(BlockState state, Direction facing,
            BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        if (world instanceof World) {
            dirtifyGrassSupport((World) world, pos);
        }
        return !state.isValidPosition(world, pos)
                ? Blocks.AIR.getDefaultState()
                : super.updatePostPlacement(state, facing, facingState, world, pos, facingPos);
    }

    @Override
    public void tick(BlockState state, World world, BlockPos pos, Random random) {
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
    public int getFlammability(BlockState state, IBlockReader world, BlockPos pos,
            Direction face) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, IBlockReader world, BlockPos pos,
            Direction face) {
        return 30;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    private static boolean hasFullSupport(IWorldReader world, BlockPos pos) {
        BlockPos support = pos.down();
        return world.getBlockState(support).isNormalCube(world, support);
    }

    private static void dirtifyGrassSupport(World world, BlockPos pos) {
        if (!world.isRemote && world.getBlockState(pos.down()).getBlock() == Blocks.GRASS_BLOCK) {
            world.setBlockState(pos.down(), Blocks.DIRT.getDefaultState(), 2);
        }
    }
}
