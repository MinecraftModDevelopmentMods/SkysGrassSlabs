package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Biome-tinted carpet-height grass source with no dirt state. */
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
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
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
