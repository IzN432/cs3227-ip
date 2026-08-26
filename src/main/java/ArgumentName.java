/**
 * Represents a slash-prefixed argument recognised by task commands.
 */
public enum ArgumentName {
    BY("by"),
    FROM("from"),
    TO("to");

    private final String text;

    ArgumentName(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    /**
     * Finds the argument name represented by parsed command text.
     *
     * @param text argument name without its leading slash
     * @return the matching argument name
     * @throws IllegalArgumentException if the text is not a recognised argument
     */
    public static ArgumentName fromText(String text) {
        for (ArgumentName argumentName : values()) {
            if (argumentName.text.equals(text)) {
                return argumentName;
            }
        }
        throw new IllegalArgumentException("Unknown argument name: " + text);
    }
}
