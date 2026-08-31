package ekko.task;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests inclusive event intervals and exact display/storage output. */
class EventTest {
    private final LocalDate start = LocalDate.of(2026, 9, 1);

    @Test
    void occursOn_multiDayInterval_includesBothEndsAndInterior() {
        Event event = new Event("trip", start.atTime(18, 0), start.plusDays(2).atStartOfDay());
        assertFalse(event.occursOn(start.minusDays(1)));
        assertTrue(event.occursOn(start));
        assertTrue(event.occursOn(start.plusDays(1)));
        assertTrue(event.occursOn(start.plusDays(2)));
        assertFalse(event.occursOn(start.plusDays(3)));
        event.setMarked(true);
        assertTrue(event.occursOn(start));
    }

    @Test
    void occursOn_equalEndpoints_matchesOnlyThatDate() {
        Event event = new Event("meeting", start.atTime(12, 0), start.atTime(12, 0));
        assertTrue(event.occursOn(start));
        assertFalse(event.occursOn(start.minusDays(1)));
        assertFalse(event.occursOn(start.plusDays(1)));
    }

    @Test
    void occursOn_yearBoundary_handlesCalendarRollover() {
        LocalDate endOfYear = LocalDate.of(2026, 12, 31);
        Event event = new Event("trip", endOfYear.atTime(23, 0), endOfYear.plusDays(1).atTime(1, 0));
        assertTrue(event.occursOn(endOfYear));
        assertTrue(event.occursOn(endOfYear.plusDays(1)));
    }

    @Test
    void toString_andSerialization_bothStates_preserveEndpoints() {
        Event event = new Event("trip", start.atStartOfDay(), start.plusDays(1).atTime(18, 0));
        assertEquals("[E][ ] trip (from: Sep 01 2026 to: Sep 02 2026, 6:00 PM)", event.toString());
        assertEquals("E | 0 | trip | 2026-09-01T00:00 | 2026-09-02T18:00", event.toSerializedString());
        event.setMarked(true);
        assertEquals("[E][X] trip (from: Sep 01 2026 to: Sep 02 2026, 6:00 PM)", event.toString());
        assertEquals("E | 1 | trip | 2026-09-01T00:00 | 2026-09-02T18:00", event.toSerializedString());
    }
}
