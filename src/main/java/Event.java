/**
 * Represents a task that takes place between specified start and end times.
 */
public class Event extends Task {
    private final String from;
    private final String to;

    public Event(String description, String from, String to) {
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
    public String toString() {
        return String.format("[E]%s (from: %s to: %s)",
                super.toString(), from, to);
    }
}
