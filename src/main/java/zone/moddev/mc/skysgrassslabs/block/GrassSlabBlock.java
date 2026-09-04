package zone.moddev.mc.skysgrassslabs.block;

import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.FlowersFeature;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.IPlantable;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class GrassSlabBlock extends LegacySlabBlock implements IGrowable {
    public GrassSlabBlock() {
        super(Material.GRASS, SoundType.GRASS, 0.6F, true);
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
    public void onPlace(BlockState state, World world, BlockPos pos, BlockState oldState,
            boolean isMoving) {
        super.onPlace(state, world, pos, oldState, isMoving);
        dirtifyGrassSupport(world, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing,
            BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        BlockState updated = super.updateShape(state, facing, facingState, world, pos,
                facingPos).setValue(SnowyDirtBlock.SNOWY,
                        SnowySlabAppearance.hasNearbySnow(world, pos));
        if (world instanceof World) {
            dirtifyGrassSupport((World) world, pos);
        }
        return updated;
    }

    @Override
    public void tick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        dirtifyGrassSupport(world, pos);
        if (state.getValue(SlabBlock.WATERLOGGED) || !GrassSpread.canRemainGrass(world, pos)) {
            world.setBlock(pos, ModBlocks.dirtStateLike(state), 3);
            return;
        }
        BlockState repaired = state.setValue(SnowyDirtBlock.SNOWY,
                SnowySlabAppearance.hasNearbySnow(world, pos));
        if (repaired != state) {
            world.setBlock(pos, repaired, 2);
        }
        GrassSpread.spreadFrom(world, pos, random, pos.below());
    }

    @Override
    public boolean canSustainPlant(BlockState state, IBlockReader world, BlockPos pos,
            Direction direction, IPlantable plantable) {
        return direction == Direction.UP && state.getValue(SlabBlock.TYPE) == SlabType.TOP &&
                !state.getValue(SlabBlock.WATERLOGGED) && Blocks.GRASS_BLOCK.canSustainPlant(
                        Blocks.GRASS_BLOCK.defaultBlockState(), world, pos, direction, plantable);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockReader world, BlockPos pos, BlockState state,
            boolean isClient) {
        return state.getValue(SlabBlock.TYPE) == SlabType.TOP &&
                !state.getValue(SlabBlock.WATERLOGGED) && world.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPos pos, BlockState state) {
        return state.getValue(SlabBlock.TYPE) == SlabType.TOP && !state.getValue(SlabBlock.WATERLOGGED);
    }

    @Override
    public void performBonemeal(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        if (!isBonemealSuccess(world, random, pos, state)) {
            return;
        }
        BlockPos start = pos.above();
        BlockState grassPlant = Blocks.GRASS.defaultBlockState();
        for (int attempt = 0; attempt < 128; ++attempt) {
            BlockPos target = start;
            int walk = 0;
            while (true) {
                if (walk >= attempt / 16) {
                    BlockState targetState = world.getBlockState(target);
                    if (targetState.getBlock() == grassPlant.getBlock() &&
                            random.nextInt(10) == 0) {
                        ((IGrowable) grassPlant.getBlock()).performBonemeal(
                                world, random, target, targetState);
                    }
                    if (!targetState.isAir()) {
                        break;
                    }
                    BlockState growth;
                    if (random.nextInt(8) == 0) {
                        List<ConfiguredFeature<?, ?>> flowers =
                                world.getBiome(target).getGenerationSettings().getFlowerFeatures();
                        if (flowers.isEmpty()) {
                            break;
                        }
                        ConfiguredFeature<?, ?> flower = flowers.get(0);
                        growth = ((FlowersFeature) flower.feature())
                                .getRandomFlower(random, target, flower.config());
                    } else {
                        growth = grassPlant;
                    }
                    if (growth.canSurvive(world, target)) {
                        world.setBlock(target, growth, 3);
                    }
                    break;
                }
                target = target.offset(random.nextInt(3) - 1,
                        (random.nextInt(3) - 1) * random.nextInt(3) / 2,
                        random.nextInt(3) - 1);
                BlockState support = world.getBlockState(target.below());
                boolean suitable = support.getBlock() == Blocks.GRASS_BLOCK ||
                        support.getBlock() == this &&
                                support.getValue(SlabBlock.TYPE) == SlabType.TOP;
                if (!suitable || world.getBlockState(target)
                        .isCollisionShapeFullBlock(world, target)) {
                    break;
                }
                ++walk;
            }
        }
    }

    private static void dirtifyGrassSupport(World world, BlockPos pos) {
        if (!world.isClientSide && world.getBlockState(pos.below()).getBlock() == Blocks.GRASS_BLOCK) {
            world.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 2);
        }
    }
}
