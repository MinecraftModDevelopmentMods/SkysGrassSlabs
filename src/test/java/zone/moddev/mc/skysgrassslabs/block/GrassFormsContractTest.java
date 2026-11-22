package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import zone.moddev.mc.skysgrassslabs.MinecraftTestBootstrap;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

import static org.junit.jupiter.api.Assertions.*;

class GrassFormsContractTest {
    @BeforeAll
    static void registerForms() {
        MinecraftTestBootstrap.registerVanilla();
        for (SlabType half : new SlabType[] {SlabType.BOTTOM, SlabType.TOP}) {
            for (boolean waterlogged : new boolean[] {false, true}) {
                GrassSlabsApi.registerGrassForm(slab(ModBlocks.DIRT_SLAB, half, waterlogged),
                        slab(ModBlocks.GRASS_SLAB, half, waterlogged));
            }
        }
    }

    @Test
    void orientationsAndWaterloggingSurviveGrassConversion() {
        for (SlabType half : new SlabType[] {SlabType.BOTTOM, SlabType.TOP}) {
            for (boolean waterlogged : new boolean[] {false, true}) {
                BlockState dirt = slab(ModBlocks.DIRT_SLAB, half, waterlogged);
                BlockState grass = slab(ModBlocks.GRASS_SLAB, half, waterlogged);

                assertEquals(grass, GrassSlabsApi.grassFor(dirt));
                assertEquals(dirt, GrassSlabsApi.dirtFor(grass.setValue(SnowyDirtBlock.SNOWY, true)));
                assertEquals(waterlogged, GrassSlabsApi.dirtFor(grass).getValue(SlabBlock.WATERLOGGED));
                assertEquals(!waterlogged, GrassSlabsApi.isSpreadingGrassForm(grass));
            }
        }
    }

    @Test
    void duplicateNullAndIdenticalMappingsAreRejected() {
        BlockState dirt = ModBlocks.DIRT_SLAB.defaultBlockState();
        BlockState grass = ModBlocks.GRASS_SLAB.defaultBlockState();

        assertThrows(IllegalArgumentException.class,
                () -> GrassSlabsApi.registerGrassForm(dirt, grass));
        assertThrows(IllegalArgumentException.class,
                () -> GrassSlabsApi.registerGrassForm(null, grass));
        assertThrows(IllegalArgumentException.class,
                () -> GrassSlabsApi.registerGrassForm(dirt, dirt));
    }

    @Test
    void aGrassSkinCanShareDirtWithoutChangingDefaultRegrowth() {
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState alternative = Blocks.MYCELIUM.defaultBlockState();
        GrassSlabsApi.registerGrassForm(dirt, grass);
        GrassSlabsApi.registerGrassVariant(dirt, alternative, false);

        assertEquals(grass, GrassSlabsApi.grassFor(dirt));
        assertEquals(dirt, GrassSlabsApi.dirtFor(alternative));
        assertFalse(GrassSlabsApi.isSpreadingGrassForm(alternative));
        assertThrows(IllegalArgumentException.class,
                () -> GrassSlabsApi.registerGrassVariant(dirt, alternative, true));
    }

    @Test
    void stepsAndCornersLeaveTheirGrassSupportExposed() {
        assertTrue(GrassSlabsApi.coversSupport(ModBlocks.GRASS_SLAB.defaultBlockState(),
                null, BlockPos.ZERO));
        assertTrue(GrassSlabsApi.coversSupport(ModBlocks.TURF.defaultBlockState(),
                null, BlockPos.ZERO));

        for (VoxelShape footprint : new VoxelShape[] {
                Block.box(0, 0, 0, 16, 8, 8),
                Block.box(8, 0, 0, 16, 8, 16),
                Block.box(0, 0, 8, 16, 8, 16),
                Block.box(0, 0, 0, 8, 8, 16),
                Block.box(0, 0, 0, 8, 8, 8)}) {
            Block piece = new Block(Block.Properties.of()) {
                @Override
                public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos position,
                        CollisionContext context) {
                    return footprint;
                }
            };
            assertFalse(GrassSlabsApi.coversSupport(piece.defaultBlockState(), null, BlockPos.ZERO));
        }
    }

    private static BlockState slab(Block block, SlabType half, boolean waterlogged) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, half)
                .setValue(SlabBlock.WATERLOGGED, waterlogged);
    }
}
