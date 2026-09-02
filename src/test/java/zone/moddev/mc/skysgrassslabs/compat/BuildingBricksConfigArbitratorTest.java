package zone.moddev.mc.skysgrassslabs.compat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildingBricksConfigArbitratorTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void provideForgeMinecraftHome() throws Exception {
        java.lang.reflect.Field field = FMLInjectionData.class.getDeclaredField("minecraftHome");
        field.setAccessible(true);
        field.set(null, temporaryDirectory.toFile());
    }

    @Test
    void enabledWorldgenIsBackedUpThenDisabled() throws Exception {
        File file = temporaryDirectory.resolve("BuildingBricks/general.cfg").toFile();
        write(file, true);
        byte[] original = Files.readAllBytes(file.toPath());

        assertTrue(BuildingBricksConfigArbitrator.disable(file));

        File backup = new File(file.getParentFile(), "general.cfg.skysgrassslabs-backup");
        assertTrue(backup.isFile());
        assertArrayEquals(original, Files.readAllBytes(backup.toPath()));
        assertFalse(read(file));
    }

    @Test
    void disabledWorldgenIsLeftByteForByteUntouched() throws Exception {
        File file = temporaryDirectory.resolve("BuildingBricks/general.cfg").toFile();
        write(file, false);
        byte[] original = Files.readAllBytes(file.toPath());

        assertFalse(BuildingBricksConfigArbitrator.disable(file));

        assertArrayEquals(original, Files.readAllBytes(file.toPath()));
        assertFalse(new File(file.getParentFile(),
                "general.cfg.skysgrassslabs-backup").exists());
    }

    @Test
    void existingOneTimeBackupIsNeverOverwritten() throws Exception {
        File file = temporaryDirectory.resolve("BuildingBricks/general.cfg").toFile();
        write(file, true);
        File backup = new File(file.getParentFile(), "general.cfg.skysgrassslabs-backup");
        byte[] sentinel = "first backup".getBytes("UTF-8");
        Files.write(backup.toPath(), sentinel);

        assertTrue(BuildingBricksConfigArbitrator.disable(file));
        assertArrayEquals(sentinel, Files.readAllBytes(backup.toPath()));
    }

    private static void write(File file, boolean enabled) {
        Configuration configuration = new Configuration(file);
        configuration.load();
        configuration.get(BuildingBricksConfigArbitrator.CATEGORY,
                BuildingBricksConfigArbitrator.PROPERTY, true).set(enabled);
        configuration.save();
    }

    private static boolean read(File file) {
        Configuration configuration = new Configuration(file);
        configuration.load();
        return configuration.get(BuildingBricksConfigArbitrator.CATEGORY,
                BuildingBricksConfigArbitrator.PROPERTY, true).getBoolean();
    }
}
