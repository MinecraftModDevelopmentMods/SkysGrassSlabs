package zone.moddev.mc.skysgrassslabs.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.world.ModWorldState;

class BuildingBricksPolicyTest {
    @Test
    void worldgenAndReplacementUseIndependentSettings() {
        assertFalse(BuildingBricksPolicy.shouldArbitrateWorldgen(false, false));
        assertFalse(BuildingBricksPolicy.shouldArbitrateWorldgen(false, true));
        assertFalse(BuildingBricksPolicy.shouldArbitrateWorldgen(true, false));
        assertTrue(BuildingBricksPolicy.shouldArbitrateWorldgen(true, true));

        assertFalse(BuildingBricksPolicy.shouldReplaceSlabs(false, false));
        assertFalse(BuildingBricksPolicy.shouldReplaceSlabs(false, true));
        assertFalse(BuildingBricksPolicy.shouldReplaceSlabs(true, false));
        assertTrue(BuildingBricksPolicy.shouldReplaceSlabs(true, true));
    }

    @Test
    void disabledReplacementNeverProcessesOrMarksAChunk() {
        assertFalse(LegacyMigrationHandler.shouldMigrateChunk(false, 0));
        assertFalse(LegacyMigrationHandler.shouldMigrateChunk(false, 1));
        assertTrue(LegacyMigrationHandler.shouldMigrateChunk(true, 0));
        assertFalse(LegacyMigrationHandler.shouldMigrateChunk(true, 1));
        assertFalse(LegacyMigrationHandler.shouldMigrateChunk(true, 2));
    }

    @Test
    void currentAndFutureChunkMarkersSurviveLaterSaves() {
        assertFalse(LegacyMigrationHandler.shouldPreserveChunkMarker(0));
        assertTrue(LegacyMigrationHandler.shouldPreserveChunkMarker(1));
        assertTrue(LegacyMigrationHandler.shouldPreserveChunkMarker(2));
    }

    @Test
    void onlySupportedLegacyIdsAreClassifiedForMissingMappingRecovery() {
        assertTrue(LegacyMigrationHandler.legacySlabKind(BuildingBricksCompat.GRASS_SLAB_ID)
                == LegacyMigrationHandler.LegacySlabKind.GRASS);
        assertTrue(LegacyMigrationHandler.legacySlabKind(
                BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID)
                == LegacyMigrationHandler.LegacySlabKind.GRASS);
        assertTrue(LegacyMigrationHandler.legacySlabKind(BuildingBricksCompat.DIRT_SLAB_ID)
                == LegacyMigrationHandler.LegacySlabKind.DIRT);
        assertTrue(LegacyMigrationHandler.legacySlabKind(
                new ResourceLocation("buildingbricks", "dirt_stairs")) == null);
    }

    @Test
    void forcedMigrationReportIdentifiesItsModeAndRetainsSchema() {
        ModWorldState state = new ModWorldState(ModWorldState.DATA_NAME);
        state.recordChunk();
        state.recordGrassBlocks(2, 1);
        state.recordDirtItems(3);

        List<String> lines = LegacyMigrationHandler.migrationReportLines(state);

        assertTrue(lines.get(0).contains(SkysGrassSlabs.VERSION));
        assertTrue(lines.contains("schema_version=1"));
        assertTrue(lines.contains("migration_version=1"));
        assertTrue(lines.contains("force_replacement_enabled=true"));
        assertTrue(lines.contains("migrated_chunks=1"));
        assertTrue(lines.contains("migrated_grass_blocks_bottom=2"));
        assertTrue(lines.contains("migrated_dirt_items=3"));
    }
}
