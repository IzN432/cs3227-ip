package ekko.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests absent-versus-empty values and replacement of named arguments.
 */
class ParsedArgumentsTest {
    @Test
    void getArgument_missingName_returnsEmptyButNotPresent() {
        ParsedArguments parsed = new ParsedArguments();
        assertEquals("", parsed.getArgument(ArgumentName.PRICE));
        assertFalse(parsed.containsArgument(ArgumentName.PRICE));
    }

    @Test
    void setArgument_emptyThenReplacement_tracksPresenceAndLastValue() {
        ParsedArguments parsed = new ParsedArguments();
        parsed.setDescription("vintage lamp");
        parsed.setArgument(ArgumentName.PRICE, "");
        assertTrue(parsed.containsArgument(ArgumentName.PRICE));
        assertEquals("", parsed.getArgument(ArgumentName.PRICE));
        parsed.setArgument(ArgumentName.PRICE, "50");
        parsed.setArgument(ArgumentName.END, "2026-09-10 1800");
        assertEquals("50", parsed.getArgument(ArgumentName.PRICE));
        assertEquals("2026-09-10 1800", parsed.getArgument(ArgumentName.END));
        assertEquals("vintage lamp", parsed.getDescription());
    }
}
