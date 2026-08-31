package ekko;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import ekko.storage.Storage;
import ekko.task.Task;
import ekko.ui.Ui;

/**
 * Tests the application loop directly and the real main entry point in child JVMs.
 * Each child uses its own temporary working directory, never the user's task file.
 */
class EkkoTest {
    @TempDir
    Path directory;

    /**
     * Runs JUnit-owned command scenarios against the real application entry point.
     */
    @TestFactory
    Stream<DynamicTest> main_commandScenarios_matchExpectedOutput() {
        return CommandScenarios.all().stream().map(scenario ->
                DynamicTest.dynamicTest(scenario.name(), () -> {
                    Path working = Files.createTempDirectory(directory, scenario.name() + "-");
                    Path file = working.resolve("data/ekko.txt");
                    if (scenario.initialData() != null) {
                        Files.createDirectories(file.getParent());
                        Files.writeString(file, scenario.initialData() + "\n");
                    }
                    assertEquals(normalize(scenario.expected()),
                            normalize(runMain(working, scenario.input())), scenario.name());
                    if (scenario.name().equals("LOAD-02")) {
                        assertFalse(Files.exists(file));
                    } else if (scenario.name().equals("LOAD-03")) {
                        assertEquals(scenario.initialData() + "\n", Files.readString(file));
                    }
                }));
    }

    @Test
    void mainLoop_mutations_persistAcrossNewApplicationInstances() throws IOException {
        Storage storage = new Storage(directory.resolve("tasks.txt"));
        runLoop(storage, "todo first\ntodo second\nmark 1\ndelete 2\nbye\n");
        assertEquals(List.of("T | 1 | first"),
                storage.loadTasks().stream().map(Task::toSerializedString).toList());
        String output = runLoop(storage, "list\nunmark 1\nbye\n");
        assertTrue(output.contains("1.[T][X] first"));
        assertEquals("T | 0 | first", storage.loadTasks().getFirst().toSerializedString());
    }

    @Test
    void mainLoop_invalidCommands_continueWithoutSavingTasks() throws IOException {
        Path file = directory.resolve("tasks.txt");
        String output = runLoop(new Storage(file),
                "\nunknown\nevent meeting /from bad /to 2026-09-01\n"
                        + "deadline book /by bad\nmark 2147483648\nbye\n");
        assertTrue(output.contains("Please enter a command."));
        assertTrue(output.contains("Unknown command. A command reference has been provided. Use it."));
        assertEquals(2, output.lines().filter(line -> line.equals(
                "Please use a valid date/time such as 2019-10-15 or 2/12/2019 1800.")).count());
        assertTrue(output.contains("Please provide a valid task number."));
        assertTrue(output.contains("Ekko offline. You are briefly responsible for yourself."));
        assertFalse(Files.exists(file));
    }

    @Test
    void mainLoop_pipeInDescription_rejectsEveryTaskTypeWithoutChangingSavedTasks() throws IOException {
        Storage storage = new Storage(directory.resolve("tasks.txt"));
        runLoop(storage, "todo keep\nbye\n");
        for (String command : List.of("todo read a | b",
                "deadline read a | b /by 2026-09-01",
                "event read a | b /from 2026-09-01 /to 2026-09-02")) {
            String output = runLoop(storage, command + "\nlist\nbye\n");
            assertEquals("Task descriptions cannot contain '|'.\n"
                    + "Human memory is unreliable. Fortunately, I kept a list:\n1.[T][ ] keep\n"
                    + "Ekko offline. You are briefly responsible for yourself.", normalize(output), command);
            assertEquals(List.of("T | 0 | keep"),
                    Files.readAllLines(directory.resolve("tasks.txt")), command);
        }
    }

    @Test
    void mainLoop_successErrorAndBye_preservesConsoleSpacing() throws IOException {
        Path file = directory.resolve("tasks.txt");
        String output = runLoop(new Storage(file), "list\nunknown\nbye\ntodo ignored\n")
                .replace("\r\n", "\n");
        String separator = "-".repeat(80) + "\n";
        String expectedTranscript = "What's on your agenda?\n\n"
                + separator + "\n" + separator
                + "Nothing on your agenda. I will assume this is an achievement.\n\n"
                + separator + "\n" + separator
                + "Unknown command. A command reference has been provided. Use it.\n\n"
                + separator + "\n" + separator
                + "Ekko offline. You are briefly responsible for yourself.\n\n" + separator;

        assertTrue(output.endsWith(expectedTranscript), output);
        assertFalse(Files.exists(file));
    }

