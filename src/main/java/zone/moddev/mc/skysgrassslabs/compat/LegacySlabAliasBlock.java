package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.resources.ResourceKey;

/** Hidden holder used only while historical block and item names deserialize. */
final class LegacySlabAliasBlock extends SlabBlock {
    LegacySlabAliasBlock(boolean grass, ResourceKey<Block> id) {
        this(grass ? Blocks.GRASS_BLOCK : Blocks.DIRT, id);
    }

    LegacySlabAliasBlock(Block source, ResourceKey<Block> id) {
        super(BlockBehaviour.Properties.ofFullCopy(source).noOcclusion().setId(id));
    }
}
