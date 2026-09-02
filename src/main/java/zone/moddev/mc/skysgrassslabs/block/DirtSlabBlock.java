package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class DirtSlabBlock extends LegacySlabBlock {
    public DirtSlabBlock() {
        super(Material.GROUND);
        setDefaultState(blockState.getBaseState()
                .withProperty(HALF, EnumBlockHalf.BOTTOM)
                .withProperty(BlockDirt.SNOWY, Boolean.FALSE));
        setTickRandomly(true);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty<?>[] {HALF, BlockDirt.SNOWY});
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.withProperty(BlockDirt.SNOWY, SnowySlabAppearance.hasNearbySnow(world, pos));
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!world.isRemote) {
            GrassSpread.tickDirtSlab(world, pos, state, random);
        }
    }
}
