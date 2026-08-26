import java.util.Scanner;

/**
 * Starts the Ekko chatbot application.
 */
public class Ekko {

    private static final int MAX_TASKS = 100;

    public static void main(String[] args) {
        Ekko instance = new Ekko();
        instance.mainLoop();
    }

    private final Scanner scanner;
    private final String[] tasks;
    private int taskCount;

    public Ekko() {
        scanner = new Scanner(System.in);
        tasks = new String[MAX_TASKS];
        taskCount = 0;
    }

    /**
     * Runs the chatbot until the user enters the {@code bye} command.
     */
    private void mainLoop() {
        printSeparator();
        printBanner();
        sendMessage(String.format("Hello! I'm %s.\nWhat can I do for you?", getName()));
        printSeparator();

        String input = getInput();
        printSeparator();
        while (!input.equals("bye")) {
            if (input.equals("list")) {
                printTasks();
            } else {
                addTask(input);
            }

            input = getInput();
            printSeparator();
        }
        sendMessage("Bye. Hope to see you again soon!");
        printSeparator();
    }

    /**
     * Stores the user's input and confirms that it was added.
     * If the storage is full, displays a message without adding the input.
     *
     * @param task task description to store
     */
    private void addTask(String task) {
        if (taskCount >= tasks.length) {
            sendMessage("Sorry, I can't store any more tasks.");
            printSeparator();
            return;
        }
        tasks[taskCount] = task;
        taskCount++;
        sendMessage(String.format("added: %s", task));
        printSeparator();
    }

    /**
     * Displays all tasks in the order they were added.
     */
    private void printTasks() {
        if (taskCount == 0) {
            sendMessage("No tasks found!");
        } else {
            for (int i = 0; i < taskCount; i++) {
                System.out.printf("%d. %s\n", i + 1, tasks[i]);
            }
        }
        System.out.println();
        printSeparator();
    }

    public String getInput() {
        String input = scanner.nextLine();
        System.out.println();
        return input;
    }

    public void printBanner() {
        String banner = " _______  __  ___  __  ___   ______   \n"
                + "|   ____||  |/  / |  |/  /  /  __  \\  \n"
                + "|  |__   |  '  /  |  '  /  |  |  |  | \n"
                + "|   __|  |    <   |    <   |  |  |  | \n"
                + "|  |____ |  .  \\  |  .  \\  |  `--'  | \n"
                + "|_______||__|\\__\\ |__|\\__\\  \\______/  \n";
        System.out.println(banner);
    }

    public void sendMessage(String message) {
        System.out.println(message);
        System.out.println();
    }

    public void printSeparator() {
        System.out.println("─".repeat(80));
    }

    public String getName() {
        return "Ekko";
    }
}
