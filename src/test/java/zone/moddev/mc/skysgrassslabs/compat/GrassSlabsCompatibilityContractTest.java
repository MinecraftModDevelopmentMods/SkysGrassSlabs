package zone.moddev.mc.skysgrassslabs.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.Test;

public class GrassSlabsCompatibilityContractTest {
    @Test
    public void onlyIdsWithSafeReplacementsAreRecognised() {
        assertEquals(GrassSlabsMigrationHandler.LegacyKind.GRASS_SLAB,
                GrassSlabsMigrationHandler.legacyKind(id("grass_slab")));
        assertEquals(GrassSlabsMigrationHandler.LegacyKind.DIRT_SLAB,
                GrassSlabsMigrationHandler.legacyKind(id("dirt_slab")));
        assertEquals(GrassSlabsMigrationHandler.LegacyKind.PATH_SLAB,
                GrassSlabsMigrationHandler.legacyKind(id("dirt_path_slab")));
        assertEquals(GrassSlabsMigrationHandler.LegacyKind.GRASS_CARPET,
                GrassSlabsMigrationHandler.legacyKind(id("grass_carpet")));

        assertNull(GrassSlabsMigrationHandler.legacyKind(id("grass_stairs")));
        assertNull(GrassSlabsMigrationHandler.legacyKind(id("dirt_carpet")));
        assertNull(GrassSlabsMigrationHandler.legacyKind(id("mycelium_slab")));
        assertNull(GrassSlabsMigrationHandler.legacyKind(id("podzol_slab")));

        assertEquals(List.of(id("grass_stairs"), id("dirt_stairs"),
                id("dirt_carpet"), id("dirt_path_stairs"),
                id("dirt_path_carpet"), id("mycelium_slab"),
                id("mycelium_stairs"), id("mycelium_carpet")),
                GrassSlabsCompat.UNSUPPORTED_1_18_IDS);
    }

    @Test
    public void absenceIsAutomaticButInstalledReplacementIsOptIn() {
        assertTrue(GrassSlabsMigrationHandler.replacementEnabled(false, true, false));
        assertFalse(GrassSlabsMigrationHandler.replacementEnabled(true, false, false));
        assertTrue(GrassSlabsMigrationHandler.replacementEnabled(true, false, true));
        assertFalse(GrassSlabsMigrationHandler.replacementEnabled(false, false, true));
    }

    @Test
    public void holdersCountersAndMarkerAreSourceSpecific() throws Exception {
        String compatibility = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/compat/GrassSlabsCompat.java"),
                StandardCharsets.UTF_8);
        String migration = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/compat/"
                        + "GrassSlabsMigrationHandler.java"), StandardCharsets.UTF_8);
        String state = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/world/ModWorldState.java"),
                StandardCharsets.UTF_8);

        assertTrue(compatibility.contains("if (isInstalled())"));
        assertTrue(compatibility.contains("new Item.Properties()"));
        assertTrue(migration.contains("skysgrassslabs_grassslabs_migration_version"));
        assertTrue(migration.contains("support.is(Blocks.DIRT)"));
        assertTrue(state.contains("grassslabs_migration_version"));
        assertTrue(state.contains("grassslabs_retained_grass_carpet_blocks"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(GrassSlabsCompat.MOD_ID, path);
    }
}
