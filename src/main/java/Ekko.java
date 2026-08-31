import java.io.IOException;
import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/**
 * Starts the Ekko chatbot application.
 */
public class Ekko {

    public static void main(String[] args) throws IOException {
        Ekko instance = new Ekko();
        if (instance.canStart) {
            instance.mainLoop();
        }
    }

    private final Ui ui;
    private final TaskList tasks;
    private final boolean canStart;

    public Ekko() throws IOException {
        ui = new Ui();
        List<Task> loadedTasks;
        boolean startupCanProceed = true;
        try {
            loadedTasks = Storage.loadTasks();
        } catch (IllegalArgumentException | DateTimeException e) {
            loadedTasks = List.of();
            startupCanProceed = handleInvalidDataFile();
        }
        tasks = new TaskList(loadedTasks);
        canStart = startupCanProceed;
    }

    /**
     * Lets the user decide whether a malformed saved-data file should be deleted.
     * Deleting the file allows Ekko to start with an empty task list; keeping it
     * prevents startup so the invalid data is not silently ignored.
     *
     * @return {@code true} if the invalid file was deleted and startup can proceed
     */
    private boolean handleInvalidDataFile() throws IOException {
        ui.showMessage("The stored task data is invalid. Delete the data file? (y/n)");
        String response = ui.readOptionalResponse();
        if (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes")) {
            Storage.deleteDataFile();
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
            } catch (EkkoException e) {
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
     * @param input complete line entered by the user
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
        case MARK -> markTask(arguments);
        case UNMARK -> unmarkTask(arguments);
        case DELETE -> deleteTask(arguments);
        case BYE -> {
            // The main loop displays the farewell message after this method returns.
        }
        }
        return command == Command.BYE;
    }

    private void addTodo(String arguments) throws IOException, EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("The description of a todo cannot be empty.");
        }
        addTask(new Todo(arguments));
    }

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
     * @param task task to store
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
     * @param arguments text following the {@code mark} command
     */
    private void markTask(String arguments) throws IOException, EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("Please provide a task number.");
        } else {
            try {
                TaskList.TaskUpdate update = tasks.mark(Integer.parseInt(arguments));
                if (!update.changed()) {
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
     * @param arguments text following the {@code unmark} command
     */
    private void unmarkTask(String arguments) throws IOException, EkkoException {
        if (arguments.isBlank()) {
            throw new EkkoException("Please provide a task number.");
        } else {
            try {
                TaskList.TaskUpdate update = tasks.unmark(Integer.parseInt(arguments));
                if (!update.changed()) {
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
     * @param arguments text following the {@code delete} command
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

    private void saveTasks() throws IOException {
        Storage.saveTasks(tasks.asList());
    }

    private String getName() {
        return "Ekko";
    }
}
