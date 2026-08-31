package ekko;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ekko.storage.Storage;
import ekko.ui.GuiUi;

/**
 * Checks the GUI command boundary without depending on a desktop or console streams.
 */
class GuiCommandTest {
    @TempDir
    Path directory;

    @Test
    void processCommand_invalidFormats_preservesMemoryAndFileAndContinues() throws IOException {
        List<String> messages = new ArrayList<>();
        Path file = directory.resolve("tasks.txt");
        Ekko ekko = new Ekko(new GuiUi(messages::add, () -> "no"), new Storage(file));
        ekko.processCommand("todo keep");
        String saved = Files.readString(file);
        String[][] commands = {
            {"deadline book /by 2026-09-01 /by 2026-09-02", "Specify /by only once."},
            {"event trip /from 2026-09-01 /to 2026-09-02 /from 2026-08-31", "Specify /from only once."},
            {"event trip /from 2026-09-01 /to 2026-09-02 /to", "Specify /to only once."},
            {"event trip /from 2026-09-01 /to 2026-09-01",
                "An event's /to date/time must be after its /from date/time."},
            {"todo keep", "A task with the same details already exists."},
            {"list extra", "The list command does not accept arguments."},
            {"bye extra", "The bye command does not accept arguments."},
            {"todo first\nsecond", "Task descriptions cannot contain control characters."}
        };
        for (String[] command : commands) {
            messages.clear();
            assertFalse(ekko.processCommand(command[0]), command[0]);
            assertEquals(List.of(command[1]), messages, command[0]);
            assertEquals(saved, Files.readString(file));
            ekko.processCommand("list");
            assertEquals("Human memory is unreliable. Fortunately, I kept a list:\n1.[T][ ] keep",
                    messages.getLast());
        }
        assertFalse(ekko.processCommand("  todo   another task  "));
        assertEquals(2, new Storage(file).loadTasks().size());
    }

    @Test
    void processCommand_duplicateDatedTasks_usesParsedDatesAndIgnoresCompletion() throws IOException {
        List<String> messages = new ArrayList<>();
        Storage storage = new Storage(directory.resolve("tasks.txt"));
        Ekko ekko = new Ekko(new GuiUi(messages::add, () -> "no"), storage);
        ekko.processCommand("deadline book /by 2026-09-01");
        ekko.processCommand("event trip /from 2026-09-01 /to 2026-09-02");
        ekko.processCommand("mark 1");
        for (String command : List.of("deadline book /by 1/9/2026",
                "event trip /from 1/9/2026 /to 2/9/2026")) {
            messages.clear();
            assertFalse(ekko.processCommand(command));
            assertEquals(List.of("A task with the same details already exists."), messages);
        }
        assertEquals(2, storage.loadTasks().size());
        assertTrue(storage.loadTasks().getFirst().isMarked());
    }

    @Test
    void constructor_semanticallyInvalidStorage_preservesFileWhenRecoveryDeclined() throws IOException {
        Path file = directory.resolve("tasks.txt");
        for (String data : List.of("T | 0 | keep\nT | 1 | keep",
                "E | 0 | trip | 2026-09-02 | 2026-09-01", "T | 0 | ")) {
            Files.writeString(file, data);
            List<String> messages = new ArrayList<>();
            Ekko ekko = new Ekko(new GuiUi(messages::add, () -> "no"), new Storage(file));
            assertFalse(ekko.canStart());
            assertTrue(ekko.processCommand("todo overwrite"));
            assertTrue(messages.getFirst().contains("stored task data is invalid"));
            assertEquals(data, Files.readString(file));
        }
    }

    @Test
    void processCommand_taskLifecycle_displaysResponsesAndPersistsChanges() throws IOException {
        List<String> messages = new ArrayList<>();
        Storage storage = new Storage(directory.resolve("tasks.txt"));
        Ekko ekko = new Ekko(new GuiUi(messages::add, () -> "no"), storage);
        for (String command : List.of("  todo read book  ", "deadline report /by 2026-09-02",
                "event lunch /from 2026-09-02 1200 /to 2026-09-02 1300", "mark 1", "list",
                "find book", "agenda 2026-09-02", "unmark 1", "delete 3")) {
            assertFalse(ekko.processCommand(command));
        }
        assertTrue(messages.stream().anyMatch(message -> message.contains("1.[T][X] read book")));
        assertTrue(messages.stream().anyMatch(message ->
                message.contains("Search complete. These tasks match your request")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("Your scheduled obligations")));
        assertEquals(2, storage.loadTasks().size());
        assertEquals("T | 0 | read book", storage.loadTasks().getFirst().toSerializedString());

        messages.clear();
        Ekko reloaded = new Ekko(new GuiUi(messages::add, () -> "no"), storage);
        reloaded.processCommand("list");
        assertTrue(messages.getFirst().contains("1.[T][ ] read book"));
        assertTrue(reloaded.processCommand("bye"));
        assertEquals("Ekko offline. You are briefly responsible for yourself.", messages.getLast());
    }

