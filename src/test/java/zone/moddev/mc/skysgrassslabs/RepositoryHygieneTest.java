package zone.moddev.mc.skysgrassslabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;

public class RepositoryHygieneTest {
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile(
            "(?i)(?<![a-z0-9])[a-z]:[\\\\/]");
    private static final Pattern USER_HOME_PATH = Pattern.compile(
            "(?i)(?<![a-z0-9])/(?:users|home)/[^\\s/]+(?:/|$)");
    private static final Pattern PERSONAL_EMAIL = Pattern.compile(
            "(?i)[a-z0-9._%+-]+@(outlook|hotmail|gmail|yahoo)\\.[a-z]{2,}");
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "bat", "gradle", "java", "json", "md", "mcmeta", "properties",
            "ps1", "sh", "toml", "txt", "xml", "yaml", "yml");
    private static final Set<String> TEXT_NAMES = Set.of(
            ".gitattributes", ".gitignore", "gradlew", "license", "notice");
    private static final Set<String> FORBIDDEN_DIRECTORY_SEGMENTS = Set.of(
            ".codex", ".claude", ".continue", ".cursor", ".gradle",
            ".gradle-verify-cache", ".idea", ".kilocode", ".roo", ".settings",
            ".vscode", ".windsurf", "agent-notes", "agent_notes", "bin", "build",
            "crash-reports", "dist", "eclipse", "evidence", "logs", "release", "run",
            "run-data", "test-output", "test-reports", "test-results");
    private static final Set<String> FORBIDDEN_FILE_NAMES = Set.of(
            ".classpath", ".project", "agent.md", "agents.md", "claude.md",
            "gemini.md", "secret.json");

    @Test
    public void trackedTreeContainsNoLocalOnlyFilesOrMachineSpecificText() throws Exception {
        assumeGitCheckout();

        CommandResult tracked = git("ls-files", "-z");
        assertEquals(tracked.output, 0, tracked.exitCode);

        for (String file : tracked.output.split("\\u0000")) {
            if (file.isEmpty()) {
                continue;
            }
            assertFalse("Local-only file is tracked: " + file, isLocalOnlyPath(file));
            if (isTextFile(file)) {
                String text = Files.readString(Path.of(file), StandardCharsets.UTF_8);
                assertFalse("Windows absolute path in tracked file: " + file,
                        WINDOWS_ABSOLUTE_PATH.matcher(text).find());
                assertFalse("User-home path in tracked file: " + file,
                        USER_HOME_PATH.matcher(text).find());
                assertFalse("Personal email address in tracked file: " + file,
                        PERSONAL_EMAIL.matcher(text).find());
            }
        }
    }

    @Test
    public void localContextPatternsAreIgnoredAtRootAndNestedDepths() throws Exception {
        assumeGitCheckout();

        List<String> ignoredCandidates = List.of(
                "AGENTS.md",
                "nested/AGENTS.md",
                "agent-notes/README.md",
                "nested/agent-notes/README.md",
                ".codex/state.json",
                "nested/.codex/state.json",
                ".vscode/settings.json",
                "nested/.continue/config.json",
                ".aider.conf.yml",
                ".github/copilot-instructions.md",
                "runClient.launch",
                ".settings/org.eclipse.buildship.core.prefs",
                ".classpath",
                ".project");

        for (String candidate : ignoredCandidates) {
            CommandResult result = git("check-ignore", "--quiet", "--no-index", "--", candidate);
            assertEquals("Expected ignored path: " + candidate + "\n" + result.output,
                    0, result.exitCode);
        }
    }

    private static void assumeGitCheckout() throws Exception {
        CommandResult result = git("rev-parse", "--is-inside-work-tree");
        assumeTrue("Repository hygiene checks require a Git checkout",
                result.exitCode == 0 && result.output.trim().equals("true"));
    }

    private static boolean isLocalOnlyPath(String file) {
        String normalized = file.replace('\\', '/').toLowerCase(Locale.ROOT);
        String[] segments = normalized.split("/");
        for (String segment : segments) {
            if (FORBIDDEN_DIRECTORY_SEGMENTS.contains(segment)) {
                return true;
            }
        }

        String name = segments[segments.length - 1];
        return FORBIDDEN_FILE_NAMES.contains(name)
                || name.endsWith(".launch")
                || name.endsWith(".log")
                || name.startsWith(".aider")
                || name.startsWith(".env")
                || name.contains("agent-handover");
    }

    private static boolean isTextFile(String file) {
        String name = Path.of(file).getFileName().toString().toLowerCase(Locale.ROOT);
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
                .directory(Path.of("").toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new CommandResult(process.waitFor(), output);
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
