package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public final class DirtSlabBlock extends LegacySlabBlock {
    public DirtSlabBlock() {
        super(Material.EARTH, SoundType.GROUND, 0.6F, true);
        setDefaultState(getDefaultState().with(SnowyDirtBlock.SNOWY, Boolean.FALSE));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(SnowyDirtBlock.SNOWY);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.with(SnowyDirtBlock.SNOWY,
                SnowySlabAppearance.hasNearbySnow(context.getWorld(), context.getPos()));
    }

    @Override
    public BlockState updatePostPlacement(BlockState state, Direction facing,
            BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        BlockState updated = super.updatePostPlacement(state, facing, facingState, world, pos,
                facingPos);
        return updated.with(SnowyDirtBlock.SNOWY,
                SnowySlabAppearance.hasNearbySnow(world, pos));
    }

    @Override
    public void tick(BlockState state, World world, BlockPos pos, Random random) {
        if (!world.isRemote) {
            BlockState repaired = state.with(SnowyDirtBlock.SNOWY,
                    SnowySlabAppearance.hasNearbySnow(world, pos));
            if (repaired != state) {
                world.setBlockState(pos, repaired, 2);
            }
            GrassSpread.tickDirtSlab(world, pos, repaired, random);
        }
    }
}
