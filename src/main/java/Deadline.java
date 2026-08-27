import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a task that must be completed by a specified date or time.
 */
public class Deadline extends Task {
    private final LocalDateTime by;

    public Deadline(String description, LocalDateTime by) {
        super(description);
        this.by = by;
    }

    @Override
    public String toSerializedString() {
        return String.format("D | %d | %s | %s",
                isMarked() ? 1 : 0, getDescription(), by);
    }

    @Override
    public boolean occursOn(LocalDate date) {
        return by.toLocalDate().equals(date);
    }

    @Override
    public String toString() {
        return String.format("[D]%s (by: %s)", super.toString(), DateTimeParser.format(by));
    }
}
