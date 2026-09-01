package zone.moddev.mc.skysgrassslabs.block;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.ToolAction;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Grass slab with target-aware spreading and top-slab vegetation behaviour. */
public final class GrassSlabBlock extends SlabBlock implements BonemealableBlock {
    public static final BooleanProperty SNOWY = SnowyDirtBlock.SNOWY;

    public GrassSlabBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SNOWY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SNOWY);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos()).is(this)) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        BlockState placed = super.getStateForPlacement(context);
        return placed == null ? null : placed.setValue(SNOWY,
                context.getLevel().getBlockState(context.getClickedPos().above()).is(BlockTags.SNOW));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
            LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        BlockState updated = super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
        return direction == Direction.UP && updated.is(this)
                ? updated.setValue(SNOWY, neighbour.is(BlockTags.SNOW)) : updated;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        if (!SoilLifecycle.canRemainGrass(state, level, pos)) {
            if (level.isAreaLoaded(pos, 1)) {
                level.setBlockAndUpdate(pos, SlabTransitions.dirtFor(state));
            }
            return;
        }
        GrassSpread.spreadFrom(level, pos, random, null);
    }

    @Override
    @Nullable
    public BlockState getToolModifiedState(BlockState state, UseOnContext context,
            ToolAction action, boolean simulate) {
        return SlabTransitions.flatten(state, action);
    }

    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos,
            Direction direction, IPlantable plantable) {
        return state.getValue(TYPE) == SlabType.TOP && direction == Direction.UP
                && Blocks.GRASS_BLOCK.canSustainPlant(Blocks.GRASS_BLOCK.defaultBlockState(),
                        level, pos, direction, plantable);
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter level, BlockPos pos, BlockState state,
            boolean clientSide) {
        return state.getValue(TYPE) == SlabType.TOP && level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, Random random, BlockPos pos, BlockState state) {
        if (state.getValue(TYPE) != SlabType.TOP) {
            return;
        }
        BlockPos start = pos.above();
        BlockState vanillaGrass = Blocks.GRASS.defaultBlockState();
        outer:
        for (int attempt = 0; attempt < 128; attempt++) {
            BlockPos target = start;
            for (int walk = 0; walk < attempt / 16; walk++) {
                target = target.offset(random.nextInt(3) - 1,
                        (random.nextInt(3) - 1) * random.nextInt(3) / 2,
                        random.nextInt(3) - 1);
                BlockState support = level.getBlockState(target.below());
                boolean supported = support.is(Blocks.GRASS_BLOCK)
                        || support.is(ModBlocks.GRASS_SLAB.get())
                                && support.getValue(TYPE) == SlabType.TOP;
                if (!supported || level.getBlockState(target).isCollisionShapeFullBlock(level, target)) {
                    continue outer;
                }
            }
            BlockState current = level.getBlockState(target);
            if (current.is(vanillaGrass.getBlock()) && random.nextInt(10) == 0) {
                ((BonemealableBlock) vanillaGrass.getBlock()).performBonemeal(level, random,
                        target, current);
            }
            if (current.isAir()) {
                Holder<PlacedFeature> feature;
                if (random.nextInt(8) == 0) {
                    List<ConfiguredFeature<?, ?>> flowers = level.getBiome(target).value()
                            .getGenerationSettings().getFlowerFeatures();
                    if (flowers.isEmpty()) {
                        continue;
                    }
                    feature = ((RandomPatchConfiguration) flowers.get(0).config()).feature();
                } else {
                    feature = VegetationPlacements.GRASS_BONEMEAL;
                }
                feature.value().place(level, level.getChunkSource().getGenerator(), random, target);
            }
        }
    }
}
