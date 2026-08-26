import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Starts the Ekko chatbot application.
 */
public class Ekko {

    public static void main(String[] args) {
        Ekko instance = new Ekko();
        instance.mainLoop();
    }

    private final Scanner scanner;
    private final List<Task> tasks;

    public Ekko() {
        scanner = new Scanner(System.in);
        tasks = new ArrayList<>();
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
        boolean shouldExit = false;
        while (!shouldExit) {
            try {
                shouldExit = handleInput(input);
            } catch (EkkoException e) {
                sendMessage(e.getMessage());
            }
            if (!shouldExit) {
                printSeparator();
                input = getInput().trim();
                printSeparator();
            }
        }
        sendMessage("Bye. Hope to see you again soon!");
        printSeparator();
    }

    /**
     * Identifies a command and passes its remaining text to the appropriate handler.
     *
     * @param input complete line entered by the user
     */
    private boolean handleInput(String input) throws EkkoException {
        if (input.isBlank()) {
            throw new EkkoException("Please enter a command.");
        }

        String[] parts = input.split("\\s+", 2);
        Command command = Command.from(parts[0]);
        String arguments = parts.length == 2 ? parts[1].trim() : "";

        switch (command) {
        case TODO -> addTodo(arguments);
        case DEADLINE -> addDeadline(arguments);
        case EVENT -> addEvent(arguments);
        case LIST -> printTasks();
        case MARK -> markTask(arguments);
        case UNMARK -> unmarkTask(arguments);
        case DELETE -> deleteTask(arguments);
        case BYE -> {
            // The main loop displays the farewell message after this method returns.
        }
        }
        return command == Command.BYE;
    }

    private void addTodo(String arguments) throws EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("The description of a todo cannot be empty.");
        }
        addTask(new Todo(arguments));
    }

    private void addDeadline(String arguments) throws EkkoException {
        ParsedArguments parsed = ArgumentParser.parse(arguments, Set.of(ArgumentName.BY));
        String by = parsed.getArgument(ArgumentName.BY);

        if (parsed.getDescription().isBlank()) {
            throw new EkkoException("The description of a deadline cannot be empty.");
        } else if (!parsed.containsArgument(ArgumentName.BY) || by.isBlank()) {
            throw new EkkoException("A deadline must have a non-empty /by argument.");
        } else {
            addTask(new Deadline(parsed.getDescription(), by));
        }
    }

    private void addEvent(String arguments) throws EkkoException {
        ParsedArguments parsed = ArgumentParser.parse(
                arguments,
                Set.of(ArgumentName.FROM, ArgumentName.TO)
        );
        String from = parsed.getArgument(ArgumentName.FROM);
        String to = parsed.getArgument(ArgumentName.TO);

        if (parsed.getDescription().isBlank()) {
            throw new EkkoException("The description of an event cannot be empty.");
        } else if (!parsed.containsArgument(ArgumentName.FROM) || from.isBlank()) {
            throw new EkkoException("An event must have a non-empty /from argument.");
        } else if (!parsed.containsArgument(ArgumentName.TO) || to.isBlank()) {
            throw new EkkoException("An event must have a non-empty /to argument.");
        } else {
            addTask(new Event(parsed.getDescription(), from, to));
        }
    }

    /**
     * Stores the user's input and confirms that it was added.
     *
     * @param task task to store
     */
    private void addTask(Task task) {
        tasks.add(task);
        sendMessage(String.format(
                "Got it. I've added this task:\n  %s\nNow you have %d tasks in the list.",
                task,
                tasks.size()
        ));
    }

    /**
     * Displays all tasks in the order they were added.
     */
    private void printTasks() {
        if (tasks.isEmpty()) {
            sendMessage("No tasks found!");
        } else {
            System.out.println("Here are the tasks in your list:");
            for (int i = 0; i < tasks.size(); i++) {
                System.out.printf("%d.%s\n", i + 1, tasks.get(i));
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
                if (taskIndex < 0 || taskIndex >= tasks.size()) {
                    throw new EkkoException(
                            "Please input a valid task number. You can send list to see how many tasks you have."
                    );
                } else if (tasks.get(taskIndex).isMarked()) {
                    sendMessage("This task has already been marked as done:\n  " + tasks.get(taskIndex));
                } else {
                    tasks.get(taskIndex).setMarked(true);
                    sendMessage("Nice! I've marked this task as done:\n  " + tasks.get(taskIndex));
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
                if (taskIndex < 0 || taskIndex >= tasks.size()) {
                    throw new EkkoException(
                            "Please input a valid task number. You can send list to see how many tasks you have."
                    );
                } else if (!tasks.get(taskIndex).isMarked()) {
                    sendMessage("This task has already been unmarked:\n  " + tasks.get(taskIndex));
                } else {
                    tasks.get(taskIndex).setMarked(false);
                    sendMessage("Okay, I've unmarked this task as not done yet:\n  " + tasks.get(taskIndex));
                }
            } catch (NumberFormatException e) {
                throw new EkkoException("Please provide a valid task number.");
            }
        }
    }

    /**
     * Deletes the task identified by a delete command.
     * Displays an error message when the command does not contain a valid task number.
     *
     * @param arguments text following the {@code delete} command
     */
    private void deleteTask(String arguments) throws EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("Please provide a task number.");
        } else {
            try {
                int taskIndex = Integer.parseInt(arguments) - 1;
                if (taskIndex < 0 || taskIndex >= tasks.size()) {
                    throw new EkkoException(
                            "Please input a valid task number. You can send list to see how many tasks you have."
                    );
                } else {
                    Task deletedTask = tasks.remove(taskIndex);
                    sendMessage(String.format(
                            "Noted. I've removed this task:\n  %s\nNow you have %d tasks in the list.",
                            deletedTask,
                            tasks.size()
                    ));
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
