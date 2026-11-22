package zone.moddev.mc.skysgrassslabs.integrationtest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Exercise the callback Minecraft actually uses for randomly ticking grass. */
final class LifecycleParityChecks {
    static void run(ServerLevel level) {
        BlockPos position = new BlockPos(8, 210, 8);
        for (SlabType half : new SlabType[] { SlabType.BOTTOM, SlabType.TOP }) {
            BlockState grass = ModBlocks.GRASS_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, half);
            level.setBlock(position.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(position, grass, Block.UPDATE_ALL);
            level.setBlock(position.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

            level.getBlockState(position).randomTick(level, position, RandomSource.create(0));
            BlockState decayed = level.getBlockState(position);
            if (!decayed.is(ModBlocks.DIRT_SLAB) || decayed.getValue(SlabBlock.TYPE) != half) {
                throw new AssertionError("Covered grass must decay through native random ticks: " + half);
            }
            level.setBlock(position.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(position.north(), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(position.north().above(), Blocks.SNOW.defaultBlockState(), Block.UPDATE_ALL);
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        BlockPlaceContext placement = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND,
                new ItemStack(ModBlocks.GRASS_SLAB), new BlockHitResult(
                        Vec3.atBottomCenterOf(position), Direction.UP, position.below(), false));
        BlockState besideTerrace = ModBlocks.GRASS_SLAB.getStateForPlacement(placement);
        if (besideTerrace == null || !besideTerrace.getValue(SnowyDirtBlock.SNOWY)) {
            throw new AssertionError("A new grass slab must pick up the snow on a higher terrace");
        }
        level.setBlock(position.north().above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(position.north(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        org.apache.logging.log4j.LogManager.getLogger().info("GRASS_NATIVE_RANDOM_TICK_PARITY_PASS halves=2");
    }

    private LifecycleParityChecks() {
    }
}
