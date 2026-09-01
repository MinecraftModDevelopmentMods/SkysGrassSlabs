package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;

/** Dirt slab that can find nearby grass sources vanilla grass cannot target. */
public final class DirtSlabBlock extends SlabBlock {
    public DirtSlabBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        if (state.getValue(WATERLOGGED) || !level.isAreaLoaded(pos, 3)
                || level.getMaxLocalRawBrightness(pos.above()) < 9) {

            return;
        }

        BlockState future = SlabTransitions.grassFor(state);

        if (!SoilLifecycle.canPropagate(future, level, pos)) {
            return;
        }

        for (int attempt = 0; attempt < 4; attempt++) {
            BlockPos sourcePos = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            BlockState source = level.getBlockState(sourcePos);

            if (SoilLifecycle.isViableGrassSource(source, level, sourcePos)) {
                level.setBlockAndUpdate(pos, future);
                return;
            }
        }
    }

    @Override
    @Nullable
    public BlockState getToolModifiedState(BlockState state, UseOnContext context,
            ToolAction action, boolean simulate) {
        return SlabTransitions.flatten(state, action);
    }
}
