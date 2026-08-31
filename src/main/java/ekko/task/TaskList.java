package ekko.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ekko.EkkoException;

/**
 * Owns the application's task collection and its list operations.
 */
public class TaskList {
    private final List<Task> tasks;

    /**
     * Creates a task list containing the supplied tasks in their original order.
     *
     * @param tasks initial tasks.
     */
    public TaskList(List<Task> tasks) {
        this.tasks = new ArrayList<>(tasks);
    }

    /**
     * Appends a task to the end of the displayed list.
     */
    public void add(Task task) {
        tasks.add(task);
    }

    /**
     * Removes and returns a task identified by its one-based display number.
     */
    public Task delete(int taskNumber) throws EkkoException {
        return tasks.remove(toIndex(taskNumber));
    }

    /**
     * Marks a task and reports whether its state changed.
     */
    public TaskUpdate mark(int taskNumber) throws EkkoException {
        Task task = get(taskNumber);
        boolean hasChanged = !task.isMarked();
        task.setMarked(true);
        return new TaskUpdate(task, hasChanged);
    }

    /**
     * Unmarks a task and reports whether its state changed.
     */
    public TaskUpdate unmark(int taskNumber) throws EkkoException {
        Task task = get(taskNumber);
        boolean hasChanged = task.isMarked();
        task.setMarked(false);
        return new TaskUpdate(task, hasChanged);
    }

    /**
     * Returns whether the list contains no tasks.
     */
    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    /**
     * Returns the number of tasks in the list.
     */
    public int size() {
        return tasks.size();
    }

    /**
     * Returns an immutable snapshot for display or persistence.
     */
    public List<Task> asList() {
        return List.copyOf(tasks);
    }

    /**
     * Finds dated tasks that occur on the specified date.
     */
    public List<Task> findOn(LocalDate date) {
        return tasks.stream().filter(task -> task.occursOn(date)).toList();
    }

    private Task get(int taskNumber) throws EkkoException {
        return tasks.get(toIndex(taskNumber));
    }

    /**
     * Converts a one-based display number to an index, rejecting numbers outside the list.
     */
    private int toIndex(int taskNumber) throws EkkoException {
        int index = taskNumber - 1;
        if (index < 0 || index >= tasks.size()) {
            throw new EkkoException(
                    "Please input a valid task number. You can send list to see how many tasks you have."
            );
        }
        return index;
    }

    /**
     * Describes the task affected by a mark or unmark operation.
     *
     * @param task affected task.
     * @param hasChanged whether the operation changed its completion state.
     */
    public record TaskUpdate(Task task, boolean hasChanged) {
    }
}
