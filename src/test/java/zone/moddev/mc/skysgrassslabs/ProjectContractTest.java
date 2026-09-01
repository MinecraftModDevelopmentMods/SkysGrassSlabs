package zone.moddev.mc.skysgrassslabs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class ProjectContractTest {
    @Test
    public void metadataUsesStableIdentityWithoutOreSpawnDependency() throws Exception {
        String metadata = Files.readString(
                Path.of("src/main/resources/META-INF/mods.toml"), StandardCharsets.UTF_8);
        String properties = Files.readString(Path.of("gradle.properties"), StandardCharsets.UTF_8);

        assertTrue(properties.contains("mod_id=skysgrassslabs"));
        assertTrue(properties.contains("minecraft_version=1.18.2"));
        assertTrue(properties.contains("forge_version=40.3.0"));
        assertFalse(metadata.contains("modId=\"orespawn\""));
    }

    @Test
    public void handoffDocumentsExist() {
        assertTrue(Files.isRegularFile(Path.of("docs/ARCHITECTURE.md")));
        assertTrue(Files.isRegularFile(Path.of("docs/LEGACY-MIGRATION.md")));
        assertTrue(Files.isRegularFile(Path.of("docs/TESTING.md")));
        assertTrue(Files.isRegularFile(Path.of("docs/REPOSITORY.md")));
        assertTrue(Files.isRegularFile(Path.of("docs/ROADMAP.md")));
        assertTrue(Files.isRegularFile(Path.of("docs/BETA-0.2.0.118021.md")));
    }

    @Test
    public void betaIdentityAndLicenseAreStable() throws Exception {
        String properties = Files.readString(Path.of("gradle.properties"), StandardCharsets.UTF_8);
        assertTrue(properties.contains("mod_version=0.2.0.118021"));
        assertTrue(properties.contains("mod_license=LGPL-2.1-only"));
        assertEquals("LGPL-2.1-only", Files.readString(Path.of("LICENSE.spdx"), StandardCharsets.UTF_8).trim());
        assertTrue(Files.readString(Path.of("NOTICE"), StandardCharsets.UTF_8)
                .contains("Copyright (C) 2026 SkyBlade1978"));
    }


    @Test
    public void commonConfigAndWorldStateUsePermanentKeys() throws Exception {
        String config = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/config/BetaConfig.java"),
                StandardCharsets.UTF_8);
        String state = Files.readString(Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/world/ModWorldState.java"),
                StandardCharsets.UTF_8);
        assertTrue(config.contains("define(\"generateGrassSlabs\", true)"));
        assertTrue(state.contains("skysgrassslabs_world_state"));
        assertTrue(state.contains("SCHEMA_VERSION = 1"));
        assertTrue(state.contains("schema_version"));
    }

    @Test
    public void allSaveFacingResourcesExist() {
        String[] paths = {
                "src/main/resources/assets/skysgrassslabs/blockstates/dirt_slab.json",
                "src/main/resources/assets/skysgrassslabs/blockstates/grass_slab.json",
                "src/main/resources/assets/skysgrassslabs/blockstates/path_slab.json",
                "src/main/resources/assets/skysgrassslabs/blockstates/turf.json",
                "src/main/resources/assets/skysgrassslabs/models/block/turf.json",
                "src/main/resources/assets/skysgrassslabs/models/item/turf.json",
                "src/main/resources/data/skysgrassslabs/recipes/dirt_slab.json",
                "src/main/resources/data/skysgrassslabs/recipes/grass_slab.json",
                "src/main/resources/data/skysgrassslabs/recipes/grass_block_from_seeds.json",
                "src/main/resources/data/skysgrassslabs/recipes/grass_slab_from_seeds.json",
                "src/main/resources/data/skysgrassslabs/recipes/turf.json",
                "src/main/resources/data/skysgrassslabs/loot_tables/blocks/dirt_slab.json",
                "src/main/resources/data/skysgrassslabs/loot_tables/blocks/grass_slab.json",
                "src/main/resources/data/skysgrassslabs/loot_tables/blocks/path_slab.json",
                "src/main/resources/data/skysgrassslabs/loot_tables/blocks/turf.json"
        };
        for (String path : paths) {
            assertTrue(path, Files.isRegularFile(Path.of(path)));
        }
    }
}
