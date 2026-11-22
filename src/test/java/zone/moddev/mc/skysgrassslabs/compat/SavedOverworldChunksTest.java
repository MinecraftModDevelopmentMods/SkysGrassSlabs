package zone.moddev.mc.skysgrassslabs.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import zone.moddev.mc.skysgrassslabs.MinecraftTestBootstrap;

class SavedOverworldChunksTest {
    @TempDir
    Path directory;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.registerVanilla();
    }

    @Test
    void savedChunksAreProtectedWithoutALegacyRegistry() throws IOException {
        Path world = Files.createDirectories(directory.resolve("world"));
        Path emptyWorld = Files.createDirectories(directory.resolve("empty-world"));
        writeRegionHeader(world.resolve("region/r.-1.2.mca"), 31, 0);
        writeRegionHeader(world.resolve("DIM-1/region/r.0.0.mca"), 0, 0);

        try {
            LegacyWorldDataHook.snapshotSavedOverworldChunks(world.toFile());
            assertTrue(LegacyWorldDataHook.isSavedOverworldChunk(-1, 64));
            assertFalse(LegacyWorldDataHook.isSavedOverworldChunk(-2, 64));
            assertFalse(LegacyWorldDataHook.isSavedOverworldChunk(0, 0));

            LegacyWorldDataHook.snapshotSavedOverworldChunks(emptyWorld.toFile());
            assertFalse(LegacyWorldDataHook.isSavedOverworldChunk(-1, 64));
        } finally {
            LegacyWorldDataHook.snapshotSavedOverworldChunks(emptyWorld.toFile());
        }
    }

    private static void writeRegionHeader(Path path, int localX, int localZ) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(8192);
        header.putInt((localZ * 32 + localX) * Integer.BYTES, (2 << 8) | 1);
        Files.createDirectories(path.getParent());
        Files.write(path, header.array());
    }
}
