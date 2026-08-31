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
        assertTrue(messages.stream().anyMatch(message -> message.contains("Here are the matching tasks")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("Here are the deadlines and events")));
        assertEquals(2, storage.loadTasks().size());
        assertEquals("T | 0 | read book", storage.loadTasks().getFirst().toSerializedString());

        messages.clear();
        Ekko reloaded = new Ekko(new GuiUi(messages::add, () -> "no"), storage);
        reloaded.processCommand("list");
        assertTrue(messages.getFirst().contains("1.[T][ ] read book"));
        assertTrue(reloaded.processCommand("bye"));
        assertEquals("Bye. Hope to see you again soon!", messages.getLast());
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
                assertEquals("Here are the tasks in your list:\n1.[T][ ] keep", messages.getLast(), input);
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
