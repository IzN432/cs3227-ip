import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a task that takes place between specified start and end times.
 */
public class Event extends Task {
    private final LocalDateTime from;
    private final LocalDateTime to;

    public Event(String description, LocalDateTime from, LocalDateTime to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    @Override
    public String toSerializedString() {
        return String.format("E | %d | %s | %s | %s",
                isMarked() ? 1 : 0, getDescription(), from, to);
    }

    @Override
    public boolean occursOn(LocalDate date) {
        return !date.isBefore(from.toLocalDate()) && !date.isAfter(to.toLocalDate());
    }

    @Override
    public String toString() {
        return String.format("[E]%s (from: %s to: %s)",
                super.toString(), DateTimeParser.format(from), DateTimeParser.format(to));
    }
}
