package ekko.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ekko.EkkoException;

/**
 * Tests list ownership, one-based indexing, state transitions, and task queries.
 */
class TaskListTest {
    @Test
    void add_duplicateDetails_rejectsButDistinctTypesAndSchedulesRemainAllowed() throws EkkoException {
        var start = LocalDate.of(2026, 9, 1).atStartOfDay();
        List<Task> originals = List.of(new Todo("same"), new Deadline("same", start),
                new Event("same", start, start.plusDays(1)));
        TaskList tasks = new TaskList(originals);
        for (int index = 0; index < originals.size(); index++) {
            Task duplicate = Task.fromSerializedString(originals.get(index).toSerializedString());
            tasks.mark(index + 1);
            assertThrows(IllegalArgumentException.class, () -> tasks.add(duplicate));
            assertEquals(3, tasks.size());
        }
        tasks.add(new Todo("Same"));
        tasks.add(new Deadline("same", start.plusNanos(1)));
        tasks.add(new Event("same", start, start.plusDays(2)));
        tasks.add(new Event("same", start.plusNanos(1), start.plusDays(1)));
        assertEquals(7, tasks.size());
        assertThrows(IllegalArgumentException.class,
                () -> new TaskList(List.of(new Todo("same"), new Todo("same"))));
    }

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
    void markAndUnmark_repeatedOperations_reportWhetherStateChanged() throws EkkoException {
        Todo first = new Todo("first");
        Todo last = new Todo("last");
        TaskList tasks = new TaskList(List.of(first, last));
        TaskList.TaskUpdate unchanged = tasks.unmark(1);
        assertSame(first, unchanged.task());
        assertFalse(unchanged.hasChanged());
        TaskList.TaskUpdate marked = tasks.mark(2);
        assertSame(last, marked.task());
        assertTrue(marked.hasChanged());
        assertTrue(last.isMarked());
        assertFalse(first.isMarked());
        assertFalse(tasks.mark(2).hasChanged());
        assertTrue(tasks.unmark(2).hasChanged());
        assertFalse(last.isMarked());
        assertFalse(tasks.unmark(2).hasChanged());
        assertTrue(tasks.mark(1).hasChanged());
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

    @Test
    void find_mixedTasks_matchesDescriptionsAndPreservesState() throws EkkoException {
        LocalDate date = LocalDate.of(2026, 9, 1);
        Todo todo = new Todo("read book");
        todo.setMarked(true);
        Deadline deadline = new Deadline("return books", date.atStartOfDay());
        Event event = new Event("book club", date.atStartOfDay(), date.atTime(18, 0));
        List<Task> original = List.of(new Todo("exercise"), todo, deadline, event, new Todo("Book"));
        TaskList tasks = new TaskList(original);

        List<Task> matches = tasks.find("book");
        assertEquals(List.of(todo, deadline, event), matches);
        assertEquals(List.of(deadline), tasks.find("return book"));
        assertThrows(UnsupportedOperationException.class, matches::clear);
        assertEquals(original, tasks.asList());
        assertTrue(todo.isMarked());
        assertFalse(deadline.isMarked());
        assertFalse(event.isMarked());

        for (String keyword : List.of("BOOK", "missing", "[T]", "[X]", "2026", "by:", "from:")) {
            assertTrue(tasks.find(keyword).isEmpty(), keyword);
        }
        assertTrue(new TaskList(List.of()).find("book").isEmpty());
    }

    @Test
    void find_blankKeyword_rejectsSearch() {
        TaskList tasks = new TaskList(List.of(new Todo("keep")));
        for (String keyword : List.of("", " ", "\t")) {
            assertEquals("Please provide a keyword to find.",
                    assertThrows(EkkoException.class, () -> tasks.find(keyword)).getMessage());
        }
    }
}
