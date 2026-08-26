import java.util.HashMap;
import java.util.Map;

/**
 * Stores a task description and the named values extracted from it.
 */
public class ParsedArguments {
    private String description;
    private final Map<String, String> arguments;

    public ParsedArguments() {
        arguments = new HashMap<>();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setArgument(String argument, String value) {
        arguments.put(argument, value);
    }

    public boolean containsArgument(String argument) {
        return arguments.containsKey(argument);
    }

    public String getDescription() {
        return description;
    }

    public String getArgument(String argument) {
        return arguments.getOrDefault(argument, "");
    }
}
