package ekko.parser;

import ekko.EkkoException;

import java.util.Locale;

/**
 * Represents a command supported by Ekko.
 */
public enum Command {
    TODO,
    DEADLINE,
    EVENT,
    AGENDA,
    LIST,
    MARK,
    UNMARK,
    DELETE,
    BYE;

    /**
     * Converts a command word into its enum value.
     *
     * @param commandWord command word entered by the user
     * @return the matching command
     * @throws EkkoException if the command word is not supported
     */
    public static Command from(String commandWord) throws EkkoException {
        for (Command command : values()) {
            if (command.name().toLowerCase(Locale.ROOT).equals(commandWord)) {
                return command;
            }
        }
        throw new EkkoException("I don't recognise that command.");
    }
}
