package ekko.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import ekko.EkkoException;

/**
 * Tests complete-command splitting, preserving argument content and errors.
 */
class ParserTest {
    @Test
    void parse_eachBareCommand_returnsEmptyArguments() throws EkkoException {
        for (Command command : Command.values()) {
            Parser.ParsedCommand parsed = Parser.parse(command.name().toLowerCase(Locale.ROOT));
            assertEquals(command, parsed.command());
            assertEquals("", parsed.arguments());
        }
    }

    @Test
    void parse_whitespaceSeparator_trimsEdgesButPreservesInternalText() throws EkkoException {
        Parser.ParsedCommand parsed = Parser.parse("  event \t meeting  notes /from 1800 /to 1900  ");
        assertEquals(Command.EVENT, parsed.command());
        assertEquals("meeting  notes /from 1800 /to 1900", parsed.arguments());
    }

    @Test
    void parse_blankInput_reportsMissingCommand() {
        for (String input : new String[] {"", " ", "\t\n"}) {
            assertEquals("Please enter a command.",
                    assertThrows(EkkoException.class, () -> Parser.parse(input)).getMessage());
        }
    }

    @Test
    void parse_unknownOrUppercaseCommand_translatesLookupFailureToEkkoException() {
        for (String input : new String[] {"remember task", "TODO task", "todotask"}) {
            assertEquals("Unknown command. A command reference has been provided. Use it.",
                    assertThrows(EkkoException.class, () -> Parser.parse(input)).getMessage());
        }
    }

    @Test
    void parse_trailingWhitespace_returnsEmptyArguments() throws EkkoException {
        assertEquals("", Parser.parse("bye \t ").arguments());
    }
}
