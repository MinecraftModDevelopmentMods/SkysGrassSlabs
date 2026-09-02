package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;

public abstract class LegacySlabBlock extends BlockSlab {
    protected LegacySlabBlock(Material material) {
        super(material);
        setDefaultState(blockState.getBaseState().withProperty(HALF, EnumBlockHalf.BOTTOM));
        setSoundType(SoundType.GROUND);
        setHardness(0.6F);
        setHarvestLevel("shovel", 0);
        setLightOpacity(0);
        useNeighborBrightness = true;
    }

    @Override
    public final boolean isDouble() {
        return false;
    }

    @Override
    public IProperty<?> getVariantProperty() {
        return null;
    }

    @Override
    public Comparable<?> getTypeForItem(ItemStack stack) {
        return null;
    }

    @Override
    public String getUnlocalizedName(int meta) {
        return getUnlocalizedName();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HALF);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HALF) == EnumBlockHalf.BOTTOM ? 1 : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HALF,
                (meta & 1) == 1 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP);
    }
}
