package ekko.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ekko.task.Task;

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
        return tasks;
    }

    /**
     * Replaces the data file with one textual task representation per line.
     *
     * @param tasks current tasks to save.
     * @throws IOException if the data directory or file cannot be written.
     */
    public void saveTasks(List<Task> tasks) throws IOException {
        Files.createDirectories(dataFile.getParent());
        Files.write(dataFile, tasks.stream().map(Task::toSerializedString).toList());
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
