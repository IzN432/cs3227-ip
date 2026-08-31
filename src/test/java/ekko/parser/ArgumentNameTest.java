package ekko.parser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests slash-argument name recognition independently of command parsing. */
class ArgumentNameTest {
    @Test
    void fromText_supportedNames_returnsEnum() {
        assertEquals(ArgumentName.BY, ArgumentName.fromText("by"));
        assertEquals(ArgumentName.FROM, ArgumentName.fromText("from"));
        assertEquals(ArgumentName.TO, ArgumentName.fromText("to"));
    }

    @Test
    void fromText_unknownOrNonExactName_throwsException() {
        for (String input : new String[] {"", "/by", "BY", "from ", "about"}) {
            assertThrows(IllegalArgumentException.class, () -> ArgumentName.fromText(input));
        }
    }
}
