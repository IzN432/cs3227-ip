package ekko.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests persisted task reconstruction and shared task behavior through concrete types.
 */
class TaskTest {
    @Test
    void constructor_pipeInDescription_rejectsEveryTaskType() {
        LocalDateTime date = LocalDateTime.of(2026, 9, 1, 18, 0);
        for (String description : List.of("|", "|leading", "trailing|", "a | b", "a||b")) {
            assertThrows(IllegalArgumentException.class, () -> new Todo(description), description);
            assertThrows(IllegalArgumentException.class,
                    () -> new Deadline(description, date), description);
            assertThrows(IllegalArgumentException.class,
                    () -> new Event(description, date, date.plusHours(1)), description);
        }
    }

    @Test
    void fromSerializedString_allTypesAndStates_roundTrips() {
        LocalDateTime date = LocalDateTime.of(2026, 9, 1, 18, 5, 30, 123456789);
        List<Task> tasks = List.of(new Todo("read book"), new Deadline("return book", date),
                new Event("meeting", date, date.plusDays(1)));
        for (Task original : tasks) {
            for (boolean isMarked : new boolean[] {false, true}) {
                original.setMarked(isMarked);
                Task restored = Task.fromSerializedString(original.toSerializedString());
                assertEquals(original.getClass(), restored.getClass());
                assertEquals(isMarked, restored.isMarked());
                assertEquals(original.toSerializedString(), restored.toSerializedString());
                assertEquals(original.toString(), restored.toString());
            }
        }
    }

    @Test
    void fromSerializedString_compactDelimitersAndEmptyDescription_loadsTask() {
        assertEquals("[T][X] book", Task.fromSerializedString("T|1|book").toString());
        assertEquals("[T][ ] ", Task.fromSerializedString("T | 0 | ").toString());
        assertEquals("[T][ ] café 学习", Task.fromSerializedString("T | 0 | café 学习").toString());
    }

    @Test
    void fromSerializedString_invalidTypeOrState_throwsException() {
        for (String input : new String[] {"", "invalid", "X | 0 | book", "t | 0 | book",
                "T | 2 | book", "T | true | book", "T | | book"}) {
            assertThrows(IllegalArgumentException.class, () -> Task.fromSerializedString(input), input);
        }
        assertThrows(IllegalArgumentException.class, () -> Task.fromSerializedString(null));
    }

    @Test
    void fromSerializedString_wrongFieldCounts_throwsException() {
        for (String input : new String[] {"T | 0", "T | 0 | book | extra",
                "D | 0 | book", "D | 0 | book | 2026-09-01 | extra",
                "E | 0 | meeting | 2026-09-01",
                "E | 0 | meeting | 2026-09-01 | 2026-09-02 | extra"}) {
            assertThrows(IllegalArgumentException.class, () -> Task.fromSerializedString(input), input);
        }
    }

    @Test
    void fromSerializedString_invalidDates_throwsException() {
        for (String input : new String[] {"D | 0 | book | bad", "D | 0 | book | 2025-02-29",
                "E | 0 | meeting | bad | 2026-09-02",
                "E | 0 | meeting | 2026-09-01 | bad"}) {
            assertThrows(DateTimeParseException.class, () -> Task.fromSerializedString(input), input);
        }
    }

    @Test
    void occursOn_undatedTask_neverMatches() {
        Task task = new Todo("read");
        assertFalse(task.occursOn(LocalDate.of(2026, 9, 1)));
        task.setMarked(true);
        assertFalse(task.occursOn(LocalDate.of(2026, 9, 2)));
    }
}
