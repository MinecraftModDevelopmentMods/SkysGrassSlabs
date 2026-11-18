package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;

/** Computes the visual snow cap from nearby snow and exposed cold ground. */
final class SnowySlabAppearance {
    static boolean hasNearbySnow(BlockGetter level, BlockPos pos) {
        return hasNearbySnow(level, pos, null, null);
    }

    static boolean hasNearbySnow(BlockGetter level, BlockPos pos, Direction changedDirection,
            BlockState changedState) {
        BlockState above = changedDirection == Direction.UP ? changedState : readState(level, pos.above());
        if (above.is(BlockTags.SNOW)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbourPos = pos.relative(direction);
            BlockState neighbour = direction == changedDirection
                    ? changedState : readState(level, neighbourPos);
            // A higher terrace carries its snow one block above the piece's horizontal neighbour.
            if (neighbour.is(BlockTags.SNOW) || neighbour.isCollisionShapeFullBlock(level, neighbourPos)
                    && readState(level, neighbourPos.above()).is(BlockTags.SNOW)) {
                return true;
            }
        }
        // Partial tops cannot support vanilla snow layers, but exposed cold ground still needs its snow cap.
        return level instanceof LevelReader reader
                && reader.getBiome(pos).value().coldEnoughToSnow(pos, reader.getSeaLevel())
                && reader.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 1
                && reader.getBrightness(LightLayer.BLOCK, pos.above()) < 10;
    }

    private static BlockState readState(BlockGetter level, BlockPos pos) {
        if (level instanceof Level world && !world.isLoaded(pos)
                || level instanceof WorldGenRegion region && !region.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return level.getBlockState(pos);
    }

    private SnowySlabAppearance() {
    }
}
