package ekko.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests exact todo display and storage representations in both completion states.
 */
class TodoTest {
    @Test
    void toStringAndSerialization_completionTransitions_preserveDescription() {
        Todo todo = new Todo("read /about Java");
        assertEquals("[T][ ] read /about Java", todo.toString());
        assertEquals("T | 0 | read /about Java", todo.toSerializedString());
        todo.setMarked(true);
        assertEquals("[T][X] read /about Java", todo.toString());
        assertEquals("T | 1 | read /about Java", todo.toSerializedString());
        todo.setMarked(false);
        assertEquals("[T][ ] read /about Java", todo.toString());
    }

    @Test
    void constructor_emptyDescription_rejectsTask() {
        assertThrows(IllegalArgumentException.class, () -> new Todo(""));
    }
}
