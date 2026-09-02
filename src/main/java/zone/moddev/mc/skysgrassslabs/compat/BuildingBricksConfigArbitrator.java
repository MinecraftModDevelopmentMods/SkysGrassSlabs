package zone.moddev.mc.skysgrassslabs.compat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import net.minecraftforge.common.config.Configuration;

/** Internal, testable ownership hand-off for BuildingBricks world generation. */
final class BuildingBricksConfigArbitrator {
    static final String CATEGORY = "compat.vanilla";
    static final String PROPERTY = "generateGrassSlabs";

    static boolean disable(File configFile) throws IOException {
        Configuration configuration = new Configuration(configFile);
        configuration.load();
        if (!configuration.get(CATEGORY, PROPERTY, true).getBoolean()) {
            return false;
        }

        if (configFile.isFile()) {
            File backup = new File(configFile.getParentFile(),
                    "general.cfg.skysgrassslabs-backup");
            if (!backup.exists()) {
                Files.copy(configFile.toPath(), backup.toPath());
            }
        }

        configuration.get(CATEGORY, PROPERTY, true).set(false);
        configuration.save();

        Configuration verification = new Configuration(configFile);
        verification.load();
        if (verification.get(CATEGORY, PROPERTY, true).getBoolean()) {
            throw new IOException("BuildingBricks configuration did not retain generateGrassSlabs=false");
        }
        return true;
    }

    private BuildingBricksConfigArbitrator() {
    }
}
