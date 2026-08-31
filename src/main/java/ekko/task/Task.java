package ekko.task;

import ekko.datetime.DateTimeParser;

import java.time.LocalDate;

/**
 * Represents the common state and behaviour of all task types.
 */
public abstract class Task {
    private boolean marked;
    private final String description;

    /**
     * Creates a task whose description cannot conflict with the storage delimiter.
     *
     * @throws IllegalArgumentException if the description contains {@code |}
     */
    protected Task(String description) {
        if (description.contains("|")) {
            throw new IllegalArgumentException("Task descriptions cannot contain '|'.");
        }
        this.description = description;
        marked = false;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    protected String getDescription() {
        return description;
    }

    /**
     * Recreates a task from the representation produced by
     * {@link #toSerializedString()}.
     *
     * @param serializedTask one line of task data
     * @return the corresponding concrete task
     * @throws IllegalArgumentException if the task data is not in a recognised format
     */
    public static Task fromSerializedString(String serializedTask) {
        if (serializedTask == null) {
            throw invalidSerializedTask(null);
        }

        String[] fields = serializedTask.split("\\s*\\|\\s*", -1);
        if (fields.length < 2 || !(fields[1].equals("0") || fields[1].equals("1"))) {
            throw invalidSerializedTask(serializedTask);
        }

        Task task = switch (fields[0]) {
        case "T" -> {
            requireFieldCount(fields, 3);
            yield new Todo(fields[2]);
        }
        case "D" -> {
            requireFieldCount(fields, 4);
            yield new Deadline(fields[2], DateTimeParser.parse(fields[3]));
        }
        case "E" -> {
            requireFieldCount(fields, 5);
            yield new Event(fields[2], DateTimeParser.parse(fields[3]), DateTimeParser.parse(fields[4]));
        }
        default -> throw invalidSerializedTask(serializedTask);
        };
        task.setMarked(fields[1].equals("1"));
        return task;
    }

    private static void requireFieldCount(String[] fields, int expectedCount) {
        if (fields.length != expectedCount) {
            throw invalidSerializedTask(String.join(" | ", fields));
        }
    }

    private static IllegalArgumentException invalidSerializedTask(String serializedTask) {
        return new IllegalArgumentException("Invalid serialized task: " + serializedTask);
    }

    /**
     * Converts this task to the stable representation stored on disk.
     *
     * @return serialized task data
     */
    public abstract String toSerializedString();

    /**
     * Reports whether this task is associated with the specified calendar date.
     * Tasks without dates return {@code false} by default.
     *
     * @param date date to check
     * @return whether this task occurs on that date
     */
    public boolean occursOn(LocalDate date) {
        return false;
    }

    @Override
    public String toString() {
        return String.format("[%c] %s", marked ? 'X' : ' ', description);
    }
}
