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
    private final Task[] tasks;
    private int taskCount;

    public Ekko() {
        scanner = new Scanner(System.in);
        tasks = new Task[MAX_TASKS];
        taskCount = 0;
    }

    /**
     * Runs the chatbot until the user enters the {@code bye} command.
     */
    public void mainLoop() {
        printSeparator();
        printBanner();
        sendMessage(String.format("Hello! I'm %s.\nWhat can I do for you?", getName()));
        printSeparator();

        String input = getInput();
        String[] command = input.split("\\s+");
        printSeparator();
        while (!command[0].equals("bye")) {
            if (command[0].equals("list")) {
                printTasks();
            } else if (command[0].equals("mark")) {
                markTask(command);
            } else if (command[0].equals("unmark")) {
                unmarkTask(command);
            } else {
                addTask(input);
            }

            input = getInput();
            command = input.split("\\s+");
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
        tasks[taskCount] = new Task(task);
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
                System.out.printf("%d.%s\n", i + 1, tasks[i]);
            }
            System.out.println();
        }
        printSeparator();
    }

    /**
     * Marks the task identified by a mark command as complete.
     * Displays an error message when the command does not contain a valid task number.
     *
     * @param command parsed command words, such as {@code ["mark", "1"]}
     */
    private void markTask(String[] command) {
        if (command.length < 2) {
            sendMessage("Please provide a task number.");
        } else {
            try {
                int taskIndex = Integer.parseInt(command[1]) - 1;
                if (taskIndex < 0 || taskIndex >= taskCount) {
                    sendMessage("Please input a valid task number. You can send list to see how many tasks you have.");
                } else if (tasks[taskIndex].isMarked()) {
                    sendMessage("This task has already been marked as done:\n  " + tasks[taskIndex]);
                } else {
                    tasks[taskIndex].setMarked(true);
                    sendMessage("Nice! I've marked this task as done:\n  " + tasks[taskIndex]);
                }
            } catch (NumberFormatException e) {
                sendMessage("Please provide a valid task number.");
            }
        }
        printSeparator();
    }

    /**
     * Marks the task identified by an unmark command as incomplete.
     * Displays an error message when the command does not contain a valid task number.
     *
     * @param command parsed command words, such as {@code ["unmark", "1"]}
     */
    private void unmarkTask(String[] command) {
        if (command.length < 2) {
            sendMessage("Please provide a task number.");
        } else {
            try {
                int taskIndex = Integer.parseInt(command[1]) - 1;
                if (taskIndex < 0 || taskIndex >= taskCount) {
                    sendMessage("Please input a valid task number. You can send list to see how many tasks you have.");
                } else if (!tasks[taskIndex].isMarked()) {
                    sendMessage("This task has already been unmarked:\n  " + tasks[taskIndex]);
                } else {
                    tasks[taskIndex].setMarked(false);
                    sendMessage("Okay, I've unmarked this task as not done yet:\n  " + tasks[taskIndex]);
                }
            } catch (NumberFormatException e) {
                sendMessage("Please provide a valid task number.");
            }
        }
        printSeparator();
    }

    private String getInput() {
        String input = scanner.nextLine();
        System.out.println();
        return input;
    }

    private void printBanner() {
        String banner = " _______  __  ___  __  ___   ______   \n"
                + "|   ____||  |/  / |  |/  /  /  __  \\  \n"
                + "|  |__   |  '  /  |  '  /  |  |  |  | \n"
                + "|   __|  |    <   |    <   |  |  |  | \n"
                + "|  |____ |  .  \\  |  .  \\  |  `--'  | \n"
                + "|_______||__|\\__\\ |__|\\__\\  \\______/  \n";
        System.out.println(banner);
    }

    private void sendMessage(String message) {
        System.out.println(message);
        System.out.println();
    }

    private void printSeparator() {
        System.out.println("─".repeat(80));
    }

    private String getName() {
        return "Ekko";
    }
}
