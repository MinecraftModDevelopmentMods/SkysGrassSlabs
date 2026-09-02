package zone.moddev.mc.skysgrassslabs.compat;

import net.minecraft.block.material.Material;
import zone.moddev.mc.skysgrassslabs.block.LegacySlabBlock;

/**
 * Temporary registry holder for a supported slab from a world whose original mod is absent.
 * Loaded aliases are replaced with the corresponding Sky slab before the chunk is used.
 */
final class LegacySlabAliasBlock extends LegacySlabBlock {
    LegacySlabAliasBlock(boolean grass) {
        super(grass ? Material.GRASS : Material.GROUND);
    }
}
