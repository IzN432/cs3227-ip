package ekko.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import ekko.task.Task;

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
        showMessage(String.format("%s online. What's on your agenda?", name));
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
     * Displays numbered tasks with the supplied heading.
     *
     * @param heading text shown before the tasks.
     * @param tasks tasks to display.
     */
    public void showTasks(String heading, List<Task> tasks) {
        showTasks(heading, tasks, tasks);
    }

    /**
     * Displays matching tasks using their current numbers and order in the full task list.
     *
     * @param heading text shown before the tasks.
     * @param matchingTasks task instances selected from the full list.
     * @param allTasks full task list whose positions define the displayed numbers.
     */
    public void showTasks(String heading, List<Task> matchingTasks, List<Task> allTasks) {
        StringBuilder message = new StringBuilder(heading);
        for (int i = 0; i < allTasks.size(); i++) {
            Task task = allTasks.get(i);
            if (matchingTasks.contains(task)) {
                message.append('\n').append(i + 1).append('.').append(task);
            }
        }
        showMessage(message.toString());
    }

    /**
     * Displays the separator between command interactions.
     */
    public void showSeparator() {
        output.println("-".repeat(80));
    }
}
