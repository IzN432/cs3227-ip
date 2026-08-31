package ekko.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Handles all console input and output for Ekko.
 */
public class Ui {
    private final Scanner scanner;
    private final PrintStream output;

    /**
     * Creates a UI connected to the standard input stream.
     */
    public Ui() {
        this(System.in, System.out);
    }

    /**
     * Creates a UI with caller-owned streams, allowing in-memory streams in tests.
     *
     * @param input stream containing user commands.
     * @param output destination for displayed messages.
     */
    public Ui(InputStream input, PrintStream output) {
        scanner = new Scanner(input);
        this.output = output;
    }

    /**
     * Reads the next command entered by the user.
     *
     * @return the command without surrounding whitespace.
     */
    public String readCommand() {
        String input = scanner.nextLine();
        output.println();
        return input.trim();
    }

    /**
     * Returns whether another console command is available, allowing a clean exit at end of input.
     */
    public boolean hasNextCommand() {
        return scanner.hasNextLine();
    }

    /**
     * Reads a response when input may have ended, such as during startup recovery.
     *
     * @return the response, or an empty string when no line is available.
     */
    public String readOptionalResponse() {
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }

    /**
     * Displays the startup banner and greeting.
     *
     * @param name application name shown in the greeting.
     */
    public void showWelcome(String name) {
        showSeparator();
        String banner = " _______  __  ___  __  ___   ______   \n"
                + "|   ____||  |/  / |  |/  /  /  __  \\  \n"
                + "|  |__   |  '  /  |  '  /  |  |  |  | \n"
                + "|   __|  |    <   |    <   |  |  |  | \n"
                + "|  |____ |  .  \\  |  .  \\  |  `--'  | \n"
                + "|_______||__|\\__\\ |__|\\__\\  \\______/  \n";
        output.println(banner);
        showMessage(String.format("%s online. Welcome to the marketplace.", name));
        showSeparator();
    }

    /**
     * Displays a message followed by a blank line.
     *
     * @param message message to display.
     */
    public void showMessage(String message) {
        output.println(message);
        output.println();
    }

    /**
     * Displays an error, preserving the console message format.
     *
     * @param message explanation of the failure.
     */
    public void showError(String message) {
        showMessage(message);
    }

    /**
     * Displays the separator between command interactions.
     */
    public void showSeparator() {
        output.println("-".repeat(80));
    }
}
