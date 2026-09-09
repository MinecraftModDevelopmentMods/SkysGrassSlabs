package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.util.RandomSource;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Shared vanilla-shaped grass propagation for slabs and turf. */
public final class GrassSpread {
    private static final int SPREAD_ATTEMPTS = 4;

    public static boolean canRemainGrass(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        return level.getMaxLocalRawBrightness(above) >= 4
                || level.getBlockState(above).getLightBlock() < 15;
    }

    public static boolean hasSpreadLight(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState cover = level.getBlockState(above);
        return level.getMaxLocalRawBrightness(above) >= 9
                && cover.getLightBlock() < 15
                && !level.getFluidState(above).is(FluidTags.WATER);
    }

    public static void spreadFrom(ServerLevel level, BlockPos source, RandomSource random,
            @Nullable BlockPos excludedTarget) {
        if (!hasLoadedArea(level, source, 3) || !hasSpreadLight(level, source)) {
            return;
        }
        for (int attempt = 0; attempt < SPREAD_ATTEMPTS; ++attempt) {
            BlockPos target = source.offset(random.nextInt(3) - 1,
                    random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (target.equals(excludedTarget) || !withinBuildHeight(level, target)
                    || !level.isLoaded(target)) {
                continue;
            }
            growTarget(level, target);
        }
    }

    public static void tickDirtSlab(ServerLevel level, BlockPos target, BlockState state,
            RandomSource random) {
        if (!hasLoadedArea(level, target, 3) || !targetIsViable(level, target)) {
            return;
        }
        for (int attempt = 0; attempt < SPREAD_ATTEMPTS; ++attempt) {
            BlockPos source = target.offset(random.nextInt(3) - 1,
                    random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (!withinBuildHeight(level, source) || !level.isLoaded(source)) {
                continue;
            }
            if (isViableSource(level, source)) {
                level.setBlockAndUpdate(target, snowyState(level, target,
                        ModBlocks.grassStateLike(state)));
                return;
            }
        }
    }

    public static boolean growTarget(ServerLevel level, BlockPos target) {
        if (!targetIsViable(level, target)) {
            return false;
        }
        BlockState state = level.getBlockState(target);
        if (state.is(Blocks.DIRT)) {
            return level.setBlockAndUpdate(target, Blocks.GRASS_BLOCK.defaultBlockState());
        }
        if (state.is(ModBlocks.DIRT_SLAB.get())) {
            if (state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
                return level.setBlockAndUpdate(target, Blocks.GRASS_BLOCK.defaultBlockState());
            }
            return level.setBlockAndUpdate(target,
                    snowyState(level, target, ModBlocks.grassStateLike(state)));
        }
        return false;
    }

    public static boolean isViableSource(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!canRemainGrass(level, pos) || !hasSpreadLight(level, pos)) {
            return false;
        }
        if (state.is(Blocks.GRASS_BLOCK)) {
            return true;
        }
        if (state.is(ModBlocks.GRASS_SLAB.get())) {
            return !state.getValue(SlabBlock.WATERLOGGED);
        }
        return state.is(ModBlocks.TURF.get())
                && level.getBlockState(pos.below()).is(Blocks.DIRT);
    }

    private static boolean targetIsViable(ServerLevel level, BlockPos target) {
        BlockState state = level.getBlockState(target);
        boolean dirt = state.is(Blocks.DIRT)
                || state.is(ModBlocks.DIRT_SLAB.get())
                        && !state.getValue(SlabBlock.WATERLOGGED);
        if (!dirt) {
            return false;
        }
        BlockPos above = target.above();
        BlockState cover = level.getBlockState(above);
        if (cover.is(ModBlocks.TURF.get()) || cover.is(ModBlocks.GRASS_SLAB.get())) {
            return false;
        }
        return level.getMaxLocalRawBrightness(above) >= 4
                && cover.getLightBlock() < 15
                && !level.getFluidState(above).is(FluidTags.WATER);
    }

    private static BlockState snowyState(ServerLevel level, BlockPos pos, BlockState state) {
        return state.hasProperty(SnowyDirtBlock.SNOWY)
                ? state.setValue(SnowyDirtBlock.SNOWY,
                        SnowySlabAppearance.hasNearbySnow(level, pos))
                : state;
    }

    private static boolean withinBuildHeight(ServerLevel level, BlockPos pos) {
        return pos.getY() >= level.getMinY() && pos.getY() <= level.getMaxY();
    }

    private static boolean hasLoadedArea(ServerLevel level, BlockPos center, int range) {
        int minX = SectionPos.blockToSectionCoord(center.getX() - range);
        int maxX = SectionPos.blockToSectionCoord(center.getX() + range);
        int minZ = SectionPos.blockToSectionCoord(center.getZ() - range);
        int maxZ = SectionPos.blockToSectionCoord(center.getZ() + range);
        for (int chunkX = minX; chunkX <= maxX; ++chunkX) {
            for (int chunkZ = minZ; chunkZ <= maxZ; ++chunkZ) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private GrassSpread() {
    }
}
