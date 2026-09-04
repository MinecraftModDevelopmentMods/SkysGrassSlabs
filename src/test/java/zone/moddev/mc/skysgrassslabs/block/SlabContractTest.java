package zone.moddev.mc.skysgrassslabs.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
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
            BlockState state = slab.getDefaultState();
            assertEquals(SlabType.BOTTOM, state.get(SlabBlock.TYPE));
            assertFalse(state.get(SlabBlock.WATERLOGGED));
            assertTrue(SlabBlock.TYPE.getAllowedValues().contains(SlabType.TOP));
            assertTrue(SlabBlock.TYPE.getAllowedValues().contains(SlabType.DOUBLE));
        }
        assertFalse(ModBlocks.DIRT_SLAB.getDefaultState().get(SnowyDirtBlock.SNOWY));
        assertFalse(ModBlocks.GRASS_SLAB.getDefaultState().get(SnowyDirtBlock.SNOWY));
    }

    @Test
    void pathProfilesAreExactlySevenSixteenthsHigh() {
        assertProfile(ModBlocks.PATH_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM),
                0.0D, 7.0D / 16.0D);
        assertProfile(ModBlocks.PATH_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP),
                8.0D / 16.0D, 15.0D / 16.0D);
    }

    @Test
    void turfIsOnePixelHighAndDoesNotConnectToFences() {
        BlockState turf = ModBlocks.TURF.getDefaultState();
        VoxelShape shape = ModBlocks.TURF.getShape(turf, null, BlockPos.ZERO, null);
        assertEquals(1.0D / 16.0D, shape.getBoundingBox().maxY);
        assertEquals(1.0D / 16.0D,
                turf.getCollisionShape(null, BlockPos.ZERO).getBoundingBox().maxY);
        assertFalse(turf.isSolid());
    }

    @Test
    void grassAndTurfUseTheCutoutMippedLayer() {
        assertEquals(BlockRenderLayer.CUTOUT_MIPPED, ModBlocks.GRASS_SLAB.getRenderLayer());
        assertEquals(BlockRenderLayer.CUTOUT_MIPPED, ModBlocks.TURF.getRenderLayer());
    }

    private static void assertProfile(BlockState state, double minY, double maxY) {
        VoxelShape shape = ModBlocks.PATH_SLAB.getShape(state, null, BlockPos.ZERO, null);
        assertEquals(minY, shape.getBoundingBox().minY);
        assertEquals(maxY, shape.getBoundingBox().maxY);
    }
}
