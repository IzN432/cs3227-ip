package ekko.task;

import ekko.EkkoException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests list ownership, one-based indexing, state transitions, and agenda queries. */
class TaskListTest {
    @Test
    void constructor_andAsList_isolateListStructure() {
        Todo first = new Todo("first");
        List<Task> source = new ArrayList<>(List.of(first));
        TaskList tasks = new TaskList(source);
        source.clear();
        List<Task> snapshot = tasks.asList();
        assertEquals(List.of(first), snapshot);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(new Todo("bad")));
        tasks.add(new Todo("second"));
        assertEquals(1, snapshot.size());
        assertEquals(2, tasks.size());
    }

    @Test
    void add_andDelete_preserveOrderAndRenumber() throws EkkoException {
        Todo first = new Todo("first");
        Todo second = new Todo("second");
        Todo third = new Todo("third");
        TaskList tasks = new TaskList(List.of());
        assertTrue(tasks.isEmpty());
        tasks.add(first);
        tasks.add(second);
        tasks.add(third);
        assertSame(second, tasks.delete(2));
        assertEquals(List.of(first, third), tasks.asList());
        assertSame(third, tasks.delete(2));
        assertSame(first, tasks.delete(1));
        assertTrue(tasks.isEmpty());
    }

    @Test
    void mark_andUnmark_repeatedOperations_reportWhetherStateChanged() throws EkkoException {
        Todo first = new Todo("first");
        Todo last = new Todo("last");
        TaskList tasks = new TaskList(List.of(first, last));
        TaskList.TaskUpdate unchanged = tasks.unmark(1);
        assertSame(first, unchanged.task());
        assertFalse(unchanged.changed());
        TaskList.TaskUpdate marked = tasks.mark(2);
        assertSame(last, marked.task());
        assertTrue(marked.changed());
        assertTrue(last.isMarked());
        assertFalse(first.isMarked());
        assertFalse(tasks.mark(2).changed());
        assertTrue(tasks.unmark(2).changed());
        assertFalse(last.isMarked());
        assertFalse(tasks.unmark(2).changed());
        assertTrue(tasks.mark(1).changed());
    }

    @Test
    void mutations_invalidNumbers_throwWithoutChangingTasks() {
        Todo task = new Todo("keep");
        TaskList tasks = new TaskList(List.of(task));
        for (int number : new int[] {Integer.MIN_VALUE, -1, 0, 2, Integer.MAX_VALUE}) {
            assertThrows(EkkoException.class, () -> tasks.delete(number));
            assertThrows(EkkoException.class, () -> tasks.mark(number));
            assertThrows(EkkoException.class, () -> tasks.unmark(number));
            assertEquals(List.of(task), tasks.asList());
            assertFalse(task.isMarked());
        }
    }

    @Test
    void mutations_emptyList_throw() {
        TaskList tasks = new TaskList(List.of());
        assertThrows(EkkoException.class, () -> tasks.delete(1));
        assertThrows(EkkoException.class, () -> tasks.mark(1));
        assertThrows(EkkoException.class, () -> tasks.unmark(1));
    }

    @Test
    void findOn_mixedTasks_preservesOrderAndIncludesCompletedMatches() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        Deadline due = new Deadline("due", date.atTime(18, 0));
        due.setMarked(true);
        Event event = new Event("trip", date.minusDays(1).atStartOfDay(), date.plusDays(1).atStartOfDay());
        TaskList tasks = new TaskList(List.of(new Todo("undated"), due,
                new Deadline("later", date.plusDays(1).atStartOfDay()), event));
        List<Task> matches = tasks.findOn(date);
        assertEquals(List.of(due, event), matches);
        assertThrows(UnsupportedOperationException.class, () -> matches.clear());
        assertEquals(4, tasks.size());
        assertTrue(tasks.findOn(date.plusDays(10)).isEmpty());
        assertTrue(new TaskList(List.of()).findOn(date).isEmpty());
    }
}
