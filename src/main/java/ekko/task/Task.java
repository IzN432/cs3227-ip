package ekko.task;

import java.time.LocalDate;

import ekko.datetime.DateTimeParser;

/**
 * Represents the common state and behavior of all task types.
 */
public abstract class Task {
    private boolean isMarked;
    private final String description;

    /**
     * Creates a task with a nonblank description safe for storage as a single line.
     *
     * @throws IllegalArgumentException if the description is null, blank, or contains
     *         control characters or the storage delimiter {@code |}.
     */
    protected Task(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Task descriptions cannot be empty.");
        }
        if (description.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Task descriptions cannot contain control characters.");
        }
        if (description.contains("|")) {
            throw new IllegalArgumentException("Task descriptions cannot contain '|'.");
        }
        this.description = description.trim();
        isMarked = false;
    }

    public boolean isMarked() {
        return isMarked;
    }

    public void setMarked(boolean isMarked) {
        this.isMarked = isMarked;
    }

    protected String getDescription() {
        return description;
    }

    /**
     * Compares task type and case-sensitive description, ignoring completion status.
     * Dated task types additionally compare their full date/time values.
     */
    public boolean hasSameDetails(Task other) {
        return other != null && getClass().equals(other.getClass()) && description.equals(other.description);
    }

    /**
     * Recreates a task from the representation produced by
     * {@link #toSerializedString()}.
     *
     * @param serializedTask one line of task data.
     * @return the corresponding concrete task.
     * @throws IllegalArgumentException if the task data is not in a recognized format.
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

    /**
     * Rejects records with missing or extra fields before reconstructing a task.
     */
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
     * @return serialized task data.
     */
    public abstract String toSerializedString();

    /**
     * Reports whether this task is associated with the specified calendar date.
     * Tasks without dates return {@code false} by default.
     *
     * @param date date to check.
     * @return whether this task occurs on that date.
     */
    public boolean occursOn(LocalDate date) {
        return false;
    }

    /**
     * Returns the completion marker and description used by each task's display text.
     */
    @Override
    public String toString() {
        return String.format("[%c] %s", isMarked ? 'X' : ' ', description);
    }
}
