package ekko.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import ekko.AppException;

/**
 * Tests complete-command splitting, preserving argument content and errors.
 */
class ParserTest {
    @Test
    void parse_eachBareCommand_returnsEmptyArguments() throws AppException {
        for (Command command : Command.values()) {
            Parser.ParsedCommand parsed = Parser.parse(command.name().toLowerCase(Locale.ROOT));
            assertEquals(command, parsed.command());
            assertEquals("", parsed.arguments());
        }
    }

    @Test
    void parse_whitespaceSeparator_trimsEdgesButPreservesInternalText() throws AppException {
        Parser.ParsedCommand parsed = Parser.parse("  bin \t laptop  deal /desc great cond /price 200  ");
        assertEquals(Command.BIN, parsed.command());
        assertEquals("laptop  deal /desc great cond /price 200", parsed.arguments());
    }

    @Test
    void parse_blankInput_reportsMissingCommand() {
        for (String input : new String[] {"", " ", "\t\n"}) {
            assertEquals("Please enter a command.",
                    assertThrows(AppException.class, () -> Parser.parse(input)).getMessage());
        }
    }

    @Test
    void parse_unknownOrUppercaseCommand_translatesLookupFailureToAppException() {
        for (String input : new String[] {"remember task", "TODO task", "todotask"}) {
            assertEquals("Unknown command. A command reference has been provided. Use it.",
                    assertThrows(AppException.class, () -> Parser.parse(input)).getMessage());
        }
    }

    @Test
    void parse_trailingWhitespace_returnsEmptyArguments() throws AppException {
        assertEquals("", Parser.parse("bye \t ").arguments());
    }
}
