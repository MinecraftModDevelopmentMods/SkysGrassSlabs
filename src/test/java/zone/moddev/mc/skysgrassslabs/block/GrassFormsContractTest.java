package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.block.BlockGrass;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.junit.jupiter.api.*;
import zone.moddev.mc.skysgrassslabs.MinecraftTestBootstrap;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import static org.junit.jupiter.api.Assertions.*;

class GrassFormsContractTest {
    @BeforeAll
    static void register() {
        MinecraftTestBootstrap.registerVanilla();

        for (int metadata = 0; metadata < 2; metadata++) {
            GrassSlabsApi.registerGrassForm(ModBlocks.DIRT_SLAB.getStateFromMeta(metadata),
                    ModBlocks.GRASS_SLAB.getStateFromMeta(metadata));
        }
    }

    @Test
    void bothOrientationsRoundTripAndSnowDoesNotChangeIdentity() {
        for (int metadata = 0; metadata < 2; metadata++) {
            IBlockState dirt = ModBlocks.DIRT_SLAB.getStateFromMeta(metadata);
            IBlockState grass = ModBlocks.GRASS_SLAB.getStateFromMeta(metadata);

            assertEquals(grass, GrassSlabsApi.grassFor(dirt));
            assertEquals(dirt, GrassSlabsApi.dirtFor(grass));
            assertEquals(dirt, GrassSlabsApi.dirtFor(grass.withProperty(BlockGrass.SNOWY, true)));
            assertEquals(grass.getValue(BlockSlab.HALF), GrassSlabsApi.grassFor(dirt).getValue(BlockSlab.HALF));
            assertTrue(GrassSlabsApi.isGrassForm(grass));
            assertFalse(GrassSlabsApi.isGrassForm(dirt));
        }
    }

    @Test
    void duplicateOrInvalidMappingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> GrassSlabsApi
                .registerGrassForm(ModBlocks.DIRT_SLAB.getStateFromMeta(0), ModBlocks.GRASS_SLAB.getStateFromMeta(0)));
        assertThrows(IllegalArgumentException.class,
                () -> GrassSlabsApi.registerGrassForm(null, ModBlocks.GRASS_SLAB.getDefaultState()));
        assertThrows(IllegalArgumentException.class, () -> GrassSlabsApi
                .registerGrassForm(ModBlocks.DIRT_SLAB.getDefaultState(), ModBlocks.DIRT_SLAB.getDefaultState()));
    }

    @Test
    void onlyACompleteFootprintCoversTheGrassBelow() {
        for (int metadata = 0; metadata < 2; metadata++) {
            assertTrue(GrassSlabsApi.coversSupport(ModBlocks.GRASS_SLAB.getStateFromMeta(metadata),
                    null, BlockPos.ORIGIN));
        }
        assertTrue(GrassSlabsApi.coversSupport(ModBlocks.TURF.getDefaultState(),
                null, BlockPos.ORIGIN));

        for (AxisAlignedBB bounds : new AxisAlignedBB[] {
                new AxisAlignedBB(0, 0, 0, 1, .5, .5),
                new AxisAlignedBB(.5, 0, 0, 1, .5, 1),
                new AxisAlignedBB(0, 0, .5, 1, .5, 1),
                new AxisAlignedBB(0, 0, 0, .5, .5, 1),
                new AxisAlignedBB(0, 0, 0, .5, .5, .5) }) {
            Block partial = new Block(Material.GRASS) {
                @Override
                public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
                    return bounds;
                }
            };
            assertFalse(GrassSlabsApi.coversSupport(partial.getDefaultState(),
                    null, BlockPos.ORIGIN));
        }
    }
}
