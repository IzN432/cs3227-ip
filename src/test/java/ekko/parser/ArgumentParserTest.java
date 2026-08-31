package ekko.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests argument boundaries, ordering, empty values, and preservation of free text.
 */
class ArgumentParserTest {
    @Test
    void parse_noAllowedArguments_keepsAllText() {
        ParsedArguments parsed = ArgumentParser.parse("  read /by tomorrow  ", Set.of());
        assertEquals("read /by tomorrow", parsed.getDescription());
        assertFalse(parsed.containsArgument(ArgumentName.BY));
    }

    @Test
    void parse_blankInput_returnsEmptyDescriptionAndNoArguments() {
        for (String input : new String[] {"", " \t "}) {
            ParsedArguments parsed = ArgumentParser.parse(input, Set.of(ArgumentName.BY));
            assertEquals("", parsed.getDescription());
            assertFalse(parsed.containsArgument(ArgumentName.BY));
        }
    }

    @Test
    void parse_noRecognizedArguments_preservesDescription() {
        ParsedArguments parsed = ArgumentParser.parse(" read /about Java ", Set.of(ArgumentName.BY));
        assertEquals("read /about Java", parsed.getDescription());
        assertFalse(parsed.containsArgument(ArgumentName.BY));
    }

    @Test
    void parse_singleArgument_trimsValueAndKeepsInternalSpaces() {
        ParsedArguments parsed = ArgumentParser.parse(" read  book \t/by tomorrow  evening ",
                Set.of(ArgumentName.BY));
        assertEquals("read  book", parsed.getDescription());
        assertEquals("tomorrow  evening", parsed.getArgument(ArgumentName.BY));
    }

    @Test
    void parse_multipleArguments_acceptsEitherOrder() {
        for (String input : new String[] {
                "meeting /from Monday morning /to Tuesday evening",
                "meeting /to Tuesday evening /from Monday morning"
        }) {
            ParsedArguments parsed = ArgumentParser.parse(input, Set.of(ArgumentName.FROM, ArgumentName.TO));
            assertEquals("meeting", parsed.getDescription());
            assertEquals("Monday morning", parsed.getArgument(ArgumentName.FROM));
            assertEquals("Tuesday evening", parsed.getArgument(ArgumentName.TO));
        }
    }

    @Test
    void parse_argumentAtStart_allowsEmptyDescription() {
        ParsedArguments parsed = ArgumentParser.parse("/by tomorrow", Set.of(ArgumentName.BY));
        assertEquals("", parsed.getDescription());
        assertEquals("tomorrow", parsed.getArgument(ArgumentName.BY));
    }

    @Test
    void parse_emptyValues_remainPresent() {
        ParsedArguments parsed = ArgumentParser.parse("meeting /from /to", Set.of(ArgumentName.FROM, ArgumentName.TO));
        assertTrue(parsed.containsArgument(ArgumentName.FROM));
        assertTrue(parsed.containsArgument(ArgumentName.TO));
        assertEquals("", parsed.getArgument(ArgumentName.FROM));
        assertEquals("", parsed.getArgument(ArgumentName.TO));
    }

    @Test
    void parse_duplicateArgument_lastValueWins() {
        ParsedArguments parsed = ArgumentParser.parse("book /by Monday /by Tuesday", Set.of(ArgumentName.BY));
        assertEquals("Tuesday", parsed.getArgument(ArgumentName.BY));
        assertEquals("book", parsed.getDescription());
    }

    @Test
    void parse_duplicateEndingEmpty_doesNotKeepEarlierValue() {
        ParsedArguments parsed = ArgumentParser.parse("book /by Monday /by", Set.of(ArgumentName.BY));
        assertTrue(parsed.containsArgument(ArgumentName.BY));
        assertEquals("", parsed.getArgument(ArgumentName.BY));
    }

    @Test
    void parse_unallowedArgumentInValue_preservesIt() {
        ParsedArguments parsed = ArgumentParser.parse("book /by Monday /to Tuesday", Set.of(ArgumentName.BY));
        assertEquals("Monday /to Tuesday", parsed.getArgument(ArgumentName.BY));
        assertFalse(parsed.containsArgument(ArgumentName.TO));
    }

    @Test
    void parse_embeddedOrLongerSlashWord_doesNotMatch() {
        for (String input : new String[] {"read/by Monday", "read /bypass road", "read /by2 days",
                "read /by_name", "read /BY Monday", "https://site/by"}) {
            ParsedArguments parsed = ArgumentParser.parse(input, Set.of(ArgumentName.BY));
            assertEquals(input, parsed.getDescription());
            assertFalse(parsed.containsArgument(ArgumentName.BY), input);
        }
    }

    @Test
    void parse_allAllowedArguments_extractsEachWithoutChangingAllowedSet() {
        Set<ArgumentName> allowed = Set.of(ArgumentName.BY, ArgumentName.FROM, ArgumentName.TO);
        ParsedArguments parsed = ArgumentParser.parse("task /by one /from two /to three", allowed);
        assertEquals("one", parsed.getArgument(ArgumentName.BY));
        assertEquals("two", parsed.getArgument(ArgumentName.FROM));
        assertEquals("three", parsed.getArgument(ArgumentName.TO));
        assertEquals(3, allowed.size());
    }
}
