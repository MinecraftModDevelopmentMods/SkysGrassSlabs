package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Lowered dirt-path slab with vanilla-style covered-path decay. */
public final class PathSlabBlock extends SlabBlock {
    private static final VoxelShape BOTTOM_PATH = Block.box(0, 0, 0, 16, 7, 16);
    private static final VoxelShape TOP_PATH = Block.box(0, 8, 0, 16, 15, 16);

    public PathSlabBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return switch (state.getValue(TYPE)) {
            case DOUBLE -> Shapes.block();
            case TOP -> TOP_PATH;
            default -> BOTTOM_PATH;
        };
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos()).is(this)) {
            return Blocks.DIRT_PATH.defaultBlockState();
        }
        BlockState placed = super.getStateForPlacement(context);
        if (placed == null) {
            return null;
        }
        return placed.getValue(WATERLOGGED) ? SlabTransitions.dirtFor(placed) : placed;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state,
            FluidState fluid) {
        boolean placed = super.placeLiquid(level, pos, state, fluid);
        if (placed) {
            level.scheduleTick(pos, this, 1);
        }
        return placed;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
            LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        BlockState updated = super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
        if (updated.getValue(WATERLOGGED)
                || direction == Direction.UP && !canSurvive(updated, level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return updated;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(WATERLOGGED)) {
            return false;
        }
        BlockState above = level.getBlockState(pos.above());
        return !above.getMaterial().isSolid() || above.getBlock() instanceof FenceGateBlock;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        if (!canSurvive(state, level, pos)) {
            BlockState dirt = SlabTransitions.dirtFor(state);
            level.setBlockAndUpdate(pos, Block.pushEntitiesUp(state, dirt, level, pos));
        }
    }
}
