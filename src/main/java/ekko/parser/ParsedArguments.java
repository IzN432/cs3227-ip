package ekko.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores a task description and the named values extracted from it.
 */
public class ParsedArguments {
    private String description;
    private final Map<ArgumentName, String> arguments;

    /**
     * Creates an empty argument map with no description assigned yet.
     */
    public ParsedArguments() {
        arguments = new HashMap<>();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Stores a named argument, replacing any previously supplied value.
     */
    public void setArgument(ArgumentName argument, String value) {
        arguments.put(argument, value);
    }

    /**
     * Returns whether the argument was supplied, even if its value is empty.
     */
    public boolean containsArgument(ArgumentName argument) {
        return arguments.containsKey(argument);
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the argument's value, or an empty string when it was not supplied.
     */
    public String getArgument(ArgumentName argument) {
        return arguments.getOrDefault(argument, "");
    }
}
