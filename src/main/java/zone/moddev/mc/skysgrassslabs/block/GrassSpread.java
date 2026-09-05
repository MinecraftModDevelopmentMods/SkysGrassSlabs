package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class GrassSpread {
    private static final int SPREAD_ATTEMPTS = 4;

    public static boolean canRemainGrass(Level world, BlockPos pos) {
        BlockPos above = pos.above();
        return world.getMaxLocalRawBrightness(above) >= 4 ||
                world.getBlockState(above).getLightBlock(world, above) < world.getMaxLightLevel();
    }

    public static boolean hasSpreadLight(Level world, BlockPos pos) {
        BlockPos above = pos.above();
        return world.getMaxLocalRawBrightness(above) >= 4 &&
                world.getBlockState(above).getLightBlock(world, above) < world.getMaxLightLevel() &&
                !world.getFluidState(above).is(FluidTags.WATER);
    }

    public static void spreadFrom(Level world, BlockPos source, Random random,
            BlockPos excludedTarget) {
        if (!world.isAreaLoaded(source, 3) || !hasSpreadLight(world, source)) {
            return;
        }
        for (int attempt = 0; attempt < SPREAD_ATTEMPTS; ++attempt) {
            BlockPos target = source.offset(random.nextInt(3) - 1,
                    random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (target.equals(excludedTarget)) {
                continue;
            }
            if (target.getY() < 0 || target.getY() >= 256 || !world.isLoaded(target)) {
                return;
            }
            growTarget(world, target);
        }
    }

    public static void tickDirtSlab(Level world, BlockPos target, BlockState state,
            Random random) {
        if (!world.isAreaLoaded(target, 3) || !targetIsViable(world, target)) {
            return;
        }
        for (int attempt = 0; attempt < SPREAD_ATTEMPTS; ++attempt) {
            BlockPos source = target.offset(random.nextInt(3) - 1,
                    random.nextInt(5) - 1, random.nextInt(3) - 1);
            if (source.getY() < 0 || source.getY() >= 256 || !world.isLoaded(source)) {
                return;
            }
            if (isViableSource(world, source)) {
                world.setBlock(target, ModBlocks.grassStateLike(state), 3);
                return;
            }
        }
    }

    public static boolean growTarget(Level world, BlockPos target) {
        if (!targetIsViable(world, target)) {
            return false;
        }
        BlockState state = world.getBlockState(target);
        if (state.getBlock() == Blocks.DIRT) {
            return world.setBlock(target, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        }
        if (state.getBlock() == ModBlocks.DIRT_SLAB) {
            if (state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
                return world.setBlock(target, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
            return world.setBlock(target, ModBlocks.grassStateLike(state), 3);
        }
        return false;
    }

    public static boolean isViableSource(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!canRemainGrass(world, pos) || !hasSpreadLight(world, pos)) {
            return false;
        }
        if (state.getBlock() == Blocks.GRASS_BLOCK) {
            return true;
        }
        if (state.getBlock() == ModBlocks.GRASS_SLAB) {
            return !state.getValue(SlabBlock.WATERLOGGED);
        }
        return state.getBlock() == ModBlocks.TURF &&
                world.getBlockState(pos.below()).getBlock() == Blocks.DIRT;
    }

    private static boolean targetIsViable(Level world, BlockPos target) {
        BlockState state = world.getBlockState(target);
        boolean dirt = state.getBlock() == Blocks.DIRT ||
                state.getBlock() == ModBlocks.DIRT_SLAB && !state.getValue(SlabBlock.WATERLOGGED);
        if (!dirt) {
            return false;
        }
        BlockPos above = target.above();
        BlockState cover = world.getBlockState(above);
        if (cover.getBlock() == ModBlocks.TURF || cover.getBlock() == ModBlocks.GRASS_SLAB) {
            return false;
        }
        return world.getMaxLocalRawBrightness(above) >= 4 &&
                cover.getLightBlock(world, above) < world.getMaxLightLevel() &&
                !world.getFluidState(above).is(FluidTags.WATER);
    }

    private GrassSpread() {
    }
}
