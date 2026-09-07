package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.util.RandomSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Biome tinted grass source at carpet height with no dirt state. */
public final class TurfBlock extends CarpetBlock {
    public TurfBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos supportPos = pos.below();

        return level.getBlockState(supportPos).isCollisionShapeFullBlock(level, supportPos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
            boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        dirtifyGrassSupport(level, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
            LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (level instanceof Level concreteLevel) {
            dirtifyGrassSupport(concreteLevel, pos);
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos supportPos = pos.below();

        if (!hasDirtSupport(level, supportPos)) {
            level.destroyBlock(pos, true);
            return;
        }

        if (!SoilLifecycle.canPropagate(state, level, pos)) {
            return;
        }

        GrassSpread.spreadFrom(level, pos, random, supportPos);
    }

    static boolean hasDirtSupport(LevelReader level, BlockPos supportPos) {
        return level.getBlockState(supportPos).is(Blocks.DIRT);
    }

    private static void dirtifyGrassSupport(Level level, BlockPos pos) {
        if (!level.isClientSide && level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 2);
        }
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos,
            Direction direction) {
        return 20;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos,
            Direction direction) {
        return 60;
    }
}
