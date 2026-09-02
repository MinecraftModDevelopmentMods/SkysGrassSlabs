package zone.moddev.mc.skysgrassslabs.block;

import java.util.Random;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class GrassSpread {
    private static final int SPREAD_ATTEMPTS = 4;

    public static boolean canRemainGrass(World world, BlockPos pos) {
        BlockPos above = pos.up();
        return world.getLightFromNeighbors(above) >= 4 ||
                world.getBlockState(above).getLightOpacity(world, above) <= 2;
    }

    public static boolean hasSpreadLight(World world, BlockPos pos) {
        BlockPos above = pos.up();
        return world.getLightFromNeighbors(above) >= 9 &&
                world.getBlockState(above).getLightOpacity(world, above) <= 2;
    }

    public static void spreadFrom(World world, BlockPos source, Random random, BlockPos excludedTarget) {
        if (!world.isAreaLoaded(source, 3) || !hasSpreadLight(world, source)) {
            return;
        }
        for (int attempt = 0; attempt < SPREAD_ATTEMPTS; ++attempt) {
            BlockPos target = source.add(random.nextInt(3) - 1,
                    random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (target.equals(excludedTarget)) {
                continue;
            }
            if (target.getY() < 0 || target.getY() >= 256 || !world.isBlockLoaded(target)) {
                return;
            }
            growTarget(world, target);
        }
    }

    public static void tickDirtSlab(World world, BlockPos target, IBlockState state, Random random) {
        if (!world.isAreaLoaded(target, 3) || !targetIsViable(world, target)) {
            return;
        }
        for (int attempt = 0; attempt < SPREAD_ATTEMPTS; ++attempt) {
            BlockPos source = target.add(random.nextInt(3) - 1,
                    random.nextInt(5) - 1, random.nextInt(3) - 1);
            if (source.getY() < 0 || source.getY() >= 256 || !world.isBlockLoaded(source)) {
                return;
            }
            if (isViableSource(world, source)) {
                world.setBlockState(target, ModBlocks.GRASS_SLAB.getDefaultState()
                        .withProperty(BlockSlab.HALF, state.getValue(BlockSlab.HALF)), 3);
                return;
            }
        }
    }

    public static boolean growTarget(World world, BlockPos target) {
        if (!targetIsViable(world, target)) {
            return false;
        }
        IBlockState state = world.getBlockState(target);
        if (state.getBlock() == Blocks.DIRT &&
                state.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.DIRT) {
            return world.setBlockState(target, Blocks.GRASS.getDefaultState(), 3);
        }
        if (state.getBlock() == ModBlocks.DIRT_SLAB) {
            return world.setBlockState(target, ModBlocks.GRASS_SLAB.getDefaultState()
                    .withProperty(BlockSlab.HALF, state.getValue(BlockSlab.HALF)), 3);
        }
        return false;
    }

    public static boolean isViableSource(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!canRemainGrass(world, pos) || !hasSpreadLight(world, pos)) {
            return false;
        }
        if (state.getBlock() == Blocks.GRASS || state.getBlock() == ModBlocks.GRASS_SLAB) {
            return true;
        }
        return state.getBlock() == ModBlocks.TURF && world.getBlockState(pos.down()).getBlock() == Blocks.DIRT;
    }

    private static boolean targetIsViable(World world, BlockPos target) {
        IBlockState state = world.getBlockState(target);
        boolean dirt = state.getBlock() == ModBlocks.DIRT_SLAB ||
                (state.getBlock() == Blocks.DIRT &&
                        state.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.DIRT);
        if (!dirt) {
            return false;
        }
        BlockPos above = target.up();
        IBlockState cover = world.getBlockState(above);
        if (cover.getBlock() == ModBlocks.TURF || cover.getBlock() == ModBlocks.GRASS_SLAB) {
            return false;
        }
        return world.getLightFromNeighbors(above) >= 4 &&
                cover.getLightOpacity(world, above) <= 2;
    }

    private GrassSpread() {
    }
}
