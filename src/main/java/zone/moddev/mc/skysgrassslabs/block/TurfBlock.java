package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public final class TurfBlock extends Block {
    public static final VoxelShape TURF_SHAPE =
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public TurfBlock() {
        super(Block.Properties.of(Material.CLOTH_DECORATION).strength(0.1F)
                .sound(SoundType.WOOL).randomTicks().noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos,
            ISelectionContext context) {
        return TURF_SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, IWorldReader world, BlockPos pos) {
        return hasFullSupport(world, pos);
    }

    @Override
    public void onPlace(BlockState state, World world, BlockPos pos, BlockState oldState,
            boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        dirtifyGrassSupport(world, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing,
            BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        if (world instanceof World) {
            dirtifyGrassSupport((World) world, pos);
        }
        return !state.canSurvive(world, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, facing, facingState, world, pos, facingPos);
    }

    @Override
    public void tick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BlockPos support = pos.below();
        if (world.getBlockState(support).getBlock() != Blocks.DIRT) {
            world.destroyBlock(pos, true);
            return;
        }
        GrassSpread.spreadFrom(world, pos, random, support);
    }

    @Override
    public int getFlammability(BlockState state, IBlockReader world, BlockPos pos,
            Direction face) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, IBlockReader world, BlockPos pos,
            Direction face) {
        return 30;
    }

    private static boolean hasFullSupport(IWorldReader world, BlockPos pos) {
        BlockPos support = pos.below();
        return world.getBlockState(support).isCollisionShapeFullBlock(world, support);
    }

    private static void dirtifyGrassSupport(World world, BlockPos pos) {
        if (!world.isClientSide && world.getBlockState(pos.below()).getBlock() == Blocks.GRASS_BLOCK) {
            world.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 2);
        }
    }
}
