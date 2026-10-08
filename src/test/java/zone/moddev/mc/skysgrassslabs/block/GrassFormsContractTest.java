package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import org.junit.jupiter.api.*;
import zone.moddev.mc.skysgrassslabs.MinecraftTestBootstrap;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import static org.junit.jupiter.api.Assertions.*;

class GrassFormsContractTest {
    @BeforeAll static void register() {
        MinecraftTestBootstrap.registerVanilla();
        for(int metadata=0;metadata<2;metadata++)GrassSlabsApi.registerGrassForm(
                ModBlocks.DIRT_SLAB.getStateFromMeta(metadata),ModBlocks.GRASS_SLAB.getStateFromMeta(metadata));
    }
    @Test void bothOrientationsRoundTripAndSnowDoesNotChangeIdentity() {
        for(int metadata=0;metadata<2;metadata++) {
            IBlockState dirt=ModBlocks.DIRT_SLAB.getStateFromMeta(metadata),grass=ModBlocks.GRASS_SLAB.getStateFromMeta(metadata);
            assertEquals(grass,GrassSlabsApi.grassFor(dirt));assertEquals(dirt,GrassSlabsApi.dirtFor(grass));
            assertEquals(dirt,GrassSlabsApi.dirtFor(grass.withProperty(BlockGrass.SNOWY,true)));
            assertEquals(grass.getValue(BlockSlab.HALF),GrassSlabsApi.grassFor(dirt).getValue(BlockSlab.HALF));
            assertTrue(GrassSlabsApi.isGrassForm(grass));assertFalse(GrassSlabsApi.isGrassForm(dirt));
        }
    }
    @Test void duplicateOrInvalidMappingsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->GrassSlabsApi.registerGrassForm(
                ModBlocks.DIRT_SLAB.getStateFromMeta(0),ModBlocks.GRASS_SLAB.getStateFromMeta(0)));
        assertThrows(IllegalArgumentException.class,()->GrassSlabsApi.registerGrassForm(null,ModBlocks.GRASS_SLAB.getDefaultState()));
        assertThrows(IllegalArgumentException.class,()->GrassSlabsApi.registerGrassForm(ModBlocks.DIRT_SLAB.getDefaultState(),ModBlocks.DIRT_SLAB.getDefaultState()));
    }
}
