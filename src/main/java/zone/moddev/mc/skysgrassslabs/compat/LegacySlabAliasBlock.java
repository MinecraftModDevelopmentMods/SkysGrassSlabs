package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.block.material.Material;
import zone.moddev.mc.skysgrassslabs.block.LegacySlabBlock;

/** Keeps one historical identity intact until loaded storage is converted. */
final class LegacySlabAliasBlock extends LegacySlabBlock {
    LegacySlabAliasBlock(boolean grass) {
        super(grass ? Material.GRASS : Material.GROUND);
    }
}
