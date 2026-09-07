package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Hidden holder used only while historical block and item names deserialize. */
final class LegacySlabAliasBlock extends SlabBlock {
    LegacySlabAliasBlock(boolean grass) {
        super(BlockBehaviour.Properties.copy(grass ? Blocks.GRASS_BLOCK : Blocks.DIRT)
                .noOcclusion());
    }
}
