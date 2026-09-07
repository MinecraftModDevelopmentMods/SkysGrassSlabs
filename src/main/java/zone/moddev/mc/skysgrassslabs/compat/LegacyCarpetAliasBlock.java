package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Hidden holder that preserves historical grass carpet on unsupported substrates. */
final class LegacyCarpetAliasBlock extends CarpetBlock {
    LegacyCarpetAliasBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.GREEN_CARPET));
    }
}
