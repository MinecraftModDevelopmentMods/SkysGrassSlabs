package zone.moddev.mc.skysgrassslabs.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.EmptyBlockReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import zone.moddev.mc.skysgrassslabs.MinecraftTestBootstrap;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

class SlabContractTest {
    @BeforeAll
    static void registerVanilla() {
        MinecraftTestBootstrap.registerVanilla();
    }

    @Test
    void slabsUseNativeTypeWaterloggingAndSnowStates() {
        for (LegacySlabBlock slab : new LegacySlabBlock[] {
                ModBlocks.DIRT_SLAB, ModBlocks.GRASS_SLAB, ModBlocks.PATH_SLAB}) {
            BlockState state = slab.defaultBlockState();
            assertEquals(SlabType.BOTTOM, state.getValue(SlabBlock.TYPE));
            assertFalse(state.getValue(SlabBlock.WATERLOGGED));
            assertTrue(SlabBlock.TYPE.getPossibleValues().contains(SlabType.TOP));
            assertTrue(SlabBlock.TYPE.getPossibleValues().contains(SlabType.DOUBLE));
        }
        assertFalse(ModBlocks.DIRT_SLAB.defaultBlockState().getValue(SnowyDirtBlock.SNOWY));
        assertFalse(ModBlocks.GRASS_SLAB.defaultBlockState().getValue(SnowyDirtBlock.SNOWY));
    }

    @Test
    void pathProfilesAreExactlySevenSixteenthsHigh() {
        assertProfile(ModBlocks.PATH_SLAB.defaultBlockState() .setValue(SlabBlock.TYPE, SlabType.BOTTOM),
                0.0D, 7.0D / 16.0D);
        assertProfile(ModBlocks.PATH_SLAB.defaultBlockState() .setValue(SlabBlock.TYPE, SlabType.TOP),
                8.0D / 16.0D, 15.0D / 16.0D);
    }

    @Test
    void turfIsOnePixelHighAndDoesNotConnectToFences() {
        BlockState turf = ModBlocks.TURF.defaultBlockState();
        VoxelShape shape = ModBlocks.TURF.getShape(turf, null, BlockPos.ZERO, null);
        assertEquals(1.0D / 16.0D, shape.bounds().maxY);
        assertEquals(1.0D / 16.0D,
                turf.getCollisionShape(null, BlockPos.ZERO).bounds().maxY);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            assertFalse(turf.isFaceSturdy(
                    EmptyBlockReader.INSTANCE, BlockPos.ZERO, direction));
        }
    }

    private static void assertProfile(BlockState state, double minY, double maxY) {
        VoxelShape shape = ModBlocks.PATH_SLAB.getShape(state, null, BlockPos.ZERO, null);
        assertEquals(minY, shape.bounds().minY);
        assertEquals(maxY, shape.bounds().maxY);
    }
}
