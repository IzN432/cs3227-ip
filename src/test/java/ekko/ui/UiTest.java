package ekko.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import ekko.task.Todo;

/**
 * Tests raw console output, including chrome ignored by normalized UI tests.
 */
class UiTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    /**
     * Supplies input without replacing the JVM's global streams.
     */
    private Ui ui(String input) {
        return new Ui(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    private String outputText() {
        return output.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    @Test
    void readCommand_trimsEdgesAndPreservesInternalSpaces() {
        Ui ui = ui(" \t todo  read book \t \n\n");
        assertEquals("todo  read book", ui.readCommand());
        assertEquals("", ui.readCommand());
        assertEquals("\n\n", outputText());
    }

    @Test
    void readCommand_exhaustedInput_throwsWithoutOutput() {
        assertThrows(NoSuchElementException.class, () -> ui("").readCommand());
        assertEquals("", outputText());
    }

    @Test
    void readOptionalResponse_trimsResponseAndHandlesEofWithoutOutput() {
        Ui ui = ui(" YES \n \n");
        assertEquals("YES", ui.readOptionalResponse());
        assertEquals("", ui.readOptionalResponse());
        assertEquals("", ui.readOptionalResponse());
        assertEquals("", outputText());
    }

    @Test
    void showMessage_multilineAndEmpty_addsBlankLine() {
        Ui ui = ui("");
        ui.showMessage("first\nsecond");
        ui.showMessage("");
        assertEquals("first\nsecond\n\n\n\n", outputText());
    }

    @Test
    void showTasks_emptyList_printsHeadingAndBlankLine() {
        ui("").showTasks("Tasks:", List.of());
        assertEquals("Tasks:\n\n", outputText());
    }

    @Test
    void showTasks_multipleTasks_numbersInOrderWithoutMutatingState() {
        Todo first = new Todo("first");
        Todo second = new Todo("second");
        second.setMarked(true);
        ui("").showTasks("Tasks:", List.of(first, second));
        assertEquals("Tasks:\n1.[T][ ] first\n2.[T][X] second\n\n", outputText());
        assertFalse(first.isMarked());
        assertTrue(second.isMarked());
    }

    @Test
    void showTasks_filteredList_preservesFullListNumbersIncludingGaps() {
        Todo first = new Todo("first");
        Todo second = new Todo("second");
        Todo third = new Todo("third");
        Todo fourth = new Todo("fourth");
        fourth.setMarked(true);

        ui("").showTasks("Matches:", List.of(second, fourth), List.of(first, second, third, fourth));

        assertEquals("Matches:\n2.[T][ ] second\n4.[T][X] fourth\n\n", outputText());
    }

    @Test
    void showSeparator_usesExactly80AsciiHyphens() {
        ui("").showSeparator();
        assertEquals("-".repeat(80) + "\n", outputText());
    }

    @Test
    void showWelcome_customName_printsBannerAndGreetingBetweenSeparators() {
        ui("").showWelcome("Test Ekko");
        String banner = " _______  __  ___  __  ___   ______   \n"
                + "|   ____||  |/  / |  |/  /  /  __  \\  \n"
                + "|  |__   |  '  /  |  '  /  |  |  |  | \n"
                + "|   __|  |    <   |    <   |  |  |  | \n"
                + "|  |____ |  .  \\  |  .  \\  |  `--'  | \n"
                + "|_______||__|\\__\\ |__|\\__\\  \\______/  \n";
        assertEquals("-".repeat(80) + "\n" + banner + "\n"
                + "Test Ekko online. What's on your agenda?\n\n"
                + "-".repeat(80) + "\n", outputText());
    }
}
