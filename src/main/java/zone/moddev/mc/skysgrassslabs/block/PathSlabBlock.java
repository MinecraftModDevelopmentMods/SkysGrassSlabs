package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.IFluidState;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.math.BlockPos;
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
        super(Material.GROUND, SoundType.PLANT, 0.65F, false);
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockReader world, BlockPos pos) {
        return state.get(BlockSlab.TYPE) == SlabType.TOP
                ? TOP_PATH_SHAPE : BOTTOM_PATH_SHAPE;
    }

    @Override
    public void onBlockAdded(IBlockState state, World world, BlockPos pos, IBlockState oldState) {
        super.onBlockAdded(state, world, pos, oldState);
        if (!world.isRemote && (state.get(BlockSlab.WATERLOGGED) ||
                world.getBlockState(pos.up()).getMaterial().isSolid())) {
            world.setBlockState(pos, ModBlocks.dirtStateLike(state), 3);
        }
    }

    @Override
    public IBlockState updatePostPlacement(IBlockState state, EnumFacing facing,
            IBlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        IBlockState updated = super.updatePostPlacement(state, facing, facingState, world, pos,
                facingPos);
        if (!world.isRemote() && (updated.get(BlockSlab.WATERLOGGED) ||
                world.getBlockState(pos.up()).getMaterial().isSolid())) {
            return ModBlocks.dirtStateLike(updated);
        }
        return updated;
    }

    @Override
    public boolean receiveFluid(IWorld world, BlockPos pos, IBlockState state,
            IFluidState fluidState) {
        boolean received = super.receiveFluid(world, pos, state, fluidState);
        if (received && !world.isRemote()) {
            IBlockState wet = state.with(BlockSlab.WATERLOGGED, Boolean.TRUE);
            world.setBlockState(pos, ModBlocks.dirtStateLike(wet), 3);
        }
        return received;
    }

    @Override
    public IItemProvider getItemDropped(IBlockState state, World world, BlockPos pos, int fortune) {
        return ModBlocks.DIRT_SLAB;
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }
}
