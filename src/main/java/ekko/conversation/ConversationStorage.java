package ekko.conversation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import ekko.storage.PersistenceCodec;
import ekko.storage.Storage;
import ekko.users.User;

/**
 * Persists each user's conversation in a separate UUID-named file.
 */
public class ConversationStorage {

    public static final String CORRUPTION_MESSAGE
            = "Your conversation history was corrupted and has been cleared.";

    private final Path conversationDirectory;

    /**
     * Creates conversation storage rooted at the given directory.
     *
     * @param conversationDirectory directory containing UUID-named conversation files.
     */
    public ConversationStorage(Path conversationDirectory) {
        this.conversationDirectory = conversationDirectory;
    }

    /**
     * Loads a user's history, replacing a damaged file with an explanatory Ekko message.
     *
     * <p>A missing file represents a user with no existing conversation and is not treated as corruption.
     *
     * @param user owner of the conversation.
     * @return stored messages, an empty list for a missing file, or the recovery message for a damaged file.
     * @throws IOException if the recovery history cannot be written.
     */
    public List<ConversationMessage> load(User user) throws IOException {
        Storage<List<ConversationMessage>> storage = storageFor(user);
        try {
            return storage.load().orElseGet(List::of);
        } catch (IOException | IllegalArgumentException loadException) {
            List<ConversationMessage> recovered = List.of(
                    new ConversationMessage("Ekko", CORRUPTION_MESSAGE));
            try {
                storage.save(recovered);
            } catch (IOException saveException) {
                saveException.addSuppressed(loadException);
                throw saveException;
            }
            return recovered;
        }
    }

    /**
     * Replaces a user's persisted conversation with the supplied history.
     *
     * @param user owner of the conversation.
     * @param messages complete ordered conversation history.
     * @throws IOException if the history cannot be saved.
     */
    public void save(User user, List<ConversationMessage> messages) throws IOException {
        storageFor(user).save(messages);
    }

    /**
     * Creates the generic storage adapter for one UUID-named conversation file.
     */
    private Storage<List<ConversationMessage>> storageFor(User user) {
        Path file = conversationDirectory.resolve(user.getUuid() + ".txt");
        return new Storage<>(file, PersistenceCodec::serializeConversation,
                PersistenceCodec::deserializeConversation);
    }
}
