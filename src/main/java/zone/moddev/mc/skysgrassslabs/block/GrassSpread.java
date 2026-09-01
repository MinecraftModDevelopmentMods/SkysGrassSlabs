package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Shared vanilla shaped grass propagation for slabs and turf. */
final class GrassSpread {
    private GrassSpread() {
    }

    static void spreadFrom(ServerLevel level, BlockPos sourcePos, Random random,
            @Nullable BlockPos excludedTarget) {

        if (!level.isAreaLoaded(sourcePos, 3)
                || level.getMaxLocalRawBrightness(sourcePos.above()) < 9) {
            return;
        }

        for (int attempt = 0; attempt < 4; attempt++) {
            BlockPos targetPos = sourcePos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);

            if (targetPos.equals(excludedTarget)) {
                continue;
            }

            BlockState target = level.getBlockState(targetPos);
            BlockState future = null;

            if (target.is(Blocks.DIRT)) {
                future = Blocks.GRASS_BLOCK.defaultBlockState();
            } else if (target.is(ModBlocks.DIRT_SLAB.get()) && !target.getValue(SlabBlock.WATERLOGGED)) {
                future = SlabTransitions.grassFor(target);
            }

            if (future != null && SoilLifecycle.canPropagate(future, level, targetPos)) {
                if (future.hasProperty(GrassSlabBlock.SNOWY)) {
                    future = future.setValue(GrassSlabBlock.SNOWY,
                            level.getBlockState(targetPos.above()).is(BlockTags.SNOW));
                }

                level.setBlockAndUpdate(targetPos, future);
            }
        }
    }
}
