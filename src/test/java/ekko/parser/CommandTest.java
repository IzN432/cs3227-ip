package ekko.parser;

import ekko.EkkoException;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests exact command-word recognition and invalid-input rejection. */
class CommandTest {
    @Test
    void from_allCommands_returnsMatchingEnum() throws EkkoException {
        for (Command command : Command.values()) {
            assertEquals(command, Command.from(command.name().toLowerCase(Locale.ROOT)));
        }
    }

    @Test
    void from_unknownOrNonExactWord_throwsHelpfulException() {
        for (String input : new String[] {"", " ", "TODO", "Todo", "todo ", "todo task", "remember"}) {
            assertEquals("I don't recognise that command.",
                    assertThrows(EkkoException.class, () -> Command.from(input)).getMessage());
        }
    }
}
