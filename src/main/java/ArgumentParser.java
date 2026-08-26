import java.util.Comparator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses explicitly allowed slash-prefixed arguments from command text.
 */
public final class ArgumentParser {

    private ArgumentParser() {
        // This class contains only static utility methods.
    }

    /**
     * Separates a task description from the named arguments accepted by a command.
     * Slash-prefixed words that are not in {@code availableArguments} remain part
     * of the surrounding text.
     *
     * @param argumentString all text following the command word
     * @param availableArguments argument names recognised by the command
     * @return the parsed description and argument values
     */
    public static ParsedArguments parse(String argumentString, Set<ArgumentName> availableArguments) {
        ParsedArguments parsedArguments = new ParsedArguments();
        if (availableArguments.isEmpty()) {
            parsedArguments.setDescription(argumentString.trim());
            return parsedArguments;
        }

        String allowedNames = availableArguments.stream()
                .sorted(Comparator.comparingInt(
                        (ArgumentName argumentName) -> argumentName.getText().length()
                ).reversed())
                .map(ArgumentName::getText)
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));

        Pattern argumentPattern = Pattern.compile(
                "(?<!\\S)/(" + allowedNames + ")\\b"
        );

        Matcher matcher = argumentPattern.matcher(argumentString);

        String description = argumentString.trim();
        ArgumentName previousName = null;
        int previousValueStartIndex = -1;

        while (matcher.find()) {
            ArgumentName argument = ArgumentName.fromText(matcher.group(1));
            if (previousName == null) {
                description = argumentString.substring(0, matcher.start()).trim();
            } else {
                String previousValue = argumentString.substring(
                        previousValueStartIndex,
                        matcher.start()
                ).trim();
                parsedArguments.setArgument(previousName, previousValue);
            }
            previousName = argument;
            previousValueStartIndex = matcher.end();
        }
        parsedArguments.setDescription(description);

        if (previousName != null) {
            String previousValue = argumentString.substring(
                    previousValueStartIndex
            ).trim();
            parsedArguments.setArgument(previousName, previousValue);
        }

        return parsedArguments;
    }
}
