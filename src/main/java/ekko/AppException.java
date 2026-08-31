package ekko;

/**
 * Signals an invalid user command or input error in the marketplace application.
 *
 * <p>This exception is reserved for user-facing command errors such as unknown
 * commands, missing arguments, and invalid values. File-system failures use
 * {@link java.io.IOException} and malformed persisted data uses
 * {@link IllegalArgumentException}.
 */
public class AppException extends Exception {

    /**
     * Creates an exception with the given user-facing message.
     *
     * @param message message to display to the user.
     */
    public AppException(String message) {
        super(message);
    }
}
