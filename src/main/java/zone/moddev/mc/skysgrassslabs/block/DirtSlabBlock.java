package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirtSnowy;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public final class DirtSlabBlock extends LegacySlabBlock {
    public DirtSlabBlock() {
        super(Material.GROUND, SoundType.GROUND, 0.6F, true);
        setDefaultState(getDefaultState().with(BlockDirtSnowy.SNOWY, Boolean.FALSE));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, IBlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(BlockDirtSnowy.SNOWY);
    }

    @Override
    public IBlockState getStateForPlacement(BlockItemUseContext context) {
        IBlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.with(BlockDirtSnowy.SNOWY,
                SnowySlabAppearance.hasNearbySnow(context.getWorld(), context.getPos()));
    }

    @Override
    public IBlockState updatePostPlacement(IBlockState state, EnumFacing facing,
            IBlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        IBlockState updated = super.updatePostPlacement(state, facing, facingState, world, pos,
                facingPos);
        return updated.with(BlockDirtSnowy.SNOWY,
                SnowySlabAppearance.hasNearbySnow(world, pos));
    }

    @Override
    public void tick(IBlockState state, World world, BlockPos pos, Random random) {
        if (!world.isRemote) {
            IBlockState repaired = state.with(BlockDirtSnowy.SNOWY,
                    SnowySlabAppearance.hasNearbySnow(world, pos));
            if (repaired != state) {
                world.setBlockState(pos, repaired, 2);
            }
            GrassSpread.tickDirtSlab(world, pos, repaired, random);
        }
    }
}
