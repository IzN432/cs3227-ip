import java.util.Scanner;
import java.util.Set;

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

        String input = getInput().trim();
        printSeparator();
        while (!input.equals("bye")) {
            try {
                handleInput(input);
            } catch (EkkoException e) {
                sendMessage(e.getMessage());
            }
            printSeparator();
            input = getInput().trim();
            printSeparator();
        }
        sendMessage("Bye. Hope to see you again soon!");
        printSeparator();
    }

    /**
     * Identifies a command and passes its remaining text to the appropriate handler.
     *
     * @param input complete line entered by the user
     */
    private void handleInput(String input) throws EkkoException {
        if (input.isBlank()) {
            throw new EkkoException("Please enter a command.");
        }

        String[] parts = input.split("\\s+", 2);
        String commandWord = parts[0];
        String arguments = parts.length == 2 ? parts[1].trim() : "";

        switch (commandWord) {
        case "todo" -> addTodo(arguments);
        case "deadline" -> addDeadline(arguments);
        case "event" -> addEvent(arguments);
        case "list" -> printTasks();
        case "mark" -> markTask(arguments);
        case "unmark" -> unmarkTask(arguments);
        default -> throw new EkkoException("I don't recognise that command.");
        }
    }

    private void addTodo(String arguments) throws EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("The description of a todo cannot be empty.");
        }
        addTask(new Todo(arguments));
    }

    private void addDeadline(String arguments) throws EkkoException {
        ParsedArguments parsed = ArgumentParser.parse(arguments, Set.of("by"));
        String by = parsed.getArgument("by");

        if (parsed.getDescription().isBlank()) {
            throw new EkkoException("The description of a deadline cannot be empty.");
        } else if (!parsed.containsArgument("by") || by.isBlank()) {
            throw new EkkoException("A deadline must have a non-empty /by argument.");
        } else {
            addTask(new Deadline(parsed.getDescription(), by));
        }
    }

    private void addEvent(String arguments) throws EkkoException {
        ParsedArguments parsed = ArgumentParser.parse(arguments, Set.of("from", "to"));
        String from = parsed.getArgument("from");
        String to = parsed.getArgument("to");

        if (parsed.getDescription().isBlank()) {
            throw new EkkoException("The description of an event cannot be empty.");
        } else if (!parsed.containsArgument("from") || from.isBlank()) {
            throw new EkkoException("An event must have a non-empty /from argument.");
        } else if (!parsed.containsArgument("to") || to.isBlank()) {
            throw new EkkoException("An event must have a non-empty /to argument.");
        } else {
            addTask(new Event(parsed.getDescription(), from, to));
        }
    }

    /**
     * Stores the user's input and confirms that it was added.
     * If the storage is full, displays a message without adding the input.
     *
     * @param task task to store
     */
    private void addTask(Task task) throws EkkoException {
        if (taskCount >= tasks.length) {
            throw new EkkoException("Sorry, I can't store any more tasks.");
        }
        tasks[taskCount] = task;
        taskCount++;
        sendMessage(String.format(
                "Got it. I've added this task:\n  %s\nNow you have %d tasks in the list.",
                task,
                taskCount
        ));
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
    }

    /**
     * Marks the task identified by a mark command as complete.
     * Displays an error message when the command does not contain a valid task number.
     *
     * @param arguments text following the {@code mark} command
     */
    private void markTask(String arguments) throws EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("Please provide a task number.");
        } else {
            try {
                int taskIndex = Integer.parseInt(arguments) - 1;
                if (taskIndex < 0 || taskIndex >= taskCount) {
                    throw new EkkoException(
                            "Please input a valid task number. You can send list to see how many tasks you have."
                    );
                } else if (tasks[taskIndex].isMarked()) {
                    sendMessage("This task has already been marked as done:\n  " + tasks[taskIndex]);
                } else {
                    tasks[taskIndex].setMarked(true);
                    sendMessage("Nice! I've marked this task as done:\n  " + tasks[taskIndex]);
                }
            } catch (NumberFormatException e) {
                throw new EkkoException("Please provide a valid task number.");
            }
        }
    }

    /**
     * Marks the task identified by an unmark command as incomplete.
     * Displays an error message when the command does not contain a valid task number.
     *
     * @param arguments text following the {@code unmark} command
     */
    private void unmarkTask(String arguments) throws EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("Please provide a task number.");
        } else {
            try {
                int taskIndex = Integer.parseInt(arguments) - 1;
                if (taskIndex < 0 || taskIndex >= taskCount) {
                    throw new EkkoException(
                            "Please input a valid task number. You can send list to see how many tasks you have."
                    );
                } else if (!tasks[taskIndex].isMarked()) {
                    sendMessage("This task has already been unmarked:\n  " + tasks[taskIndex]);
                } else {
                    tasks[taskIndex].setMarked(false);
                    sendMessage("Okay, I've unmarked this task as not done yet:\n  " + tasks[taskIndex]);
                }
            } catch (NumberFormatException e) {
                throw new EkkoException("Please provide a valid task number.");
            }
        }
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
