package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.util.RandomSource;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Shared vanilla-shaped grass propagation for slabs and turf. */
public final class GrassSpread {
    private static final int SPREAD_ATTEMPTS = 4;

    public static boolean canRemainGrass(Level level, BlockPos pos) {
        return SoilLifecycle.canRemainGrass(level.getBlockState(pos), level, pos);
    }

    public static boolean hasSpreadLight(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState cover = level.getBlockState(above);
        return level.getMaxLocalRawBrightness(above) >= 9
                && cover.getLightBlock(level, above) < level.getMaxLightLevel()
                && !level.getFluidState(above).is(FluidTags.WATER);
    }

    public static void spreadFrom(Level level, BlockPos source, RandomSource random,
            @Nullable BlockPos excludedTarget) {
        if (!level.isAreaLoaded(source, 3) || !hasSpreadLight(level, source)) {
            return;
        }
        for (int attempt = 0; attempt < SPREAD_ATTEMPTS; ++attempt) {
            BlockPos target = source.offset(random.nextInt(3) - 1,
                    random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (target.equals(excludedTarget) || !withinBuildHeight(level, target)
                    || !level.isLoaded(target)) {
                continue;
            }
            growTarget(level, target, level.getBlockState(source));
        }
    }

    public static void tickDirtSlab(Level level, BlockPos target, BlockState state,
            RandomSource random) {
        if (!level.isAreaLoaded(target, 3) || !targetIsViable(level, target)) {
            return;
        }
        for (int attempt = 0; attempt < SPREAD_ATTEMPTS; ++attempt) {
            BlockPos source = target.offset(random.nextInt(3) - 1,
                    random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (!withinBuildHeight(level, source) || !level.isLoaded(source)) {
                continue;
            }
            if (isViableSource(level, source)) {
                BlockState grass = GrassSlabsApi.grassFor(state, level.getBlockState(source));
                if (grass != null) {
                    level.setBlockAndUpdate(target, snowyState(level, target, grass));
                    return;
                }
            }
        }
    }

    public static boolean growTarget(Level level, BlockPos target) {
        return growTarget(level, target, Blocks.GRASS_BLOCK.defaultBlockState());
    }

    private static boolean growTarget(Level level, BlockPos target, BlockState source) {
        if (!targetIsViable(level, target)) {
            return false;
        }
        BlockState state = level.getBlockState(target);
        if (state.is(ModBlocks.DIRT_SLAB.get())
                && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
            return level.setBlockAndUpdate(target, Blocks.GRASS_BLOCK.defaultBlockState());
        }

        BlockState grass = GrassSlabsApi.grassFor(state, source);
        if (grass == null && state.is(Blocks.DIRT)) {
            grass = Blocks.GRASS_BLOCK.defaultBlockState();
        }
        return grass != null && level.setBlockAndUpdate(target, snowyState(level, target, grass));
    }

    public static boolean isViableSource(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!canRemainGrass(level, pos) || !hasSpreadLight(level, pos)) {
            return false;
        }
        if (state.is(Blocks.GRASS_BLOCK) || GrassSlabsApi.isSpreadingGrassForm(state)) {
            return true;
        }
        if (state.is(ModBlocks.GRASS_SLAB.get())) {
            return !state.getValue(SlabBlock.WATERLOGGED);
        }
        return state.is(ModBlocks.TURF.get())
                && level.getBlockState(pos.below()).is(Blocks.DIRT);
    }

    private static boolean targetIsViable(Level level, BlockPos target) {
        BlockState state = level.getBlockState(target);
        boolean dirt = state.is(Blocks.DIRT) || GrassSlabsApi.grassFor(state) != null
                || state.is(ModBlocks.DIRT_SLAB.get());
        if (!dirt || !state.getFluidState().isEmpty()) {
            return false;
        }
        BlockPos above = target.above();
        BlockState cover = level.getBlockState(above);
        if (cover.is(ModBlocks.TURF.get()) || GrassSlabsApi.isGrassForm(cover)
                && (!state.isCollisionShapeFullBlock(level, target)
                        || GrassSlabsApi.coversSupport(cover, level, above))) {
            return false;
        }
        return level.getMaxLocalRawBrightness(above) >= 4
                && cover.getLightBlock(level, above) < level.getMaxLightLevel()
                && !level.getFluidState(above).is(FluidTags.WATER);
    }

    private static BlockState snowyState(Level level, BlockPos pos, BlockState state) {
        return state.hasProperty(SnowyDirtBlock.SNOWY)
                ? state.setValue(SnowyDirtBlock.SNOWY,
                        SnowySlabAppearance.hasNearbySnow(level, pos))
                : state;
    }

    private static boolean withinBuildHeight(Level level, BlockPos pos) {
        return pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight();
    }

    private GrassSpread() {
    }
}
