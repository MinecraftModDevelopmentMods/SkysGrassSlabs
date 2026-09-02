package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

final class SnowySlabAppearance {
    static boolean hasNearbySnow(IBlockAccess world, BlockPos pos) {
        if (isSnow(world.getBlockState(pos.up()).getBlock())) {
            return true;
        }
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            if (isSnow(world.getBlockState(pos.offset(facing)).getBlock())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSnow(Block block) {
        return block == Blocks.SNOW || block == Blocks.SNOW_LAYER;
    }

    private SnowySlabAppearance() {
    }
}
