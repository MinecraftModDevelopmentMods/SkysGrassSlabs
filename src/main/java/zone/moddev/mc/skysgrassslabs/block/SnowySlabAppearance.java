package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

final class SnowySlabAppearance {
    static boolean hasNearbySnow(IBlockReader world, BlockPos pos) {
        if (isSnow(world.getBlockState(pos.up()).getBlock())) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isSnow(world.getBlockState(pos.offset(direction)).getBlock())) {
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
