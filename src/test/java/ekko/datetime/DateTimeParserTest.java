package ekko.datetime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

/**
 * Tests date/time parsing, strict calendar validation, and user-facing display
 * rules without console or file dependencies.
 */
class DateTimeParserTest {
    @Test
    void parseDate_isoDate_returnsDate() {
        assertEquals(LocalDate.of(2019, 12, 2), DateTimeParser.parseDate("2019-12-02"));
    }

    @Test
    void parseDate_dayFirstDate_returnsDate() {
        assertEquals(LocalDate.of(2019, 12, 2), DateTimeParser.parseDate("2/12/2019"));
    }

    @Test
    void parseDate_paddedAndUnpaddedDayFirstDates_returnsSameDate() {
        LocalDate expected = LocalDate.of(2026, 9, 1);
        assertAll(
                () -> assertEquals(expected, DateTimeParser.parseDate("1/9/2026")),
                () -> assertEquals(expected, DateTimeParser.parseDate("01/09/2026"))
        );
    }

    @Test
    void parseDate_ambiguousDayFirstDate_usesDayThenMonth() {
        assertEquals(LocalDate.of(2019, 3, 2), DateTimeParser.parseDate("2/3/2019"));
    }

    @Test
    void parseDate_yearBoundaries_returnsDate() {
        assertAll(
                () -> assertEquals(LocalDate.of(2026, 1, 1), DateTimeParser.parseDate("2026-01-01")),
                () -> assertEquals(LocalDate.of(2026, 12, 31), DateTimeParser.parseDate("31/12/2026"))
        );
    }

    @Test
    void parseDate_validMonthEnds_returnsDate() {
        assertAll(
                () -> assertEquals(LocalDate.of(2025, 2, 28), DateTimeParser.parseDate("2025-02-28")),
                () -> assertEquals(LocalDate.of(2026, 4, 30), DateTimeParser.parseDate("30/4/2026")),
                () -> assertEquals(LocalDate.of(2026, 1, 31), DateTimeParser.parseDate("2026-01-31"))
        );
    }

    @Test
    void parseDate_leapDayInLeapYear_returnsDate() {
        LocalDate expected = LocalDate.of(2024, 2, 29);
        assertAll(
                () -> assertEquals(expected, DateTimeParser.parseDate("2024-02-29")),
                () -> assertEquals(expected, DateTimeParser.parseDate("29/2/2024"))
        );
    }

    @Test
    void parseDate_leapDayInCenturyDivisibleBy400_returnsDate() {
        assertEquals(LocalDate.of(2000, 2, 29), DateTimeParser.parseDate("2000-02-29"));
    }

    @Test
    void parseDate_leapDayInNonLeapYear_throwsException() {
        assertRejected("2025-02-29", "29/2/2025", "1900-02-29", "29/2/1900");
    }

    @Test
    void parseDate_dayBeyondMonthEnd_throwsException() {
        assertRejected("2024-02-30", "30/2/2024", "2026-04-31", "31/4/2026");
    }

    @Test
    void parseDate_dayOutsideRange_throwsException() {
        assertRejected("2026-01-00", "0/1/2026", "2026-01-32", "32/1/2026");
    }

    @Test
    void parseDate_monthOutsideRange_throwsException() {
        assertRejected("2026-00-01", "1/0/2026", "2026-13-01", "1/13/2026");
    }

    @Test
    void parseDate_emptyOrBlankInput_throwsException() {
        assertRejected("", " ", "\t\n");
    }

    @Test
    void parseDate_unsupportedFormat_throwsException() {
        assertRejected("tomorrow", "2026/09/01", "01-09-2026", "09/30/2026", "1/9/26");
    }

    @Test
    void parseDate_incompleteDate_throwsException() {
        assertRejected("2026", "2026-09", "1/9", "2026-09-");
    }

    @Test
    void parseDate_surroundingWhitespace_throwsException() {
        assertRejected(" 2026-09-01", "2026-09-01 ", "\t1/9/2026", "1/9/2026\n");
    }

    @Test
    void parseDate_timeComponent_throwsException() {
        assertRejected("2026-09-01 1800", "1/9/2026 1800", "2026-09-01T18:00:00");
    }

    @Test
    void parseDate_trailingText_throwsException() {
        assertRejected("2026-09-01extra", "1/9/2026 extra");
    }

    @Test
    void parse_isoDateWithCompactTime_returnsDateTime() {
        assertEquals(LocalDateTime.of(2019, 12, 2, 18, 0), DateTimeParser.parse("2019-12-02 1800"));
    }

    @Test
    void parse_dayFirstDateWithCompactTime_returnsDateTime() {
        assertEquals(LocalDateTime.of(2019, 12, 2, 18, 0), DateTimeParser.parse("2/12/2019 1800"));
    }

    @Test
    void parse_isoDateWithColonTime_returnsDateTime() {
        LocalDateTime expected = LocalDateTime.of(2019, 12, 2, 18, 5);
        assertAll(
                () -> assertEquals(expected, DateTimeParser.parse("2019-12-02T18:05")),
                // The existing formatter allows the T separator to be absent.
                () -> assertEquals(expected, DateTimeParser.parse("2019-12-0218:05"))
        );
    }

    @Test
    void parse_isoSecondsAndFraction_preservesPrecision() {
        assertAll(
                () -> assertEquals(LocalDateTime.of(2019, 12, 2, 18, 5, 30),
                        DateTimeParser.parse("2019-12-02T18:05:30")),
                () -> assertEquals(LocalDateTime.of(2019, 12, 2, 18, 5, 30, 123456789),
                        DateTimeParser.parse("2019-12-02T18:05:30.123456789"))
        );
    }

