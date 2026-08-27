import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves the current task list to the application's data file.
 */
public final class Storage {
    private static final Path DATA_FILE = Path.of("data", "ekko.txt");

    private Storage() {
    }

    /**
     * Loads tasks from the data file. A missing data directory or file represents
     * a new user with an empty task list.
     *
     * @return tasks stored in the data file, in their original order
     * @throws IOException if the data file cannot be read
     * @throws IllegalArgumentException if the file contains invalid task data
     */
    public static List<Task> loadTasks() throws IOException {
        if (Files.notExists(DATA_FILE)) {
            return new ArrayList<>();
        }

        List<Task> tasks = new ArrayList<>();
        for (String line : Files.readAllLines(DATA_FILE)) {
            tasks.add(Task.fromSerializedString(line));
        }
        return tasks;
    }

    /**
     * Replaces the data file with one textual task representation per line.
     *
     * @param tasks current tasks to save
     * @throws IOException if the data directory or file cannot be written
     */
    public static void saveTasks(List<Task> tasks) throws IOException {
        Files.createDirectories(DATA_FILE.getParent());
        Files.write(DATA_FILE, tasks.stream().map(Task::toSerializedString).toList());
    }
}
