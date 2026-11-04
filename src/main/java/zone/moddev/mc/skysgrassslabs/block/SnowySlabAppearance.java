package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

final class SnowySlabAppearance {
    static boolean hasNearbySnow(BlockGetter world, BlockPos pos) {
        if (isSnow(world.getBlockState(pos.above()).getBlock())) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isSnow(world.getBlockState(pos.relative(direction)).getBlock())) {
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