    @Test
    void mainLoop_bye_ignoresFollowingCommands() throws IOException {
        Path file = directory.resolve("tasks.txt");
        runLoop(new Storage(file), "bye\ntodo ignored\n");
        assertFalse(Files.exists(file));
    }

    @Test
    void mainLoop_find_displaysNumberedMatchesWithoutChangingSavedTasks() throws IOException {
        Path file = directory.resolve("tasks.txt");
        String savedTasks = "T | 0 | exercise\nT | 1 | read book\n"
                + "D | 0 | return book | 2026-09-01T00:00\n";
        Files.writeString(file, savedTasks);

        String output = runLoop(new Storage(file), "find book\nfind   read book  \nbye\n");

        assertEquals("Search complete. These tasks match your request:\n"
                + "2.[T][X] read book\n3.[D][ ] return book (by: Sep 01 2026)\n"
                + "Search complete. These tasks match your request:\n2.[T][X] read book\n"
                + "Ekko offline. You are briefly responsible for yourself.", normalize(output));
        assertEquals(savedTasks, Files.readString(file));
    }

    @Test
    void mainLoop_findEmptyOrBlank_reportsOutcomeAndContinuesWithoutSaving() throws IOException {
        Path file = directory.resolve("tasks.txt");
        String output = runLoop(new Storage(file), "find book\nfind\nfind \t \nbye\n");
        assertEquals("No matching tasks. Check your spelling before questioning my competence.\n"
                + "Please provide a keyword to find.\n"
                + "Please provide a keyword to find.\nEkko offline. You are briefly responsible for yourself.",
                normalize(output));
        assertFalse(Files.exists(file));
    }

    @Test
    void mainLoop_multipleDatedTaskErrors_preservesValidationOrderAndSavedTasks() throws IOException {
        Path file = directory.resolve("tasks.txt");
        String savedTasks = "T | 0 | keep\n";
        Files.writeString(file, savedTasks);
        String[][] invalidCommands = {
            {"deadline /by bad", "The description of a deadline cannot be empty."},
            {"deadline /by", "The description of a deadline cannot be empty."},
            {"event /from bad /to bad", "The description of an event cannot be empty."},
            {"event meeting", "An event must have a non-empty /from argument."},
            {"event meeting /to bad", "An event must have a non-empty /from argument."},
            {"event meeting /from bad", "An event must have a non-empty /to argument."},
            {"event meeting /from bad /to", "An event must have a non-empty /to argument."}
        };

        for (String[] invalidCommand : invalidCommands) {
            String output = runLoop(new Storage(file), invalidCommand[0] + "\nlist\nbye\n");
            assertEquals(invalidCommand[1] + "\nHuman memory is unreliable. Fortunately, I kept a list:\n"
                    + "1.[T][ ] keep\n"
                    + "Ekko offline. You are briefly responsible for yourself.", normalize(output), invalidCommand[0]);
            assertEquals(savedTasks, Files.readString(file), invalidCommand[0]);
        }
    }

    @Test
    void mainLoop_endOfInput_exitsCleanlyAfterSaving() throws IOException {
        Storage storage = new Storage(directory.resolve("tasks.txt"));
        String output = runLoop(storage, "todo keep\n");
        assertTrue(output.contains("Ekko offline."));
        assertEquals(1, storage.loadTasks().size());
    }

    @Test
    void mainLoop_equalEventEndpoints_rejectsWithoutSaving() throws IOException {
        Storage storage = new Storage(directory.resolve("tasks.txt"));
        String output = runLoop(storage,
                "event meeting /from 2026-09-01 1800 /to 2026-09-01 1800\nbye\n");
        assertTrue(output.contains("An event's /to date/time must be after its /from date/time."));
        assertTrue(storage.loadTasks().isEmpty());
    }

