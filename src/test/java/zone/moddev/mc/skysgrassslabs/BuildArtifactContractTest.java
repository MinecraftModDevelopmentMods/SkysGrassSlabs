package zone.moddev.mc.skysgrassslabs;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildArtifactContractTest {
    @Test
    void checksumTaskDeclaresItsArtifactInputs() throws Exception {
        String build = new String(Files.readAllBytes(Paths.get("build.gradle")), StandardCharsets.UTF_8);
        String checksumTask = build.substring(build.indexOf("tasks.register('writeReleaseChecksums')"));
        assertTrue(checksumTask.contains("inputs.files(releaseFiles.map"),
                "Incremental builds must regenerate checksums when a release jar changes");
    }
}
