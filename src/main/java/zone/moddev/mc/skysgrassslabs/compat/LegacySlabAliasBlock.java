package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import zone.moddev.mc.skysgrassslabs.block.LegacySlabBlock;

/** Hidden deserialization holder for a supported historical slab ID. */
final class LegacySlabAliasBlock extends LegacySlabBlock {
    LegacySlabAliasBlock(boolean grass) {
        super(grass ? Material.GRASS : Material.DIRT,
                grass ? SoundType.GRASS : SoundType.GRAVEL, 0.6F, false);
    }
}