    @Test
    void constructor_loadFailure_propagatesIoException() {
        assertThrows(IOException.class,
                () -> new Ekko(ui("bye\n", new ByteArrayOutputStream()), new Storage(directory)));
    }

    @Test
    void mainLoop_saveFailure_propagatesWithoutSuccessMessage() throws IOException {
        Path file = directory.resolve("data/tasks.txt");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Ekko app = new Ekko(ui("todo cannot save\nbye\n", output), new Storage(file));
        // Startup sees a missing file; subsequently block creation of its parent.
        Files.writeString(file.getParent(), "blocked");
        assertThrows(IOException.class, app::mainLoop);
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("Added to your agenda"));
    }

    @Test
    void main_unreadableStorage_reportsActionableErrorWithoutStackTrace() throws Exception {
        Files.createDirectories(directory.resolve("data/ekko.txt"));
        String output = runMain(directory, "");
        assertTrue(output.contains("Could not access the task file:"));
        assertTrue(output.contains("Fix the file access problem and restart Ekko."));
        assertEquals("", Files.readString(directory.resolve("stderr.txt")));
    }

    @Test
    void main_recoveryConfirmationVariants_deleteOnlyAfterAffirmativeResponse() throws Exception {
        for (String response : new String[] {"y", " YES ", "no", "maybe", ""}) {
            Path working = Files.createTempDirectory(directory, "recovery-");
            Path file = working.resolve("data/ekko.txt");
            Files.createDirectories(file.getParent());
            Files.writeString(file, "invalid");
            boolean isAffirmative = response.trim().equalsIgnoreCase("y")
                    || response.trim().equalsIgnoreCase("yes");
            String input = response.isEmpty() ? "" : response + "\nbye\n";
            String output = runMain(working, input);
            if (isAffirmative) {
                assertFalse(Files.exists(file));
                assertTrue(output.contains("What's on your agenda?"));
                assertTrue(output.contains("Ekko offline. You are briefly responsible for yourself."));
            } else {
                assertEquals("invalid", Files.readString(file));
                assertTrue(output.contains("The data file was kept."));
                assertFalse(output.contains("What's on your agenda?"));
            }
        }
    }

    /**
     * Runs the public loop with caller-provided isolated dependencies.
     */
    private String runLoop(Storage storage, String input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new Ekko(ui(input, output), storage).mainLoop();
        return output.toString(StandardCharsets.UTF_8);
    }

    private Ui ui(String input, ByteArrayOutputStream output) {
        return new Ui(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    /**
     * Launches the real entry point with a bounded lifetime and redirected output.
     * File redirection avoids a full pipe blocking the child before it can exit.
     */
    private String runMain(Path working, String input) throws Exception {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        String classPath = Path.of(Ekko.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .toAbsolutePath().toString();
        Path output = working.resolve("stdout.txt");
        Path error = working.resolve("stderr.txt");
        Process process = new ProcessBuilder(java.toString(), "-ea", "-cp", classPath, "ekko.Ekko")
                .directory(working.toFile())
                .redirectOutput(output.toFile())
                .redirectError(error.toFile())
                .start();
        try {
            try (var inputStream = process.getOutputStream()) {
                inputStream.write(input.getBytes(StandardCharsets.UTF_8));
            }
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Ekko did not terminate");
            assertEquals(0, process.exitValue(), Files.readString(error));
            return Files.readString(output);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Ignores only stable chrome, retaining recovery messages and task indentation.
     */
    private String normalize(String output) {
        String text = output.replace("\r\n", "\n");
        int greeting = text.indexOf("What's on your agenda?");
        if (greeting >= 0) {
            int banner = text.indexOf("-".repeat(80));
            assertTrue(banner >= 0 && banner < greeting, "Missing startup separator");
            text = text.substring(0, banner) + text.substring(greeting + "What's on your agenda?".length());
        }
        return text.lines().map(String::stripTrailing)
                .filter(line -> !line.isEmpty() && !line.equals("-".repeat(80)))
                .collect(Collectors.joining("\n"));
    }
}
