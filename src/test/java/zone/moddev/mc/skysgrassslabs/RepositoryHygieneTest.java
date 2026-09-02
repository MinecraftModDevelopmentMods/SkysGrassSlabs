package zone.moddev.mc.skysgrassslabs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class RepositoryHygieneTest {
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile(
            "(?i)(?<![a-z0-9])[a-z]:[\\\\/]");
    private static final Pattern USER_HOME_PATH = Pattern.compile(
            "(?i)(?<![a-z0-9])/(?:users|home)/[^\\s/]+(?:/|$)");
    private static final Pattern PERSONAL_EMAIL = Pattern.compile(
            "(?i)[a-z0-9._%+-]+@(outlook|hotmail|gmail|yahoo)\\.[a-z]{2,}");
    private static final Set<String> TEXT_EXTENSIONS = setOf(
            "bat", "gradle", "java", "json", "md", "mcmeta", "properties",
            "ps1", "sh", "toml", "txt", "xml", "yaml", "yml");
    private static final Set<String> TEXT_NAMES = setOf(
            ".gitattributes", ".gitignore", "gradlew", "license", "notice");
    private static final Set<String> FORBIDDEN_DIRECTORY_SEGMENTS = setOf(
            ".codex", ".claude", ".continue", ".cursor", ".eclipse", ".gradle",
            ".gradle-verify-cache", ".idea", ".kilocode", ".metadata", ".roo",
            ".settings", ".vscode", ".windsurf", "agent-notes", "agent_notes", "bin",
            "build", "crash-reports", "dist", "eclipse", "evidence", "logs",
            "mcmodsrepo", "out", "release", "run", "run-data", "test-output",
            "test-reports", "test-results");
    private static final Set<String> FORBIDDEN_FILE_NAMES = setOf(
            ".classpath", ".project", "agent.md", "agents.md", "claude.md",
            "gemini.md", "secret.json");

    @Test
    public void trackedTreeContainsNoLocalOnlyFilesOrMachineSpecificText() throws Exception {
        assumeGitCheckout();

        CommandResult tracked = git("ls-files", "-z");
        assertEquals(0, tracked.exitCode, tracked.output);

        for (String file : tracked.output.split("\\u0000")) {
            if (file.isEmpty()) {
                continue;
            }
            assertFalse(isLocalOnlyPath(file), "Local-only file is tracked: " + file);
            if (isTextFile(file)) {
                String text = new String(Files.readAllBytes(Paths.get(file)),
                        StandardCharsets.UTF_8);
                assertFalse(WINDOWS_ABSOLUTE_PATH.matcher(text).find(),
                        "Windows absolute path in tracked file: " + file);
                assertFalse(USER_HOME_PATH.matcher(text).find(),
                        "User-home path in tracked file: " + file);
                assertFalse(PERSONAL_EMAIL.matcher(text).find(),
                        "Personal email address in tracked file: " + file);
            }
        }
    }

    @Test
    public void localContextPatternsAreIgnoredAtRootAndNestedDepths() throws Exception {
        assumeGitCheckout();

        List<String> ignoredCandidates = Arrays.asList(
                "AGENTS.md",
                "nested/AGENTS.md",
                "agent-notes/README.md",
                "nested/agent-notes/README.md",
                ".codex/state.json",
                "nested/.codex/state.json",
                ".claude/settings.json",
                "nested/.cursor/rules/project.md",
                ".vscode/settings.json",
                "nested/.continue/config.json",
                ".windsurf/rules/project.md",
                "nested/.roo/rules.md",
                ".kilocode/settings.json",
                ".aider.conf.yml",
                "project.code-workspace",
                ".github/copilot-instructions.md",
                ".github/instructions/project.instructions.md",
                "runClient.launch",
                "run-server-smoke/output.log",
                "evidence/report.txt",
                ".settings/org.eclipse.buildship.core.prefs",
                ".classpath",
                ".project");

        for (String candidate : ignoredCandidates) {
            CommandResult result = git("check-ignore", "--quiet", "--no-index", "--", candidate);
            assertEquals(0, result.exitCode,
                    "Expected ignored path: " + candidate + "\n" + result.output);
        }
    }

    @Test
    public void localOnlyClassifierCoversGeneratedAndLocalContext() {
        List<String> localOnlyCandidates = Arrays.asList(
                "AGENTS.md",
                "nested/agent-notes/README.md",
                ".github/copilot-instructions.md",
                ".github/instructions/project.instructions.md",
                "nested/.windsurf/rules/project.md",
                "run-server-smoke/output.log",
                "evidence/report.txt",
                ".metadata/.plugins/state.dat",
                "project.iml",
                "forge-1.10.2-changelog.txt",
                "README.txt",
                "workspace.code-workspace",
                ".env.local");
        for (String candidate : localOnlyCandidates) {
            assertTrue(isLocalOnlyPath(candidate), "Expected local-only path: " + candidate);
        }

        assertFalse(isLocalOnlyPath("README.md"));
        assertFalse(isLocalOnlyPath("docs/TESTING.md"));
        assertFalse(isLocalOnlyPath(".github/workflows/ci.yml"));
        assertFalse(isLocalOnlyPath(".env.example"));
    }

    private static void assumeGitCheckout() throws Exception {
        CommandResult result = git("rev-parse", "--is-inside-work-tree");
        assumeTrue(result.exitCode == 0 && result.output.trim().equals("true"),
                "Repository hygiene checks require a Git checkout");
    }

    private static boolean isLocalOnlyPath(String file) {
        String normalized = file.replace('\\', '/').toLowerCase(Locale.ROOT);
        if (normalized.equals(".github/copilot-instructions.md")
                || normalized.startsWith(".github/instructions/")) {
            return true;
        }

        String[] segments = normalized.split("/");
        for (String segment : segments) {
            if (FORBIDDEN_DIRECTORY_SEGMENTS.contains(segment)
                    || segment.startsWith("run-")) {
                return true;
            }
        }

        String name = segments[segments.length - 1];
        return FORBIDDEN_FILE_NAMES.contains(name)
                || name.endsWith(".launch")
                || name.endsWith(".log")
                || name.contains(".log.")
                || name.endsWith(".tmp")
                || name.endsWith(".temp")
                || name.endsWith(".bak")
                || name.endsWith(".orig")
                || name.endsWith(".rej")
                || name.endsWith(".jfr")
                || name.endsWith(".hprof")
                || name.endsWith(".dmp")
                || name.endsWith(".ipr")
                || name.endsWith(".iws")
                || name.endsWith(".iml")
                || name.startsWith(".aider")
                || (name.startsWith(".env") && !name.equals(".env.example"))
                || name.startsWith("hs_err_pid")
                || name.startsWith("replay_pid")
                || (name.startsWith("forge") && name.endsWith("changelog.txt"))
                || normalized.equals("readme.txt")
                || name.endsWith(".code-workspace")
                || name.contains("agent-handover");
    }

    private static boolean isTextFile(String file) {
        String name = Paths.get(file).getFileName().toString().toLowerCase(Locale.ROOT);
        if (TEXT_NAMES.contains(name)) {
            return true;
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 && TEXT_EXTENSIONS.contains(name.substring(dot + 1));
    }

    private static CommandResult git(String... arguments) throws IOException, InterruptedException {
        String[] command = new String[arguments.length + 1];
        command[0] = "git";
        System.arraycopy(arguments, 0, command, 1, arguments.length);

        Process process = new ProcessBuilder(Arrays.asList(command))
                .directory(Paths.get("").toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(readAllBytes(process.getInputStream()),
                StandardCharsets.UTF_8);
        return new CommandResult(process.waitFor(), output);
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static final class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}
