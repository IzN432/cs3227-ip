package ekko.parser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests absent-versus-empty values and replacement of named arguments. */
class ParsedArgumentsTest {
    @Test
    void getArgument_missingName_returnsEmptyButNotPresent() {
        ParsedArguments parsed = new ParsedArguments();
        assertEquals("", parsed.getArgument(ArgumentName.BY));
        assertFalse(parsed.containsArgument(ArgumentName.BY));
    }

    @Test
    void setArgument_emptyThenReplacement_tracksPresenceAndLastValue() {
        ParsedArguments parsed = new ParsedArguments();
        parsed.setDescription("read book");
        parsed.setArgument(ArgumentName.BY, "");
        assertTrue(parsed.containsArgument(ArgumentName.BY));
        assertEquals("", parsed.getArgument(ArgumentName.BY));
        parsed.setArgument(ArgumentName.BY, "tomorrow");
        parsed.setArgument(ArgumentName.TO, "later");
        assertEquals("tomorrow", parsed.getArgument(ArgumentName.BY));
        assertEquals("later", parsed.getArgument(ArgumentName.TO));
        assertEquals("read book", parsed.getDescription());
    }
}
