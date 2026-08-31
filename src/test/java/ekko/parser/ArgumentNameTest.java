package ekko.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests slash-argument name recognition independently of command parsing.
 */
class ArgumentNameTest {
    @Test
    void fromText_supportedNames_returnsEnum() {
        assertEquals(ArgumentName.DESC, ArgumentName.fromText("desc"));
        assertEquals(ArgumentName.PRICE, ArgumentName.fromText("price"));
        assertEquals(ArgumentName.END, ArgumentName.fromText("end"));
        assertEquals(ArgumentName.LOW, ArgumentName.fromText("low"));
        assertEquals(ArgumentName.HIGH, ArgumentName.fromText("high"));
    }

    @Test
    void fromText_unknownOrNonExactName_throwsException() {
        for (String input : new String[] {"", "/desc", "DESC", "price ", "about"}) {
            assertThrows(IllegalArgumentException.class, () -> ArgumentName.fromText(input));
        }
    }
}
