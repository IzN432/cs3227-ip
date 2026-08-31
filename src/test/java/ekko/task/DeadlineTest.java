package ekko.task;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests deadline date matching and exact display/storage output. */
class DeadlineTest {
    private final LocalDate date = LocalDate.of(2026, 9, 1);

    @Test
    void occursOn_onlyDueDate_matchesRegardlessOfCompletion() {
        Deadline task = new Deadline("book", date.atTime(23, 59));
        assertFalse(task.occursOn(date.minusDays(1)));
        assertTrue(task.occursOn(date));
        assertFalse(task.occursOn(date.plusDays(1)));
        task.setMarked(true);
        assertTrue(task.occursOn(date));
    }

    @Test
    void toString_andSerialization_midnightAndTimedValues_useExpectedFormats() {
        Deadline task = new Deadline("book", date.atStartOfDay());
        assertEquals("[D][ ] book (by: Sep 01 2026)", task.toString());
        assertEquals("D | 0 | book | 2026-09-01T00:00", task.toSerializedString());
        task = new Deadline("book", date.atTime(18, 5));
        task.setMarked(true);
        assertEquals("[D][X] book (by: Sep 01 2026, 6:05 PM)", task.toString());
        assertEquals("D | 1 | book | 2026-09-01T18:05", task.toSerializedString());
    }
}
