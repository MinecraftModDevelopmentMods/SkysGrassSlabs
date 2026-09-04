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
import net.minecraft.world.server.ServerWorld;

public final class DirtSlabBlock extends LegacySlabBlock {
    public DirtSlabBlock() {
        super(Material.DIRT, SoundType.GRAVEL, 0.6F, true);
        registerDefaultState(defaultBlockState().setValue(SnowyDirtBlock.SNOWY, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SnowyDirtBlock.SNOWY);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(SnowyDirtBlock.SNOWY,
                SnowySlabAppearance.hasNearbySnow(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing,
            BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        BlockState updated = super.updateShape(state, facing, facingState, world, pos,
                facingPos);
        return updated.setValue(SnowyDirtBlock.SNOWY,
                SnowySlabAppearance.hasNearbySnow(world, pos));
    }

    @Override
    public void tick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BlockState repaired = state.setValue(SnowyDirtBlock.SNOWY,
                SnowySlabAppearance.hasNearbySnow(world, pos));
        if (repaired != state) {
            world.setBlock(pos, repaired, 2);
        }
        GrassSpread.tickDirtSlab(world, pos, repaired, random);
    }
}
