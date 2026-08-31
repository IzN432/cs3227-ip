package ekko.task;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests exact todo display and storage representations in both completion states. */
class TodoTest {
    @Test
    void toString_andSerialization_completionTransitions_preserveDescription() {
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
    void toString_emptyDescription_preservesEmptyValue() {
        Todo todo = new Todo("");
        assertEquals("[T][ ] ", todo.toString());
        assertEquals("T | 0 | ", todo.toSerializedString());
    }
}
