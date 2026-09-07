package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/** Writes equivalent migration states without running gameplay placement callbacks. */
final class ChunkMigrationAccess {
    static void setBlockState(LevelChunk chunk, BlockPos pos, BlockState state) {
        int sectionIndex = chunk.getSectionIndex(pos.getY());
        LevelChunkSection section = chunk.getSection(sectionIndex);
        section.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15,
                state, false);
    }

    private ChunkMigrationAccess() {
    }
}
