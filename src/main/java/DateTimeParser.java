import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

/**
 * Converts supported user-facing date/time formats to and from Java date/time objects.
 */
public final class DateTimeParser {
    private static final List<DateTimeFormatter> INPUT_FORMATS = List.of(
            DateTimeFormatter.ofPattern("uuuu-MM-dd HHmm").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu HHmm").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu-MM-dd['T']HH:mm").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT)
    );
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("MMM dd uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("MMM dd uuuu, h:mm a", Locale.ENGLISH);

    private DateTimeParser() {
    }

    /**
     * Parses an ISO or day-first date, optionally followed by a 24-hour time.
     * A date without a time is represented as midnight.
     *
     * @param value date/time text supplied by the user or storage layer
     * @return parsed date and time
     * @throws DateTimeParseException if the value has no supported valid format
     */
    public static LocalDateTime parse(String value) {
        for (DateTimeFormatter formatter : INPUT_FORMATS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return java.time.LocalDate.parse(value, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }
        throw new DateTimeParseException("Unsupported date/time", value, 0);
    }

    /**
     * Parses a calendar date without a time component.
     *
     * @param value date text in ISO or day-first format
     * @return parsed date
     * @throws DateTimeParseException if the value is not a supported valid date
     */
    public static LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }
        throw new DateTimeParseException("Unsupported date", value, 0);
    }

    /**
     * Formats a calendar date for display to users.
     */
    public static String format(LocalDate date) {
        return date.format(DISPLAY_DATE);
    }

    /**
     * Formats a date for users, including the time only when it is not midnight.
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)
                ? dateTime.format(DISPLAY_DATE)
                : dateTime.format(DISPLAY_DATE_TIME);
    }
}
