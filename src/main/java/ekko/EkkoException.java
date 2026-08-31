package ekko;

/**
 * Represents an error caused by a command that Ekko cannot process.
 */
public class EkkoException extends Exception {

    /**
     * Creates an exception with the message that should be shown to the user.
     *
     * @param message explanation of the command error
     */
    public EkkoException(String message) {
        super(message);
    }
}
