package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public final class TurfBlock extends Block {
    public static final VoxelShape TURF_SHAPE =
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public TurfBlock() {
        super(Block.Properties.of(Material.CLOTH_DECORATION).strength(0.1F)
                .sound(SoundType.WOOL).randomTicks().noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
            CollisionContext context) {
        return TURF_SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return hasFullSupport(world, pos);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState,
            boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        dirtifyGrassSupport(world, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing,
            BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos) {
        if (world instanceof Level) {
            dirtifyGrassSupport((Level) world, pos);
        }
        return !state.canSurvive(world, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, facing, facingState, world, pos, facingPos);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        BlockPos support = pos.below();
        if (world.getBlockState(support).getBlock() != Blocks.DIRT) {
            world.destroyBlock(pos, true);
            return;
        }
        GrassSpread.spreadFrom(world, pos, random, support);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos,
            Direction face) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos,
            Direction face) {
        return 30;
    }

    private static boolean hasFullSupport(LevelReader world, BlockPos pos) {
        BlockPos support = pos.below();
        return world.getBlockState(support).isCollisionShapeFullBlock(world, support);
    }

    private static void dirtifyGrassSupport(Level world, BlockPos pos) {
        if (!world.isClientSide && world.getBlockState(pos.below()).getBlock() == Blocks.GRASS_BLOCK) {
            world.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 2);
        }
    }
}
