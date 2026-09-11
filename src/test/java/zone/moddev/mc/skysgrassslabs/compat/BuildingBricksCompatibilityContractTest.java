package zone.moddev.mc.skysgrassslabs.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import org.junit.Test;

public class BuildingBricksCompatibilityContractTest {
    @Test
    public void onlySupportedHistoricalSlabsAreRecognised() {
        assertEquals(LegacyMigrationHandler.LegacySlabKind.GRASS,
                LegacyMigrationHandler.legacySlabKind(BuildingBricksCompat.GRASS_SLAB_ID));
        assertEquals(LegacyMigrationHandler.LegacySlabKind.GRASS,
                LegacyMigrationHandler.legacySlabKind(
                        BuildingBricksCompat.HISTORICAL_GRASS_SLAB_ID));
        assertEquals(LegacyMigrationHandler.LegacySlabKind.DIRT,
                LegacyMigrationHandler.legacySlabKind(BuildingBricksCompat.DIRT_SLAB_ID));

        assertNull(LegacyMigrationHandler.legacySlabKind(
                ResourceLocation.fromNamespaceAndPath("buildingbricks", "grass_stairs")));
        assertNull(LegacyMigrationHandler.legacySlabKind(
                ResourceLocation.fromNamespaceAndPath("buildingbricks", "dirt_vertical_slab")));
    }

    @Test
    public void absenceIsAutomaticButInstalledReplacementIsOptIn() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/compat/LegacyMigrationHandler.java"),
                StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertTrue(migration.contains("BuildingBricksCompat.hasLegacyAliases()"));
        assertTrue(migration.contains("BuildingBricksCompat.isInstalled()"));
        assertTrue(migration.contains(
                "SkysGrassSlabsConfig.forceReplaceBuildingBricksSlabs()"));
        assertTrue(migration.contains("|| BuildingBricksCompat.isInstalled()\n"
                + "                        && SkysGrassSlabsConfig"
                + ".forceReplaceBuildingBricksSlabs()"));
    }
}
