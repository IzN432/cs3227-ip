package ekko.parser;

import java.util.Locale;

import ekko.EkkoException;

/**
 * Defines supported commands and interface-independent reference text.
 */
public enum Command {
    TODO("<description>", "Add a task without a date."),
    DEADLINE("<description> /by <date/time>", "Add a task with a due date."),
    EVENT("<description> /from <date/time> /to <date/time>", "Schedule a task with a start and end."),
    AGENDA("<date>", "Show tasks scheduled for a date."),
    LIST("", "Show all tasks and their task numbers."),
    FIND("<keyword>", "Search task descriptions."),
    MARK("<task number>", "Mark a task as done."),
    UNMARK("<task number>", "Mark a task as not done."),
    DELETE("<task number>", "Remove a task from your list."),
    BYE("", "End the session.");

    /** Human-readable argument syntax; empty for commands without arguments. */
    private final String usage;
    private final String description;

    Command(String usage, String description) {
        this.usage = usage;
        this.description = description;
    }

    /**
     * Returns the lowercase command word shared by parsing and help displays.
     *
     * @return command word independent of the system locale.
     */
    public String getWord() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String getUsage() {
        return usage;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Converts a command word into its enum value.
     *
     * @param commandWord command word entered by the user.
     * @return the matching command.
     * @throws EkkoException if the command word is not supported.
     */
    public static Command from(String commandWord) throws EkkoException {
        for (Command command : values()) {
            if (command.getWord().equals(commandWord)) {
                return command;
            }
        }
        throw new EkkoException("Unknown command. A command reference has been provided. Use it.");
    }
}
