package zone.moddev.mc.skysgrassslabs.block;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Lossless state transitions shared by soil slabs. */
final class SlabTransitions {
    private SlabTransitions() {
    }

    @Nullable
    static BlockState flatten(BlockState state, ToolAction action) {
        if (action != ToolActions.SHOVEL_FLATTEN || state.getValue(SlabBlock.WATERLOGGED)) {
            return null;
        }
        SlabType type = state.getValue(SlabBlock.TYPE);
        if (type == SlabType.DOUBLE) {
            return Blocks.DIRT_PATH.defaultBlockState();
        }
        return ModBlocks.PATH_SLAB.get().defaultBlockState()
                .setValue(SlabBlock.TYPE, type)
                .setValue(SlabBlock.WATERLOGGED, false);
    }

    static BlockState dirtFor(BlockState state) {
        SlabType type = state.getValue(SlabBlock.TYPE);
        if (type == SlabType.DOUBLE) {
            return Blocks.DIRT.defaultBlockState();
        }
        return ModBlocks.DIRT_SLAB.get().defaultBlockState()
                .setValue(SlabBlock.TYPE, type)
                .setValue(SlabBlock.WATERLOGGED, state.getValue(SlabBlock.WATERLOGGED));
    }

    static BlockState grassFor(BlockState state) {
        SlabType type = state.getValue(SlabBlock.TYPE);
        if (type == SlabType.DOUBLE) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        return ModBlocks.GRASS_SLAB.get().defaultBlockState()
                .setValue(SlabBlock.TYPE, type)
                .setValue(SlabBlock.WATERLOGGED, false);
    }
}
