package ekko.conversation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores each user's ordered conversation history.
 */
public class ConversationStore {

    private final Map<String, List<ConversationMessage>> conversations;

    /**
     * Creates an empty conversation store.
     */
    public ConversationStore() {
        conversations = new HashMap<>();
    }

    /**
     * Appends one message to a user's conversation.
     *
     * @param userUuid conversation owner's immutable UUID.
     * @param speaker displayed message author.
     * @param text displayed message body.
     */
    public void append(String userUuid, String speaker, String text) {
        if (userUuid == null || userUuid.isBlank()) {
            throw new IllegalArgumentException("Conversation user UUID cannot be blank.");
        }
        conversations.computeIfAbsent(userUuid, ignored -> new ArrayList<>())
                .add(new ConversationMessage(speaker, text));
    }

    /**
     * Returns an immutable snapshot of a user's messages in display order.
     *
     * @param userUuid conversation owner's immutable UUID.
     * @return stored messages, or an empty list if the user has no history.
     */
    public List<ConversationMessage> getMessages(String userUuid) {
        return List.copyOf(conversations.getOrDefault(userUuid, List.of()));
    }
}
