package ekko.parser;

/**
 * Defines the slash-prefixed argument names recognised by marketplace commands.
 */
public enum ArgumentName {
    DESC("desc"),
    PRICE("price"),
    END("end"),
    TYPE("type"),
    ACTIVE("active"),
    SOLD("sold"),
    MINE("mine"),
    LOW("low"),
    HIGH("high");

    private final String text;

    ArgumentName(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    /**
     * Converts a slash-argument word to its enum value.
     *
     * @param text argument word without the leading slash.
     * @return the matching argument name.
     * @throws IllegalArgumentException if the word is not a recognised argument.
     */
    public static ArgumentName fromText(String text) {
        for (ArgumentName name : values()) {
            if (name.text.equals(text)) {
                return name;
            }
        }
        throw new IllegalArgumentException("Unknown argument: /" + text);
    }
}
