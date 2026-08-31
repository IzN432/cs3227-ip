package ekko;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

import ekko.datetime.DateTimeParser;
import ekko.parser.ArgumentName;
import ekko.parser.ArgumentParser;
import ekko.parser.Command;
import ekko.parser.ParsedArguments;
import ekko.parser.Parser;
import ekko.storage.Storage;
import ekko.task.Deadline;
import ekko.task.Event;
import ekko.task.Task;
import ekko.task.TaskList;
import ekko.task.Todo;
import ekko.ui.Ui;

/**
 * Starts the Ekko chatbot application.
 */
public class Ekko {

    private final Ui ui;
    private final Storage storage;
    private final TaskList tasks;
    /** Indicates whether loading or recovery permits the command loop to start. */
    private final boolean canStart;

    /**
     * Creates the application with console UI and default file storage.
     */
    public Ekko() throws IOException {
        this(new Ui(), new Storage());
    }

    /**
     * Loads the application using supplied UI and storage dependencies.
     *
     * @param ui user interaction endpoint.
     * @param storage task persistence endpoint.
     * @throws IOException if loading or recovery fails.
     */
    public Ekko(Ui ui, Storage storage) throws IOException {
        this.ui = ui;
        this.storage = storage;
        List<Task> loadedTasks;
        boolean canProceedWithStartup = true;
        try {
            loadedTasks = storage.loadTasks();
        } catch (IllegalArgumentException | DateTimeException e) {
            loadedTasks = List.of();
            canProceedWithStartup = handleInvalidDataFile();
        }
        tasks = new TaskList(loadedTasks);
        canStart = canProceedWithStartup;
    }

    /**
     * Starts the console application if its saved data can be loaded or recovered.
     *
     * @param args command-line arguments, which are not used.
     * @throws IOException if loading, recovery, or saving fails.
     */
    public static void main(String[] args) throws IOException {
        Ekko instance = new Ekko();
        if (instance.canStart) {
            instance.mainLoop();
        }
    }

    public boolean canStart() {
        return canStart;
    }

    /**
     * Processes one GUI command without waiting for console input.
     * Validation failures are displayed through the supplied UI.
     *
     * @param input complete command entered by the user.
     * @return whether the caller should stop accepting commands.
     * @throws IOException if saving fails; callers should stop the session to avoid unsaved edits.
     */
    public boolean processCommand(String input) throws IOException {
        if (!canStart) {
            return true;
        }
        try {
            boolean shouldExit = handleInput(input.trim());
            if (shouldExit) {
                ui.showMessage("Bye. Hope to see you again soon!");
            }
            return shouldExit;
        } catch (EkkoException | IllegalArgumentException e) {
            ui.showMessage(e.getMessage());
            return false;
        }
    }

    /**
     * Lets the user decide whether a malformed saved-data file should be deleted.
     * Deleting the file allows Ekko to start with an empty task list; keeping it
     * prevents startup so the invalid data is not silently ignored.
     *
     * @return {@code true} if the invalid file was deleted and startup can proceed.
     */
    private boolean handleInvalidDataFile() throws IOException {
        ui.showMessage("The stored task data is invalid. Delete the data file? (y/n)");
        String response = ui.readOptionalResponse();
        if (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes")) {
            storage.deleteDataFile();
            ui.showMessage("The invalid data file was deleted. Ekko will start with an empty task list.");
            return true;
        } else {
            ui.showMessage("The data file was kept. Ekko will now exit.");
            return false;
        }
    }

    /**
     * Runs the chatbot until the user enters the {@code bye} command.
     */
    public void mainLoop() throws IOException {
        ui.showWelcome(getName());

        String input = ui.readCommand();
        ui.showSeparator();
        boolean shouldExit = false;
        while (!shouldExit) {
            try {
                shouldExit = handleInput(input);
            } catch (EkkoException | IllegalArgumentException e) {
                ui.showMessage(e.getMessage());
            }
            if (!shouldExit) {
                ui.showSeparator();
                input = ui.readCommand();
                ui.showSeparator();
            }
        }
        ui.showMessage("Bye. Hope to see you again soon!");
        ui.showSeparator();
    }

    /**
     * Identifies a command and passes its remaining text to the appropriate handler.
     *
     * @param input complete line entered by the user.
     */
    private boolean handleInput(String input) throws IOException, EkkoException {
        Parser.ParsedCommand parsedCommand = Parser.parse(input);
        Command command = parsedCommand.command();
        String arguments = parsedCommand.arguments();

        switch (command) {
            case TODO -> addTodo(arguments);
            case DEADLINE -> addDeadline(arguments);
            case EVENT -> addEvent(arguments);
            case AGENDA -> printAgenda(arguments);
            case LIST -> printTasks();
            case FIND -> printMatchingTasks(arguments);
            case MARK -> markTask(arguments);
            case UNMARK -> unmarkTask(arguments);
            case DELETE -> deleteTask(arguments);
            case BYE -> {
                // The main loop displays the farewell message after this method returns.
            }
            default -> throw new AssertionError("Unhandled command: " + command);
        }
        return command == Command.BYE;
    }

