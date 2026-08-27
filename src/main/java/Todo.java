/**
 * Represents a task without an associated date or time.
 */
public class Todo extends Task {

    public Todo(String description) {
        super(description);
    }

    @Override
    public String toSerializedString() {
        return String.format("T | %d | %s", isMarked() ? 1 : 0, getDescription());
    }

    @Override
    public String toString() {
        return "[T]" + super.toString();
    }
}
