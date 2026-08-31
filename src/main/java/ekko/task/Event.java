package ekko.task;

import java.time.LocalDate;
import java.time.LocalDateTime;

import ekko.datetime.DateTimeParser;

/**
 * Represents a task that takes place between specified start and end times.
 */
public class Event extends Task {
    private final LocalDateTime from;
    private final LocalDateTime to;

    /**
     * Creates an event with the supplied endpoints; command validation checks their order.
     *
     * @param description event description without a storage delimiter.
     * @param from start date and time.
     * @param to end date and time.
     * @throws IllegalArgumentException if either endpoint is null or the end is not after the start.
     */
    public Event(String description, LocalDateTime from, LocalDateTime to) {
        super(description);
        if (from == null || to == null || !to.isAfter(from)) {
            throw new IllegalArgumentException("An event's /to date/time must be after its /from date/time.");
        }
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean hasSameDetails(Task other) {
        return super.hasSameDetails(other)
                && from.equals(((Event) other).from) && to.equals(((Event) other).to);
    }

    @Override
    public String toSerializedString() {
        return String.format("E | %d | %s | %s | %s",
                isMarked() ? 1 : 0, getDescription(), from, to);
    }

    /**
     * Returns whether the date falls within the event's inclusive calendar-date interval.
     */
    @Override
    public boolean occursOn(LocalDate date) {
        return !date.isBefore(from.toLocalDate()) && !date.isAfter(to.toLocalDate());
    }

    /**
     * Returns the event type, completion marker, description, and formatted endpoints.
     */
    @Override
    public String toString() {
        return String.format("[E]%s (from: %s to: %s)",
                super.toString(), DateTimeParser.format(from), DateTimeParser.format(to));
    }
}