    /**
     * Validates the todo description and adds the resulting task.
     */
    private void addTodo(String arguments) throws IOException, EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("The description of a todo cannot be empty.");
        }
        addTask(new Todo(arguments));
    }

    /**
     * Validates the description and required deadline before adding a task.
     */
    private void addDeadline(String arguments) throws IOException, EkkoException {
        ParsedArguments parsed = ArgumentParser.parse(arguments, Set.of(ArgumentName.BY));
        String by = parsed.getArgument(ArgumentName.BY);

        if (parsed.getDescription().isBlank()) {
            throw new EkkoException("The description of a deadline cannot be empty.");
        } else if (!parsed.containsArgument(ArgumentName.BY) || by.isBlank()) {
            throw new EkkoException("A deadline must have a non-empty /by argument.");
        } else {
            addTask(new Deadline(parsed.getDescription(), parseDateTime(by)));
        }
    }

    /**
     * Validates both event endpoints and rejects an end before the start.
     */
    private void addEvent(String arguments) throws IOException, EkkoException {
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
            LocalDateTime start = parseDateTime(from);
            LocalDateTime end = parseDateTime(to);
            if (end.isBefore(start)) {
                throw new EkkoException("An event's /to date/time cannot be before its /from date/time.");
            }
            addTask(new Event(parsed.getDescription(), start, end));
        }
    }

    /**
     * Displays deadlines due and events occurring on a specified date.
     */
    private void printAgenda(String arguments) throws EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("Please provide a date for the agenda.");
        }

        LocalDate date;
        try {
            date = DateTimeParser.parseDate(arguments);
        } catch (DateTimeParseException e) {
            throw new EkkoException("Please use a valid date such as 2019-10-15 or 2/12/2019.");
        }

        List<Task> matchingTasks = tasks.findOn(date);
        String formattedDate = DateTimeParser.format(date);
        if (matchingTasks.isEmpty()) {
            ui.showMessage("No deadlines or events found on " + formattedDate + ".");
            return;
        }

        ui.showTasks("Here are the deadlines and events on " + formattedDate + ":", matchingTasks);
    }

    /**
     * Parses a command date/time and translates parsing failures into a helpful UI error.
     */
    private LocalDateTime parseDateTime(String value) throws EkkoException {
        try {
            return DateTimeParser.parse(value);
        } catch (DateTimeParseException e) {
            throw new EkkoException(
                    "Please use a valid date/time such as 2019-10-15 or 2/12/2019 1800."
            );
        }
    }

    /**
     * Stores the user's input and confirms that it was added.
     *
     * @param task task to store.
     */
    private void addTask(Task task) throws IOException {
        tasks.add(task);
        saveTasks();
        ui.showMessage(String.format(
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
            ui.showMessage("No tasks found!");
        } else {
            ui.showTasks("Here are the tasks in your list:", tasks.asList());
        }
    }

    /**
     * Marks the task identified by a mark command as complete.
     * Displays an error message when the command does not contain a valid task number.
     *
     * @param arguments text following the {@code mark} command.
     */
    private void markTask(String arguments) throws IOException, EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("Please provide a task number.");
        } else {
            try {
                TaskList.TaskUpdate update = tasks.mark(Integer.parseInt(arguments));
                if (!update.hasChanged()) {
                    ui.showMessage("This task has already been marked as done:\n  " + update.task());
                } else {
                    saveTasks();
                    ui.showMessage("Nice! I've marked this task as done:\n  " + update.task());
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
     * @param arguments text following the {@code unmark} command.
     */
    private void unmarkTask(String arguments) throws IOException, EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("Please provide a task number.");
        } else {
            try {
                TaskList.TaskUpdate update = tasks.unmark(Integer.parseInt(arguments));
                if (!update.hasChanged()) {
                    ui.showMessage("This task has already been unmarked:\n  " + update.task());
                } else {
                    saveTasks();
                    ui.showMessage("Okay, I've unmarked this task as not done yet:\n  " + update.task());
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
     * @param arguments text following the {@code delete} command.
     */
    private void deleteTask(String arguments) throws IOException, EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("Please provide a task number.");
        } else {
            try {
                Task deletedTask = tasks.delete(Integer.parseInt(arguments));
                saveTasks();
                ui.showMessage(String.format(
                        "Noted. I've removed this task:\n  %s\nNow you have %d tasks in the list.",
                        deletedTask,
                        tasks.size()
                ));
            } catch (NumberFormatException e) {
                throw new EkkoException("Please provide a valid task number.");
            }
        }
    }

    /**
     * Displays tasks whose descriptions contain the supplied case-sensitive search text.
     */
    private void printMatchingTasks(String arguments) throws EkkoException {
        List<Task> matchingTasks = tasks.find(arguments);
        if (matchingTasks.isEmpty()) {
            ui.showMessage("No matching tasks found!");
        } else {
            ui.showTasks("Here are the matching tasks in your list:", matchingTasks);
        }
    }

    private void saveTasks() throws IOException {
        storage.saveTasks(tasks.asList());
    }

    private String getName() {
        return "Ekko";
    }
}
