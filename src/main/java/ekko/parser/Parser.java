package ekko.parser;

import ekko.EkkoException;

/**
 * Interprets a complete line of user input as a command and its arguments.
 */
public final class Parser {
    private Parser() {
        // This class contains only static parsing behaviour.
    }

    /**
     * Splits a line into its command word and remaining argument text.
     *
     * @param input complete line entered by the user
     * @return parsed command and arguments
     * @throws EkkoException if the input is blank or names an unknown command
     */
    public static ParsedCommand parse(String input) throws EkkoException {
        if (input.isBlank()) {
            throw new EkkoException("Please enter a command.");
        }

        String[] parts = input.split("\\s+", 2);
        Command command = Command.from(parts[0]);
        String arguments = parts.length == 2 ? parts[1].trim() : "";
        return new ParsedCommand(command, arguments);
    }

    /**
     * Holds the two meaningful parts of a parsed input line.
     *
     * @param command recognised command
     * @param arguments text following the command word
     */
    public record ParsedCommand(Command command, String arguments) {
    }
}