    @Test
    void parse_dateOnly_defaultsToMidnight() {
        LocalDateTime expected = LocalDateTime.of(2019, 12, 2, 0, 0);
        assertAll(
                () -> assertEquals(expected, DateTimeParser.parse("2019-12-02")),
                () -> assertEquals(expected, DateTimeParser.parse("2/12/2019"))
        );
    }

    @Test
    void parse_timeBoundaries_returnsDateTime() {
        assertAll(
                () -> assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0),
                        DateTimeParser.parse("2026-01-01 0000")),
                () -> assertEquals(LocalDateTime.of(2026, 12, 31, 23, 59),
                        DateTimeParser.parse("31/12/2026 2359"))
        );
    }

    @Test
    void parse_leapDay_returnsDateTime() {
        assertEquals(LocalDateTime.of(2024, 2, 29, 12, 0), DateTimeParser.parse("29/2/2024 1200"));
    }

    @Test
    void parse_invalidCalendarDate_throwsException() {
        assertDateTimeRejected("2025-02-29 1200", "31/4/2026 1200", "2026-13-01T12:00",
                "2026-01-00", "29/2/1900");
    }

    @Test
    void parse_invalidTime_throwsException() {
        assertDateTimeRejected("2026-09-01 2400", "1/9/2026 1260", "2026-09-01T24:00",
                "2026-09-01T12:60", "2026-09-01T12:00:60", "2026-09-01 -100");
    }

    @Test
    void parse_emptyOrUnsupportedInput_throwsException() {
        assertDateTimeRejected("", " ", "tomorrow", "2026/09/01", "2026-09",
                "2026-09-01 6pm", "1/9/2026 18:00");
    }

    @Test
    void parse_surroundingWhitespaceOrTrailingText_throwsException() {
        assertDateTimeRejected(" 2026-09-01 1800", "2026-09-01 1800 ", "1/9/2026 1800extra");
    }

    @Test
    void parse_timezoneSuffix_throwsException() {
        assertDateTimeRejected("2026-09-01T18:00Z", "2026-09-01T18:00+08:00");
    }

    @Test
    void parse_serializedLocalDateTime_roundTripsWithoutPrecisionLoss() {
        LocalDateTime[] values = {
            LocalDateTime.of(2026, 9, 1, 0, 0),
            LocalDateTime.of(2026, 9, 1, 18, 5),
            LocalDateTime.of(2026, 9, 1, 18, 5, 30, 123456789)
        };
        // Task persistence uses LocalDateTime.toString(), not the display format.
        assertAll(java.util.Arrays.stream(values).map(value -> () ->
                assertEquals(value, DateTimeParser.parse(value.toString()))));
    }

    @Test
    void formatDate_singleDigitDay_usesEnglishMonthAndPaddedDay() {
        assertEquals("Sep 01 2026", DateTimeParser.format(LocalDate.of(2026, 9, 1)));
    }

    @Test
    void formatDate_leapDayAndYearEnd_formatsCalendarDate() {
        assertAll(
                () -> assertEquals("Feb 29 2024", DateTimeParser.format(LocalDate.of(2024, 2, 29))),
                () -> assertEquals("Dec 31 2026", DateTimeParser.format(LocalDate.of(2026, 12, 31)))
        );
    }

    @Test
    void formatDateTime_midnight_omitsTime() {
        assertEquals("Sep 01 2026", DateTimeParser.format(LocalDateTime.of(2026, 9, 1, 0, 0)));
    }

    @Test
    void formatDateTime_morning_usesAmAndPaddedMinutes() {
        assertEquals("Sep 01 2026, 9:05 AM", DateTimeParser.format(LocalDateTime.of(2026, 9, 1, 9, 5)));
    }

    @Test
    void formatDateTime_noon_usesTwelvePm() {
        assertEquals("Sep 01 2026, 12:00 PM", DateTimeParser.format(LocalDateTime.of(2026, 9, 1, 12, 0)));
    }

    @Test
    void formatDateTime_afternoonAndLastMinute_usesTwelveHourClock() {
        assertAll(
                () -> assertEquals("Sep 01 2026, 1:00 PM",
                        DateTimeParser.format(LocalDateTime.of(2026, 9, 1, 13, 0))),
                () -> assertEquals("Sep 01 2026, 11:59 PM",
                        DateTimeParser.format(LocalDateTime.of(2026, 9, 1, 23, 59)))
        );
    }

    @Test
    void formatDateTime_justAfterMidnight_includesTime() {
        assertAll(
                () -> assertEquals("Sep 01 2026, 12:01 AM",
                        DateTimeParser.format(LocalDateTime.of(2026, 9, 1, 0, 1))),
                () -> assertEquals("Sep 01 2026, 12:00 AM",
                        DateTimeParser.format(LocalDateTime.of(2026, 9, 1, 0, 0, 0, 1)))
        );
    }

    @Test
    void formatDateTime_secondsAndFraction_omitsSubMinutePrecision() {
        assertEquals("Sep 01 2026, 6:05 PM",
                DateTimeParser.format(LocalDateTime.of(2026, 9, 1, 18, 5, 59, 999999999)));
    }

    /**
     * Checks that date/time parsing rejects each invalid input in a category.
     */
    private void assertDateTimeRejected(String... inputs) {
        assertAll(java.util.Arrays.stream(inputs).map(input -> () ->
                assertThrows(DateTimeParseException.class,
                        () -> DateTimeParser.parse(input), "Input: " + input)));
    }

    /**
     * Checks each invalid input and includes it in any failure message.
     * Grouped assertions report all failures within an input category.
     */
    private void assertRejected(String... inputs) {
        assertAll(java.util.Arrays.stream(inputs).map(input -> () ->
                assertThrows(DateTimeParseException.class,
                        () -> DateTimeParser.parseDate(input), "Input: " + input)));
    }
}
