package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.IFluidState;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class PathSlabBlock extends LegacySlabBlock {
    public static final VoxelShape BOTTOM_PATH_SHAPE =
            Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);
    public static final VoxelShape TOP_PATH_SHAPE =
            Block.makeCuboidShape(0.0D, 8.0D, 0.0D, 16.0D, 15.0D, 16.0D);

    public PathSlabBlock() {
        super(Material.EARTH, SoundType.PLANT, 0.65F, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos,
            ISelectionContext context) {
        return state.get(SlabBlock.TYPE) == SlabType.TOP
                ? TOP_PATH_SHAPE : BOTTOM_PATH_SHAPE;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState,
            boolean isMoving) {
        super.onBlockAdded(state, world, pos, oldState, isMoving);
        if (!world.isRemote && (state.get(SlabBlock.WATERLOGGED) ||
                world.getBlockState(pos.up()).getMaterial().isSolid())) {
            world.setBlockState(pos, ModBlocks.dirtStateLike(state), 3);
        }
    }

    @Override
    public BlockState updatePostPlacement(BlockState state, Direction facing,
            BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        BlockState updated = super.updatePostPlacement(state, facing, facingState, world, pos,
                facingPos);
        if (!world.isRemote() && (updated.get(SlabBlock.WATERLOGGED) ||
                world.getBlockState(pos.up()).getMaterial().isSolid())) {
            return ModBlocks.dirtStateLike(updated);
        }
        return updated;
    }

    @Override
    public boolean receiveFluid(IWorld world, BlockPos pos, BlockState state,
            IFluidState fluidState) {
        boolean received = super.receiveFluid(world, pos, state, fluidState);
        if (received && !world.isRemote()) {
            BlockState wet = state.with(SlabBlock.WATERLOGGED, Boolean.TRUE);
            world.setBlockState(pos, ModBlocks.dirtStateLike(wet), 3);
        }
        return received;
    }
}
