package zone.moddev.mc.skysgrassslabs.block;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraftforge.common.IPlantable;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;

public final class GrassSlabBlock extends LegacySlabBlock implements BonemealableBlock {
    public GrassSlabBlock() {
        super(MapColor.GRASS, SoundType.GRASS, 0.6F, true);
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
                        SnowySlabAppearance.hasNearbySnow(world, pos, facing, facingState));
        if (world instanceof Level) {
            dirtifyGrassSupport((Level) world, pos);
        }
        return updated;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
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
    public boolean onTreeGrow(BlockState state, LevelReader level,
            BiConsumer<BlockPos, BlockState> placeBlock, RandomSource random,
            BlockPos pos, TreeConfiguration config) {
        // Decoration from a new chunk must not turn a saved slab into tree soil.
        return level instanceof WorldGenRegion region
                && region.getLevel().dimension() == Level.OVERWORLD
                && LegacyWorldDataHook.isSavedOverworldChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos pos,
            Direction direction, IPlantable plantable) {
        return direction == Direction.UP && state.getValue(SlabBlock.TYPE) == SlabType.TOP &&
                !state.getValue(SlabBlock.WATERLOGGED) && Blocks.GRASS_BLOCK.canSustainPlant(
                        Blocks.GRASS_BLOCK.defaultBlockState(), world, pos, direction, plantable);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state,
            boolean isClient) {
        return state.getValue(SlabBlock.TYPE) == SlabType.TOP &&
                !state.getValue(SlabBlock.WATERLOGGED) && world.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return state.getValue(SlabBlock.TYPE) == SlabType.TOP && !state.getValue(SlabBlock.WATERLOGGED);
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
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
                    Holder<PlacedFeature> growth;
                    if (random.nextInt(8) == 0) {
                        List<ConfiguredFeature<?, ?>> flowers = world.getBiome(target).value()
                                .getGenerationSettings().getFlowerFeatures();
                        if (flowers.isEmpty()) {
                            break;
                        }
                        growth = ((RandomPatchConfiguration) flowers.get(0).config()).feature();
                    } else {
                        growth = world.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                                .getHolderOrThrow(VegetationPlacements.GRASS_BONEMEAL);
                    }
                    growth.value().place(world, world.getChunkSource().getGenerator(), random, target);
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
        GrassSlabsApi.repairSupport(world, pos);
    }

}
