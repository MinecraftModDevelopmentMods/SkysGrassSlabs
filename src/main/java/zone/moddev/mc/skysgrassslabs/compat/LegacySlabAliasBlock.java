package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import zone.moddev.mc.skysgrassslabs.block.LegacySlabBlock;

/** Hidden deserialization holder for a supported historical slab ID. */
final class LegacySlabAliasBlock extends LegacySlabBlock {
    LegacySlabAliasBlock(net.minecraft.world.level.block.Block fullBlock) {
        super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(fullBlock));
    }

    LegacySlabAliasBlock(boolean grass) {
        super(grass ? MapColor.GRASS : MapColor.DIRT,
                grass ? SoundType.GRASS : SoundType.GRAVEL, 0.6F, false);
    }
}
