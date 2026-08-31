package ekko.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import ekko.task.Task;
import ekko.task.TaskList;

/**
 * Saves the current task list to the application's data file.
 */
public final class Storage {
    private final Path dataFile;

    /**
     * Creates storage at the application's default data-file location.
     */
    public Storage() {
        this(Path.of("data", "ekko.txt"));
    }

    /**
     * Creates storage at a supplied location, allowing isolated temporary files in tests.
     *
     * @param dataFile file used for both loading and saving.
     */
    public Storage(Path dataFile) {
        this.dataFile = dataFile.toAbsolutePath();
    }

    /**
     * Loads tasks from the data file. A missing data directory or file represents
     * a new user with an empty task list.
     *
     * @return tasks stored in the data file, in their original order.
     * @throws IOException if the data file cannot be read.
     * @throws IllegalArgumentException if the file contains invalid task data.
     */
    public List<Task> loadTasks() throws IOException {
        if (Files.notExists(dataFile)) {
            return new ArrayList<>();
        }

        List<Task> tasks = new ArrayList<>();
        for (String line : Files.readAllLines(dataFile)) {
            tasks.add(Task.fromSerializedString(line));
        }
        return new TaskList(tasks).asList();
    }

    /**
     * Replaces the data file with one textual task representation per line.
     * Writes to a sibling temporary file before atomically replacing the destination.
     *
     * @param tasks current tasks to save.
     * @throws IOException if writing or atomic replacement fails, including unsupported atomic moves.
     * @throws IllegalArgumentException if the task list contains null or duplicate tasks.
     */
    public void saveTasks(List<Task> tasks) throws IOException {
        Files.createDirectories(dataFile.getParent());
        // Serialize and validate before touching the destination or creating a temporary file.
        List<String> records = new TaskList(tasks).asList().stream().map(Task::toSerializedString).toList();
        Path temporaryFile = Files.createTempFile(dataFile.getParent(), "ekko-", ".tmp");
        try {
            Files.write(temporaryFile, records);
            // Fail safely if the filesystem cannot atomically replace the saved file.
            Files.move(temporaryFile, dataFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupFailure) {
                e.addSuppressed(cleanupFailure);
            }
            throw e;
        }
    }

    /**
     * Deletes the task data file when it exists.
     *
     * @return {@code true} if a file was deleted.
     * @throws IOException if the file cannot be deleted.
     */
    public boolean deleteDataFile() throws IOException {
        return Files.deleteIfExists(dataFile);
    }
}
