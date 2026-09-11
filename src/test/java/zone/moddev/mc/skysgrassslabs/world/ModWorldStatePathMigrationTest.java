package zone.moddev.mc.skysgrassslabs.world;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ModWorldStatePathMigrationTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void copiesLegacyStateAndPreservesTheOriginal() throws Exception {
        Path root = temporaryFolder.newFolder("world").toPath();
        Path oldFile = oldFile(root);
        byte[] state = {10, 20, 30, 40};
        Files.createDirectories(oldFile.getParent());
        Files.write(oldFile, state);

        ModWorldState.relocateLegacyState(root);

        assertArrayEquals(state, Files.readAllBytes(newFile(root)));
        assertArrayEquals(state, Files.readAllBytes(oldFile));
    }

    @Test
    public void existingNamespacedStateTakesPriority() throws Exception {
        Path root = temporaryFolder.newFolder("priority").toPath();
        Path oldFile = oldFile(root);
        Path newFile = newFile(root);
        Files.createDirectories(oldFile.getParent());
        Files.createDirectories(newFile.getParent());
        Files.write(oldFile, new byte[] {1});
        Files.write(newFile, new byte[] {2});

        ModWorldState.relocateLegacyState(root);

        assertArrayEquals(new byte[] {2}, Files.readAllBytes(newFile));
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(oldFile));
    }

    @Test
    public void migrationFailureDoesNotDiscardLegacyState() throws Exception {
        Path root = temporaryFolder.newFolder("failure").toPath();
        Path oldFile = oldFile(root);
        Files.createDirectories(oldFile.getParent());
        Files.write(oldFile, new byte[] {7, 8, 9});
        Files.writeString(root.resolve("dimensions"), "blocks directory creation");

        assertThrows(IllegalStateException.class,
                () -> ModWorldState.relocateLegacyState(root));
        assertTrue(Files.isRegularFile(oldFile));
        assertArrayEquals(new byte[] {7, 8, 9}, Files.readAllBytes(oldFile));
    }

    private static Path oldFile(Path root) {
        return root.resolve("data").resolve(ModWorldState.DATA_NAME + ".dat");
    }

    private static Path newFile(Path root) {
        return root.resolve("dimensions").resolve("minecraft").resolve("overworld")
                .resolve("data").resolve("skysgrassslabs")
                .resolve(ModWorldState.DATA_NAME + ".dat");
    }
}
