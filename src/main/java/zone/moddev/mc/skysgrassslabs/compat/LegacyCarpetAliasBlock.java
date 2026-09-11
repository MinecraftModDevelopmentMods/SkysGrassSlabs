package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.resources.ResourceKey;

/** Hidden holder that preserves historical grass carpet on unsupported substrates. */
final class LegacyCarpetAliasBlock extends CarpetBlock {
    LegacyCarpetAliasBlock(ResourceKey<Block> id) {
        super(BlockBehaviour.Properties.ofFullCopy(Blocks.CARPET.green()).setId(id));
    }
}
