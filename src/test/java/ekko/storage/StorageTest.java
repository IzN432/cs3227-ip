package ekko.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ekko.task.Deadline;
import ekko.task.Event;
import ekko.task.Task;
import ekko.task.Todo;

/**
 * Tests persistence entirely within JUnit-managed temporary directories.
 */
class StorageTest {
    @TempDir
    Path directory;

    @Test
    void loadTasks_invalidTaskDetails_rejectsWithoutChangingFile() throws IOException {
        Path file = directory.resolve("tasks.txt");
        for (String record : List.of("T | 0 | ", "T | 0 | same\nT | 1 | same",
                "E | 0 | trip | 2026-09-01 | 2026-09-01",
                "E | 0 | trip | 2026-09-02 | 2026-09-01")) {
            Files.writeString(file, record);
            assertThrows(IllegalArgumentException.class, () -> new Storage(file).loadTasks(), record);
            assertEquals(record, Files.readString(file));
        }
    }

    @Test
    void saveTasks_replacementFails_preservesDestinationAndCleansTemporaryFile() throws IOException {
        Path destination = Files.createDirectory(directory.resolve("tasks.txt"));
        Path sentinel = destination.resolve("keep.txt");
        Files.writeString(sentinel, "keep");
        assertThrows(IOException.class,
                () -> new Storage(destination).saveTasks(List.of(new Todo("new"))));
        assertEquals("keep", Files.readString(sentinel));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(destination), files.toList());
        }
    }

    @Test
    void saveTasks_invalidList_preservesExistingFile() throws IOException {
        Path file = directory.resolve("tasks.txt");
        Storage storage = new Storage(file);
        storage.saveTasks(List.of(new Todo("keep")));
        String saved = Files.readString(file);
        assertThrows(IllegalArgumentException.class,
                () -> storage.saveTasks(List.of(new Todo("same"), new Todo("same"))));
        assertEquals(saved, Files.readString(file));
    }

    @Test
    void loadTasks_missingOrEmptyFile_returnsEmptyList() throws IOException {
        Path file = directory.resolve("data/tasks.txt");
        Storage storage = new Storage(file);
        assertTrue(storage.loadTasks().isEmpty());
        assertFalse(Files.exists(file));
        Files.createDirectories(file.getParent());
        Files.writeString(file, "");
        assertTrue(storage.loadTasks().isEmpty());
    }

    @Test
    void saveTasks_createsParentsAndRoundTripsAllTypesAndStates() throws IOException {
        Path file = directory.resolve("nested/data/tasks.txt");
        Storage storage = new Storage(file);
        LocalDateTime date = LocalDateTime.of(2026, 9, 1, 18, 0);
        Todo todo = new Todo("café 学习");
        todo.setMarked(true);
        Event event = new Event("meeting", date, date.plusHours(2));
        event.setMarked(true);
        List<Task> tasks = List.of(todo, new Deadline("book", date), event);
        storage.saveTasks(tasks);
        assertEquals(tasks.stream().map(Task::toSerializedString).toList(), Files.readAllLines(file));
        List<Task> loaded = storage.loadTasks();
        assertEquals(tasks.stream().map(Task::toString).toList(), loaded.stream().map(Task::toString).toList());
        assertEquals(List.of(Todo.class, Deadline.class, Event.class),
                loaded.stream().map(Task::getClass).toList());
    }

    @Test
    void saveTasks_existingFile_replacesRatherThanAppends() throws IOException {
        Path file = directory.resolve("tasks.txt");
        Storage storage = new Storage(file);
        storage.saveTasks(List.of(new Todo("first"), new Todo("second")));
        storage.saveTasks(List.of(new Todo("replacement")));
        assertEquals(List.of("T | 0 | replacement"), Files.readAllLines(file));
        storage.saveTasks(List.of());
        assertEquals("", Files.readString(file));
        assertTrue(storage.loadTasks().isEmpty());
    }

    @Test
    void loadTasks_malformedRecord_throwsWithoutChangingFile() throws IOException {
        Path file = directory.resolve("tasks.txt");
        String invalid = "T | 0 | valid\nnot a task\n";
        Files.writeString(file, invalid);
        assertThrows(IllegalArgumentException.class, () -> new Storage(file).loadTasks());
        assertEquals(invalid, Files.readString(file));
    }

    @Test
    void loadTasks_invalidDate_propagatesParseFailure() throws IOException {
        Path file = directory.resolve("tasks.txt");
        Files.writeString(file, "D | 0 | bad | 2025-02-29");
        assertThrows(DateTimeParseException.class, () -> new Storage(file).loadTasks());
    }

    @Test
    void loadTasks_directoryInsteadOfFile_propagatesIoFailure() {
        assertThrows(IOException.class, () -> new Storage(directory).loadTasks());
    }

    @Test
    void saveTasks_parentIsFile_propagatesIoFailure() throws IOException {
        Path parent = directory.resolve("blocked");
        Files.writeString(parent, "keep");
        assertThrows(IOException.class,
                () -> new Storage(parent.resolve("tasks.txt")).saveTasks(List.of(new Todo("task"))));
        assertEquals("keep", Files.readString(parent));
    }

    @Test
    void deleteDataFile_existingThenMissing_returnsCorrectStatusAndKeepsSiblings() throws IOException {
        Path file = directory.resolve("tasks.txt");
        Path sibling = directory.resolve("keep.txt");
        Files.writeString(sibling, "keep");
        Storage storage = new Storage(file);
        assertFalse(storage.deleteDataFile());
        storage.saveTasks(List.of(new Todo("task")));
        assertTrue(storage.deleteDataFile());
        assertFalse(Files.exists(file));
        assertFalse(storage.deleteDataFile());
        assertEquals("keep", Files.readString(sibling));
    }

    @Test
    void deleteDataFile_nonEmptyDirectory_propagatesIoFailure() throws IOException {
        Files.writeString(directory.resolve("keep.txt"), "keep");
        assertThrows(IOException.class, () -> new Storage(directory).deleteDataFile());
        assertTrue(Files.exists(directory.resolve("keep.txt")));
    }
}
