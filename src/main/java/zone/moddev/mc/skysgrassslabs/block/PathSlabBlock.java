package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class PathSlabBlock extends LegacySlabBlock {
    public static final VoxelShape BOTTOM_PATH_SHAPE =
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);
    public static final VoxelShape TOP_PATH_SHAPE =
            Block.box(0.0D, 8.0D, 0.0D, 16.0D, 15.0D, 16.0D);

    public PathSlabBlock() {
        super(Material.DIRT, SoundType.GRASS, 0.65F, false, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
            CollisionContext context) {
        return state.getValue(SlabBlock.TYPE) == SlabType.TOP
                ? TOP_PATH_SHAPE : BOTTOM_PATH_SHAPE;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState,
            boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        if (!world.isClientSide && (state.getValue(SlabBlock.WATERLOGGED) ||
                world.getBlockState(pos.above()).getMaterial().isSolid())) {
            world.setBlock(pos, ModBlocks.dirtStateLike(state), 3);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing,
            BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos) {
        BlockState updated = super.updateShape(state, facing, facingState, world, pos,
                facingPos);
        if (!world.isClientSide() && (updated.getValue(SlabBlock.WATERLOGGED) ||
                world.getBlockState(pos.above()).getMaterial().isSolid())) {
            return ModBlocks.dirtStateLike(updated);
        }
        return updated;
    }

    @Override
    public boolean placeLiquid(LevelAccessor world, BlockPos pos, BlockState state,
            FluidState fluidState) {
        boolean received = super.placeLiquid(world, pos, state, fluidState);
        if (received && !world.isClientSide()) {
            BlockState wet = state.setValue(SlabBlock.WATERLOGGED, Boolean.TRUE);
            world.setBlock(pos, ModBlocks.dirtStateLike(wet), 3);
        }
        return received;
    }
}
