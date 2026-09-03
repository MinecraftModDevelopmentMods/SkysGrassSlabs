package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.ToolType;

/** Common native 1.13 slab behaviour shared by the three permanent slabs. */
public abstract class LegacySlabBlock extends BlockSlab {
    protected LegacySlabBlock(Material material, SoundType sound, float hardness,
            boolean randomTicks) {
        super(properties(material, sound, hardness, randomTicks));
    }

    private static Block.Properties properties(Material material, SoundType sound,
            float hardness, boolean randomTicks) {
        Block.Properties properties = Block.Properties.create(material)
                .hardnessAndResistance(hardness).sound(sound).variableOpacity();
        return randomTicks ? properties.needsRandomTick() : properties;
    }

    @Override
    public ToolType getHarvestTool(IBlockState state) {
        return ToolType.SHOVEL;
    }

    @Override
    public int getHarvestLevel(IBlockState state) {
        return 0;
    }
}
