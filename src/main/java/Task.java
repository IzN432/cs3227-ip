/**
 * Represents the common state and behaviour of all task types.
 */
public abstract class Task {
    private boolean marked;
    private final String description;

    protected Task(String description) {
        this.description = description;
        marked = false;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    @Override
    public String toString() {
        return String.format("[%c] %s", marked ? 'X' : ' ', description);
    }
}
