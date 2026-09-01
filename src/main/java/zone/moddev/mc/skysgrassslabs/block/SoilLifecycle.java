package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LayerLightEngine;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Vanilla-equivalent light and water rules with slab-aware identities. */
final class SoilLifecycle {
    private SoilLifecycle() {
    }

    static boolean canRemainGrass(BlockState state, LevelReader level, BlockPos pos) {
        if (state.hasProperty(SlabBlock.WATERLOGGED) && state.getValue(SlabBlock.WATERLOGGED)) {
            return false;
        }
        BlockPos above = pos.above();
        BlockState cover = level.getBlockState(above);
        if (cover.is(Blocks.SNOW) && cover.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        if (cover.getFluidState().getAmount() == 8) {
            return false;
        }
        // Slabs and carpet-height turf expose complete horizontal faces to the
        // shape-based light test. Grass survival only cares about the cover
        // above, not the source block's own collision shape.
        BlockState lightState = state.is(ModBlocks.GRASS_SLAB.get())
                || state.is(ModBlocks.TURF.get())
                ? Blocks.AIR.defaultBlockState() : state;
        int blocked = LayerLightEngine.getLightBlockInto(level, lightState, pos, cover, above,
                Direction.UP, cover.getLightBlock(level, above));
        return blocked < level.getMaxLightLevel();
    }

    static boolean canPropagate(BlockState futureGrass, LevelReader level, BlockPos pos) {
        return canRemainGrass(futureGrass, level, pos)
                && !level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    static boolean isViableGrassSource(BlockState state, LevelReader level, BlockPos pos) {
        if (state.is(ModBlocks.TURF.get())) {
            return TurfBlock.hasDirtSupport(level, pos.below())
                    && canPropagate(state, level, pos);
        }
        return (state.is(Blocks.GRASS_BLOCK) || state.is(ModBlocks.GRASS_SLAB.get()))
                && canRemainGrass(state, level, pos);
    }
}
