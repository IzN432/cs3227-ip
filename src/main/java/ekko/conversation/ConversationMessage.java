package ekko.conversation;

/**
 * Represents one persisted message in a user's conversation with Ekko.
 *
 * @param speaker displayed author, such as {@code You}, {@code Ekko}, or {@code Error}.
 * @param text displayed message body.
 */
public record ConversationMessage(String speaker, String text) {

    /**
     * Validates the persisted message fields.
     */
    public ConversationMessage {
        if (speaker == null || speaker.isBlank()) {
            throw new IllegalArgumentException("Message speaker cannot be blank.");
        }
        if (text == null) {
            throw new IllegalArgumentException("Message text cannot be null.");
        }
    }
}
