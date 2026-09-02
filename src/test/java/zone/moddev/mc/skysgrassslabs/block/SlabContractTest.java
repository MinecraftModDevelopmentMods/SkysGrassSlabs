package zone.moddev.mc.skysgrassslabs.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
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
    void metadataMatchesBuildingBricksTopAndBottomContract() {
        assertMetadata(ModBlocks.DIRT_SLAB);
        assertMetadata(ModBlocks.GRASS_SLAB);
        assertMetadata(ModBlocks.PATH_SLAB);
    }

    @Test
    void visualSnowStateIsNotPersistedInSlabMetadata() {
        for (BlockSlab.EnumBlockHalf half : BlockSlab.EnumBlockHalf.values()) {
            int expected = half == BlockSlab.EnumBlockHalf.TOP ? 0 : 1;
            IBlockState dirt = ModBlocks.DIRT_SLAB.getDefaultState()
                    .withProperty(BlockSlab.HALF, half)
                    .withProperty(BlockDirt.SNOWY, Boolean.TRUE);
            IBlockState grass = ModBlocks.GRASS_SLAB.getDefaultState()
                    .withProperty(BlockSlab.HALF, half)
                    .withProperty(BlockGrass.SNOWY, Boolean.TRUE);

            assertEquals(expected, ModBlocks.DIRT_SLAB.getMetaFromState(dirt));
            assertEquals(expected, ModBlocks.GRASS_SLAB.getMetaFromState(grass));
            assertFalse(ModBlocks.DIRT_SLAB.getStateFromMeta(expected)
                    .getValue(BlockDirt.SNOWY));
            assertFalse(ModBlocks.GRASS_SLAB.getStateFromMeta(expected)
                    .getValue(BlockGrass.SNOWY));
        }
    }

    @Test
    void pathProfilesAreExactlySevenSixteenthsHigh() {
        IBlockState bottom = ModBlocks.PATH_SLAB.getStateFromMeta(1);
        IBlockState top = ModBlocks.PATH_SLAB.getStateFromMeta(0);

        assertEquals(0.0D, ModBlocks.PATH_SLAB.getBoundingBox(bottom, null, BlockPos.ORIGIN).minY);
        assertEquals(7.0D / 16.0D,
                ModBlocks.PATH_SLAB.getBoundingBox(bottom, null, BlockPos.ORIGIN).maxY);
        assertEquals(8.0D / 16.0D,
                ModBlocks.PATH_SLAB.getBoundingBox(top, null, BlockPos.ORIGIN).minY);
        assertEquals(15.0D / 16.0D,
                ModBlocks.PATH_SLAB.getBoundingBox(top, null, BlockPos.ORIGIN).maxY);
    }

    @Test
    void turfIsOnePixelHighAndNotAFullCube() {
        assertEquals(1.0D / 16.0D,
                ModBlocks.TURF.getBoundingBox(ModBlocks.TURF.getDefaultState(), null,
                        BlockPos.ORIGIN).maxY);
        assertFalse(ModBlocks.TURF.isFullCube(ModBlocks.TURF.getDefaultState()));
        assertFalse(ModBlocks.TURF.isOpaqueCube(ModBlocks.TURF.getDefaultState()));
    }

    @Test
    void grassAndTurfUseTheCutoutMippedLayer() {
        assertEquals(BlockRenderLayer.CUTOUT_MIPPED, ModBlocks.GRASS_SLAB.getBlockLayer());
        assertEquals(BlockRenderLayer.CUTOUT_MIPPED, ModBlocks.TURF.getBlockLayer());
    }

    private static void assertMetadata(LegacySlabBlock slab) {
        IBlockState top = slab.getStateFromMeta(0);
        IBlockState bottom = slab.getStateFromMeta(1);
        assertEquals(BlockSlab.EnumBlockHalf.TOP, top.getValue(BlockSlab.HALF));
        assertEquals(BlockSlab.EnumBlockHalf.BOTTOM, bottom.getValue(BlockSlab.HALF));
        assertEquals(0, slab.getMetaFromState(top));
        assertEquals(1, slab.getMetaFromState(bottom));
        assertFalse(slab.isDouble());
        assertTrue(slab.getDefaultState().getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM);
    }
}
