package zone.moddev.mc.skysgrassslabs.world;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Small native plants may give way to a transition in a newly generated chunk. */
final class SmoothingFoliage {
    static boolean isPlant(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.SHORT_GRASS || block == Blocks.FERN || block == Blocks.DEAD_BUSH
                || block == Blocks.DANDELION || block == Blocks.POPPY || block == Blocks.BLUE_ORCHID
                || block == Blocks.ALLIUM || block == Blocks.AZURE_BLUET || block == Blocks.RED_TULIP
                || block == Blocks.ORANGE_TULIP || block == Blocks.WHITE_TULIP || block == Blocks.PINK_TULIP
                || block == Blocks.OXEYE_DAISY || block == Blocks.CORNFLOWER
                || block == Blocks.LILY_OF_THE_VALLEY || block == Blocks.TALL_GRASS
                || block == Blocks.LARGE_FERN || block == Blocks.SUNFLOWER || block == Blocks.LILAC
                || block == Blocks.ROSE_BUSH || block == Blocks.PEONY;
    }

    static boolean canReplace(LevelAccessor world, ChunkAccess owner, BlockPos position) {
        BlockState state = owner.getBlockState(position);
        if (state.hasBlockEntity() || owner.getBlockEntity(position) != null
                || owner.getBlockEntityNbt(position) != null
                || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.isAir()) {
            return true;
        }
        if (!isPlant(state)) {
            return false;
        }
        if (!(state.getBlock() instanceof DoublePlantBlock)) {
            return true;
        }
        if (position.getY() >= owner.getMaxY() || state.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.LOWER) {
            return false;
        }

        BlockState upper = owner.getBlockState(position.above());
        return upper.getBlock() == state.getBlock()
                && upper.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER
                && owner.getBlockEntity(position.above()) == null
                && owner.getBlockEntityNbt(position.above()) == null;
    }

    private SmoothingFoliage() {
    }
}
