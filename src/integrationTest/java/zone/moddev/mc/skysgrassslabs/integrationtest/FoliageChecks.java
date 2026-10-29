package zone.moddev.mc.skysgrassslabs.integrationtest;

import java.util.Random;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.GrassSlabSmoothingHandler;

/** Covers plants present before this owner's decoration, not a second generation pass. */
final class FoliageChecks {
    static int run(WorldServer world) {
        BlockPos floor = new BlockPos(3528, 210, 3528);
        BlockPos origin = new BlockPos(floor.getX() & ~15, 0, floor.getZ() & ~15);
        GrassSlabSmoothingHandler handler = new GrassSlabSmoothingHandler();
        int checks = 0;

        for (IBlockState plant : new IBlockState[] { Blocks.TALLGRASS.getDefaultState(),
                Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.FERN),
                Blocks.YELLOW_FLOWER.getDefaultState(), Blocks.RED_FLOWER.getDefaultState(),
                Blocks.DEADBUSH.getDefaultState() }) {
            for (int arrangement = 0; arrangement < 3; arrangement++) {
                prepare(world, floor);
                if (arrangement != 1) {
                    world.setBlockState(floor.up(), plant, 2);
                }
                if (arrangement != 0) {
                    world.setBlockState(floor.east().up(2), plant, 2);
                }
                handler.beforeDecoration(new DecorateBiomeEvent.Pre(world, new Random(1), origin));
                require(world.getBlockState(floor.up()).getBlock() == ModBlocks.GRASS_SLAB
                        && world.getBlockState(floor).getBlock() == Blocks.DIRT,
                        "plants on either side must not hide a valid slope");
                checks++;
            }
        }

        prepare(world, floor);
        world.setBlockState(floor.up(), Blocks.DOUBLE_PLANT.getDefaultState()
                .withProperty(BlockDoublePlant.HALF, BlockDoublePlant.EnumBlockHalf.LOWER), 2);
        world.setBlockState(floor.up(2), Blocks.DOUBLE_PLANT.getDefaultState()
                .withProperty(BlockDoublePlant.HALF, BlockDoublePlant.EnumBlockHalf.UPPER), 2);
        handler.beforeDecoration(new DecorateBiomeEvent.Pre(world, new Random(1), origin));
        require(world.getBlockState(floor.up()).getBlock() == ModBlocks.GRASS_SLAB
                && world.isAirBlock(floor.up(2)), "tall plants leave no orphaned upper half");
        checks++;

        for (IBlockState obstacle : new IBlockState[] { Blocks.LEAVES.getDefaultState(), Blocks.LOG.getDefaultState(),
                Blocks.WHEAT.getDefaultState(), Blocks.SAPLING.getDefaultState(), Blocks.VINE.getDefaultState(),
                Blocks.CHEST.getDefaultState(), Blocks.WATER.getDefaultState() }) {
            prepare(world, floor);
            world.setBlockState(floor.up(), obstacle, 2);
            handler.beforeDecoration(new DecorateBiomeEvent.Pre(world, new Random(1), origin));
            require(world.getBlockState(floor.up()).equals(obstacle)
                    && world.getBlockState(floor).getBlock() == Blocks.GRASS,
                    "trees, crops, vines, fluids and block entities remain intact");
            checks++;
        }

        System.out.println("GRASS_SLAB_FOLIAGE_PASS checks=" + checks);
        return checks;
    }

    private static void prepare(WorldServer world, BlockPos floor) {
        for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {
            for (int offsetX = -2; offsetX <= 2; offsetX++) {
                BlockPos support = floor.add(offsetX, 0, offsetZ);
                for (int height = 1; height <= 6; height++) {
                    world.setBlockState(support.up(height), Blocks.AIR.getDefaultState(), 2);
                }
                world.setBlockState(support, Blocks.GRASS.getDefaultState(), 2);
            }
        }
        world.setBlockState(floor.east().up(), Blocks.GRASS.getDefaultState(), 2);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private FoliageChecks() {
    }
}
