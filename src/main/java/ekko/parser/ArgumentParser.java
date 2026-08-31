package ekko.parser;

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
     * @param argumentString all text following the command word.
     * @param availableArguments argument names recognized by the command.
     * @return the parsed description and argument values.
     */
    public static ParsedArguments parse(String argumentString, Set<ArgumentName> availableArguments) {
        ParsedArguments parsedArguments = new ParsedArguments();
        parsedArguments.setDescription(argumentString.trim());
        if (availableArguments.isEmpty()) {
            return parsedArguments;
        }

        Matcher matcher = createArgumentPattern(availableArguments).matcher(argumentString);
        if (!matcher.find()) {
            return parsedArguments;
        }
        parsedArguments.setDescription(argumentString.substring(0, matcher.start()).trim());

        boolean hasNextArgument;
        do {
            ArgumentName argument = ArgumentName.fromText(matcher.group(1));
            // The regex is built only from this command's allowed names.
            assert availableArguments.contains(argument) : "Matched argument must be allowed by the command";
            int valueStartIndex = matcher.end();
            hasNextArgument = matcher.find();
            int valueEndIndex = hasNextArgument ? matcher.start() : argumentString.length();
            // Adjacent arguments and an argument at end-of-input may have empty values.
            assert valueStartIndex <= valueEndIndex : "Argument value boundaries must not overlap";
            String value = argumentString.substring(valueStartIndex, valueEndIndex).trim();
            parsedArguments.setArgument(argument, value);
        } while (hasNextArgument);

        return parsedArguments;
    }

    /**
     * Builds a pattern matching only allowed names at whitespace and word boundaries.
     * Longer names are tried first, and names are quoted as literal regex text.
     */
    private static Pattern createArgumentPattern(Set<ArgumentName> availableArguments) {
        String allowedNames = availableArguments.stream()
                .sorted(Comparator.comparingInt(
                        (ArgumentName argumentName) -> argumentName.getText().length()
                ).reversed())
                .map(ArgumentName::getText)
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));

        return Pattern.compile(
                "(?<!\\S)/(" + allowedNames + ")\\b"
        );
    }
}
