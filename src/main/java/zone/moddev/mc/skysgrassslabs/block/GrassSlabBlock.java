package zone.moddev.mc.skysgrassslabs.block;

import java.util.List;
import java.util.Random;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.AbstractFlowerFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.IPlantable;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class GrassSlabBlock extends LegacySlabBlock implements BonemealableBlock {
    public GrassSlabBlock() {
        super(Material.GRASS, SoundType.GRASS, 0.6F, true);
        registerDefaultState(defaultBlockState().setValue(SnowyDirtBlock.SNOWY, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SnowyDirtBlock.SNOWY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(SnowyDirtBlock.SNOWY,
                SnowySlabAppearance.hasNearbySnow(context.getLevel(), context.getClickedPos()));
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
        BlockState updated = super.updateShape(state, facing, facingState, world, pos,
                facingPos).setValue(SnowyDirtBlock.SNOWY,
                        SnowySlabAppearance.hasNearbySnow(world, pos));
        if (world instanceof Level) {
            dirtifyGrassSupport((Level) world, pos);
        }
        return updated;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
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
    public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos pos,
            Direction direction, IPlantable plantable) {
        return direction == Direction.UP && state.getValue(SlabBlock.TYPE) == SlabType.TOP &&
                !state.getValue(SlabBlock.WATERLOGGED) && Blocks.GRASS_BLOCK.canSustainPlant(
                        Blocks.GRASS_BLOCK.defaultBlockState(), world, pos, direction, plantable);
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter world, BlockPos pos, BlockState state,
            boolean isClient) {
        return state.getValue(SlabBlock.TYPE) == SlabType.TOP &&
                !state.getValue(SlabBlock.WATERLOGGED) && world.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level world, Random random, BlockPos pos, BlockState state) {
        return state.getValue(SlabBlock.TYPE) == SlabType.TOP && !state.getValue(SlabBlock.WATERLOGGED);
    }

    @Override
    public void performBonemeal(ServerLevel world, Random random, BlockPos pos, BlockState state) {
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
                        ((BonemealableBlock) grassPlant.getBlock()).performBonemeal(
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
                        growth = randomFlower(random, target, flower);
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

    private static void dirtifyGrassSupport(Level world, BlockPos pos) {
        if (!world.isClientSide && world.getBlockState(pos.below()).getBlock() == Blocks.GRASS_BLOCK) {
            world.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 2);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState randomFlower(Random random, BlockPos pos,
            ConfiguredFeature<?, ?> configured) {
        AbstractFlowerFeature feature = (AbstractFlowerFeature) configured.feature;
        return feature.getRandomFlower(random, pos,
                (FeatureConfiguration) configured.config());
    }
}
