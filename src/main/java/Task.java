public class Task {
    private boolean marked;
    private final String task;

    public Task(String task) {
        this.task = task;
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
        return String.format("[%c] %s", marked ? 'X' : ' ', task);
    }
}
