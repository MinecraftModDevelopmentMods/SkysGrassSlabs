package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Computes the visual snow cap from nearby vanilla snow. */
final class SnowySlabAppearance {
    static boolean hasNearbySnow(BlockGetter level, BlockPos pos) {
        if (isSnow(level.getBlockState(pos.above()).getBlock())) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isSnow(level.getBlockState(pos.relative(direction)).getBlock())) {
                return true;
            }
        }
        return false;
    }

    static boolean hasNearbySnow(BlockGetter level, BlockPos pos, Direction changedDirection,
            BlockState changedState) {
        if (changedDirection == Direction.UP) {
            if (isSnow(changedState.getBlock())) {
                return true;
            }
        } else if (isSnow(level.getBlockState(pos.above()).getBlock())) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbour = direction == changedDirection
                    ? changedState : level.getBlockState(pos.relative(direction));
            if (isSnow(neighbour.getBlock())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSnow(Block block) {
        return block == Blocks.SNOW || block == Blocks.SNOW_BLOCK;
    }

    private SnowySlabAppearance() {
    }
}
