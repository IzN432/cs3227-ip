package ekko.task;

import java.time.LocalDate;
import java.time.LocalDateTime;

import ekko.datetime.DateTimeParser;

/**
 * Represents a task that must be completed by a specified date or time.
 */
public class Deadline extends Task {
    private final LocalDateTime by;

    /**
     * Creates a task due at the specified date and time.
     *
     * @param description task description without a storage delimiter.
     * @param by deadline date and time.
     * @throws IllegalArgumentException if the deadline is null.
     */
    public Deadline(String description, LocalDateTime by) {
        super(description);
        if (by == null) {
            throw new IllegalArgumentException("A deadline must have a date/time.");
        }
        this.by = by;
    }

    @Override
    public boolean hasSameDetails(Task other) {
        return super.hasSameDetails(other) && by.equals(((Deadline) other).by);
    }

    @Override
    public String toSerializedString() {
        return String.format("D | %d | %s | %s",
                isMarked() ? 1 : 0, getDescription(), by);
    }

    /**
     * Returns whether the date matches this task's due date, regardless of completion.
     */
    @Override
    public boolean occursOn(LocalDate date) {
        return by.toLocalDate().equals(date);
    }

    /**
     * Returns the deadline type, completion marker, description, and formatted due date.
     */
    @Override
    public String toString() {
        return String.format("[D]%s (by: %s)", super.toString(), DateTimeParser.format(by));
    }
}
