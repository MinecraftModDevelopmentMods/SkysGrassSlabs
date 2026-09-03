package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import zone.moddev.mc.skysgrassslabs.block.LegacySlabBlock;

/** Hidden deserialization holder for a supported historical slab ID. */
final class LegacySlabAliasBlock extends LegacySlabBlock {
    LegacySlabAliasBlock(boolean grass) {
        super(grass ? Material.GRASS : Material.GROUND,
                grass ? SoundType.PLANT : SoundType.GROUND, 0.6F, false);
    }
}
