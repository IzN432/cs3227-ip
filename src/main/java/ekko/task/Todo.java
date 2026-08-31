package ekko.task;

/**
 * Represents a task without an associated date or time.
 */
public class Todo extends Task {

    /**
     * Creates an undated task with the supplied description.
     */
    public Todo(String description) {
        super(description);
    }

    @Override
    public String toSerializedString() {
        return String.format("T | %d | %s", isMarked() ? 1 : 0, getDescription());
    }

    /**
     * Returns the todo type, completion marker, and description.
     */
    @Override
    public String toString() {
        return "[T]" + super.toString();
    }
}
