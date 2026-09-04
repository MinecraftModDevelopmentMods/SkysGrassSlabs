package zone.moddev.mc.skysgrassslabs.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.state.properties.SlabType;
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
    void everyLegacyMetadataValueUsesOnlyItsOrientationBit() {
        for (boolean grass : new boolean[] {false, true}) {
            for (int metadata = 0; metadata < 16; ++metadata) {
                BlockState migrated = ModBlocks.legacySlabState(grass, metadata);
                assertSame(grass ? ModBlocks.GRASS_SLAB : ModBlocks.DIRT_SLAB,
                        migrated.getBlock());
                assertEquals((metadata & 1) == 0 ? SlabType.TOP : SlabType.BOTTOM,
                        migrated.get(SlabBlock.TYPE));
                assertFalse(migrated.get(SlabBlock.WATERLOGGED));
                assertFalse(migrated.get(SnowyDirtBlock.SNOWY));
            }
        }
    }

    @Test
    void supportedBuildingBricksAliasesMapToTheMatchingSkyState() {
        assertSame(ModBlocks.GRASS_SLAB, LegacyWorldDataHook.legacyState(
                BuildingBricksCompat.GRASS_SLAB_ID, 1).getBlock());
        assertSame(ModBlocks.GRASS_SLAB, LegacyWorldDataHook.legacyState(
                BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID, 0).getBlock());
        assertSame(ModBlocks.DIRT_SLAB, LegacyWorldDataHook.legacyState(
                BuildingBricksCompat.DIRT_SLAB_ID, 1).getBlock());
        assertTrue(LegacyMigrationHandler.legacySlabKind(BuildingBricksCompat.DIRT_SLAB_ID)
                == LegacyMigrationHandler.LegacySlabKind.DIRT);
    }

    @Test
    void flatteningTableCanHoldLegacyForgeNumericIdsAboveVanillasRange() {
        assertTrue(LegacyWorldDataHook.expandFlatteningTable(8192).length >= 8192);
    }
}
