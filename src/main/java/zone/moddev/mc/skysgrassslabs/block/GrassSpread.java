package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;

public final class GrassSpread {
    private static final int SPREAD_ATTEMPTS = 4;

    public static boolean canRemainGrass(Level world, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState cover = world.getBlockState(above);
        if (cover.is(Blocks.SNOW)
                && cover.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        if (cover.getFluidState().getAmount() == 8) {
            return false;
        }
        // Test the cover without letting the piece's own partial shape hide it.
        int blocked = net.minecraft.world.level.lighting.LightEngine.getLightBlockInto(
                world, Blocks.AIR.defaultBlockState(), pos, cover, above,
                net.minecraft.core.Direction.UP, cover.getLightBlock(world, above));
        return blocked < world.getMaxLightLevel();
    }

    public static boolean hasSpreadLight(Level world, BlockPos pos) {
        BlockPos above = pos.above();
        return world.getMaxLocalRawBrightness(above) >= 9 &&
                world.getBlockState(above).getLightBlock(world, above) < world.getMaxLightLevel() &&
                !world.getFluidState(above).is(FluidTags.WATER);
    }

    public static void spreadFrom(Level world, BlockPos source, RandomSource random,
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
            if (target.getY() < world.getMinBuildHeight() || target.getY() >= world.getMaxBuildHeight() || !world.isLoaded(target)) {
                return;
            }
            growTarget(world, target, world.getBlockState(source));
        }
    }

    public static void tickDirtSlab(Level world, BlockPos target, BlockState state,
            RandomSource random) {
        if (!world.isAreaLoaded(target, 3) || !targetIsViable(world, target)) {
            return;
        }
        for (int attempt = 0; attempt < SPREAD_ATTEMPTS; ++attempt) {
            BlockPos source = target.offset(random.nextInt(3) - 1,
                    random.nextInt(5) - 1, random.nextInt(3) - 1);
            if (source.getY() < world.getMinBuildHeight() || source.getY() >= world.getMaxBuildHeight() || !world.isLoaded(source)) {
                return;
            }
            if (isViableSource(world, source)) {
                BlockState grass = GrassSlabsApi.grassFor(state, world.getBlockState(source));
                if (grass != null) {
                    world.setBlock(target, grass, 3);
                }
                return;
            }
        }
    }

    public static boolean growTarget(Level world, BlockPos target) {
        return growTarget(world, target, Blocks.GRASS_BLOCK.defaultBlockState());
    }

    private static boolean growTarget(Level world, BlockPos target, BlockState source) {
        if (!targetIsViable(world, target)) {
            return false;
        }
        BlockState state = world.getBlockState(target);
        if (state.getBlock() == Blocks.DIRT) {
            BlockState grass = GrassSlabsApi.grassFor(state, source);
            return world.setBlock(target,
                    grass == null ? Blocks.GRASS_BLOCK.defaultBlockState() : grass, 3);
        }
        if (state.getBlock() == ModBlocks.DIRT_SLAB) {
            if (state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
                return world.setBlock(target, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
        }

        BlockState grass = GrassSlabsApi.grassFor(state, source);
        if (grass != null) {
            return world.setBlock(target, grass, 3);
        }
        return false;
    }

    public static boolean isViableSource(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!canRemainGrass(world, pos) || !hasSpreadLight(world, pos)) {
            return false;
        }
        if (state.getBlock() == Blocks.GRASS_BLOCK || GrassSlabsApi.isSpreadingGrassForm(state)) {
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
        boolean dirt = state.getBlock() == Blocks.DIRT || GrassSlabsApi.grassFor(state) != null ||
                state.getBlock() == ModBlocks.DIRT_SLAB && !state.getValue(SlabBlock.WATERLOGGED);
        if (!dirt || !state.getFluidState().isEmpty()
                || state.hasProperty(SlabBlock.WATERLOGGED) && state.getValue(SlabBlock.WATERLOGGED)) {
            return false;
        }
        BlockPos above = target.above();
        BlockState cover = world.getBlockState(above);
        // Narrow steps and corners leave full ground exposed, but still cover smaller pieces.
        if (cover.getBlock() == ModBlocks.TURF || GrassSlabsApi.isGrassForm(cover)
                && (!state.isCollisionShapeFullBlock(world, target)
                        || GrassSlabsApi.coversSupport(cover, world, above))) {
            return false;
        }
        return world.getMaxLocalRawBrightness(above) >= 4 &&
                cover.getLightBlock(world, above) < world.getMaxLightLevel() &&
                !world.getFluidState(above).is(FluidTags.WATER);
    }

    private GrassSpread() {
    }
}
