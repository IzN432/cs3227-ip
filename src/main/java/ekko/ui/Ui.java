package ekko.ui;

import ekko.task.Task;

import java.util.List;
import java.util.Scanner;

/**
 * Handles all console input and output for Ekko.
 */
public class Ui {
    private final Scanner scanner;

    /**
     * Creates a UI connected to the standard input stream.
     */
    public Ui() {
        scanner = new Scanner(System.in);
    }

    /**
     * Reads the next command entered by the user.
     *
     * @return the command without surrounding whitespace
     */
    public String readCommand() {
        String input = scanner.nextLine();
        System.out.println();
        return input.trim();
    }

    /**
     * Reads a response when input may have ended, such as during startup recovery.
     *
     * @return the response, or an empty string when no line is available
     */
    public String readOptionalResponse() {
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }

    /**
     * Displays the startup banner and greeting.
     *
     * @param name application name shown in the greeting
     */
    public void showWelcome(String name) {
        showSeparator();
        String banner = " _______  __  ___  __  ___   ______   \n"
                + "|   ____||  |/  / |  |/  /  /  __  \\  \n"
                + "|  |__   |  '  /  |  '  /  |  |  |  | \n"
                + "|   __|  |    <   |    <   |  |  |  | \n"
                + "|  |____ |  .  \\  |  .  \\  |  `--'  | \n"
                + "|_______||__|\\__\\ |__|\\__\\  \\______/  \n";
        System.out.println(banner);
        showMessage(String.format("Hello! I'm %s.\nWhat can I do for you?", name));
        showSeparator();
    }

    /**
     * Displays a message followed by a blank line.
     *
     * @param message message to display
     */
    public void showMessage(String message) {
        System.out.println(message);
        System.out.println();
    }

    /**
     * Displays numbered tasks with the supplied heading.
     *
     * @param heading text shown before the tasks
     * @param tasks tasks to display
     */
    public void showTasks(String heading, List<Task> tasks) {
        System.out.println(heading);
        for (int i = 0; i < tasks.size(); i++) {
            System.out.printf("%d.%s%n", i + 1, tasks.get(i));
        }
        System.out.println();
    }

    /**
     * Displays the separator between command interactions.
     */
    public void showSeparator() {
        System.out.println("─".repeat(80));
    }
}
