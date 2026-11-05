package zone.moddev.mc.skysgrassslabs.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SavedOverworldChunksTest {
    @Rule
    public final TemporaryFolder folders = new TemporaryFolder();

    @Test
    public void savedChunksAreProtectedWithoutALegacyRegistry() throws IOException {
        Path world = folders.newFolder("world").toPath();
        Path emptyWorld = folders.newFolder("empty-world").toPath();
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
        int slot = localZ * 32 + localX;
        header.putInt(slot * Integer.BYTES, (2 << 8) | 1);
        Files.createDirectories(path.getParent());
        Files.write(path, header.array());
    }
}
