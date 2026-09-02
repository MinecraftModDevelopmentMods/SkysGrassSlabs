package zone.moddev.mc.skysgrassslabs.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import zone.moddev.mc.skysgrassslabs.MinecraftTestBootstrap;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

class MigrationMappingTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.registerVanilla();
    }

    @Test
    void buildingBricksMetadataIsPreservedForBothSupportedBlocks() {
        for (boolean grass : new boolean[] {false, true}) {
            for (int metadata = 0; metadata <= 1; ++metadata) {
                IBlockState migrated = LegacyMigrationHandler.skyStateFor(grass, metadata);
                assertSame(grass ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB,
                        migrated.getBlock());
                assertEquals(metadata == 0 ? BlockSlab.EnumBlockHalf.TOP
                        : BlockSlab.EnumBlockHalf.BOTTOM,
                        migrated.getValue(BlockSlab.HALF));
            }
        }
    }

    @Test
    void onlyTheOrientationBitIsAcceptedFromHistoricalMetadata() {
        assertEquals(BlockSlab.EnumBlockHalf.TOP,
                LegacyMigrationHandler.skyStateFor(true, 2).getValue(BlockSlab.HALF));
        assertEquals(BlockSlab.EnumBlockHalf.BOTTOM,
                LegacyMigrationHandler.skyStateFor(false, 3).getValue(BlockSlab.HALF));
    }
}
