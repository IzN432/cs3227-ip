package ekko.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * Tests exact command-word recognition, invalid-input rejection, and user-facing command metadata.
 */
class CommandTest {
    @Test
    void from_allCommands_returnsMatchingEnum() {
        for (Command command : Command.values()) {
            assertEquals(command, Command.from(command.name().toLowerCase(Locale.ROOT)));
        }
    }

    @Test
    void from_unknownOrNonExactWord_throwsIllegalArgumentException() {
        for (String input : new String[] {null, "", " ", "TODO", "Todo", "todo ", "todo task", "remember"}) {
            assertEquals("Unknown command: " + input,
                    assertThrows(IllegalArgumentException.class, () -> Command.from(input)).getMessage());
        }
    }

    @Test
    void getUsage_findCommand_includesOptionalPriceBounds() {
        assertEquals("<keyword> [/low <min>] [/high <max>]", Command.FIND.getUsage());
    }
}