    @Test
    void processCommand_filteredNumbers_targetFullListAndRefreshAfterDeletion() throws IOException {
        for (String command : List.of("find book", "agenda 2026-09-02")) {
            Path file = directory.resolve(command.split(" ")[0] + ".txt");
            String savedTasks = "T | 0 | exercise\n"
                    + "D | 1 | return book | 2026-09-02T00:00\n"
                    + "T | 0 | laundry\n"
                    + "E | 0 | book club | 2026-09-02T12:00 | 2026-09-02T13:00\n";
            Files.writeString(file, savedTasks);
            List<String> messages = new ArrayList<>();
            Storage storage = new Storage(file);
            Ekko ekko = new Ekko(new GuiUi(messages::add, () -> "no"), storage);

            assertFalse(ekko.processCommand(command));
            String expectedTasks = "\n2.[D][X] return book (by: Sep 02 2026)\n"
                    + "4.[E][ ] book club (from: Sep 02 2026, 12:00 PM to: Sep 02 2026, 1:00 PM)";
            assertEquals(expectedTasks, messages.getLast().substring(messages.getLast().indexOf('\n')));
            assertEquals(savedTasks, Files.readString(file));

            ekko.processCommand("unmark 2");
            assertFalse(storage.loadTasks().get(1).isMarked());
            ekko.processCommand("mark 4");
            assertTrue(storage.loadTasks().get(3).isMarked());
            assertFalse(storage.loadTasks().getFirst().isMarked());
            ekko.processCommand("delete 2");
            assertEquals(List.of("T | 0 | exercise", "T | 0 | laundry",
                    "E | 1 | book club | 2026-09-02T12:00 | 2026-09-02T13:00"),
                    storage.loadTasks().stream().map(task -> task.toSerializedString()).toList());

            ekko.processCommand(command);
            assertTrue(messages.getLast().endsWith(
                    "\n3.[E][X] book club (from: Sep 02 2026, 12:00 PM to: Sep 02 2026, 1:00 PM)"));
            assertFalse(messages.getLast().contains("return book"));
        }
    }

    @Test
    void processCommand_invalidInput_reportsErrorsAndAllowsNextCommand() throws IOException {
        List<String> messages = new ArrayList<>();
        Path file = directory.resolve("tasks.txt");
        Ekko ekko = new Ekko(new GuiUi(messages::add, () -> "no"), new Storage(file));
        for (String command : List.of("  ", "unknown", "todo", "mark 1", "deadline book /by bad")) {
            assertFalse(ekko.processCommand(command));
        }
        assertEquals(5, messages.size());
        assertEquals("Please enter a command.", messages.getFirst());
        assertFalse(Files.exists(file));
        assertFalse(ekko.processCommand("todo valid"));
        assertTrue(Files.readString(file).contains("valid"));
    }

    @Test
    void processCommand_invalidTaskNumbers_preservesTasksAndReportsConsistentErrors() throws IOException {
        List<String> messages = new ArrayList<>();
        Path file = directory.resolve("tasks.txt");
        Ekko ekko = new Ekko(new GuiUi(messages::add, () -> "no"), new Storage(file));
        ekko.processCommand("todo keep");
        String savedTasks = Files.readString(file);
        String[][] invalidArguments = {
            {"", "Please provide a task number."},
            {"   ", "Please provide a task number."},
            {"first", "Please provide a valid task number."},
            {"1 2", "Please provide a valid task number."},
            {"2147483648", "Please provide a valid task number."},
            {"-2147483649", "Please provide a valid task number."},
            {"0", "Please input a valid task number. You can send list to see how many tasks you have."},
            {"-1", "Please input a valid task number. You can send list to see how many tasks you have."},
            {"2", "Please input a valid task number. You can send list to see how many tasks you have."}
        };

        for (String command : List.of("mark", "unmark", "delete")) {
            for (String[] invalidArgument : invalidArguments) {
                String input = command + " " + invalidArgument[0];
                messages.clear();
                assertFalse(ekko.processCommand(input), input);
                assertEquals(List.of(invalidArgument[1]), messages, input);
                assertEquals(savedTasks, Files.readString(file), input);
                ekko.processCommand("list");
                assertEquals("Human memory is unreliable. Fortunately, I kept a list:\n1.[T][ ] keep",
                        messages.getLast(), input);
            }
        }
    }

    @Test
    void processCommand_recoveryDeclined_preservesFileAndBlocksCommands() throws IOException {
        Path file = directory.resolve("tasks.txt");
        Files.writeString(file, "invalid saved data");
        List<String> messages = new ArrayList<>();
        Ekko ekko = new Ekko(new GuiUi(messages::add, () -> "no"), new Storage(file));
        assertFalse(ekko.canStart());
        assertTrue(ekko.processCommand("todo must not overwrite"));
        assertEquals("invalid saved data", Files.readString(file));
        assertTrue(messages.getLast().contains("The data file was kept"));
    }

    @Test
    void processCommand_recoveryAccepted_startsFreshAndSaves() throws IOException {
        Path file = directory.resolve("tasks.txt");
        Files.writeString(file, "invalid saved data");
        Ekko ekko = new Ekko(new GuiUi(message -> { }, () -> "yes"), new Storage(file));
        assertTrue(ekko.canStart());
        assertFalse(Files.exists(file));
        assertFalse(ekko.processCommand("todo fresh start"));
        assertEquals("T | 0 | fresh start", Files.readString(file).strip());
    }
}
