package ekko.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ekko.users.User;

/**
 * Tests UUID-named conversation persistence and corrupted-file recovery.
 */
class ConversationStorageTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void load_missingConversation_returnsEmptyHistory() throws IOException {
        ConversationStorage storage = new ConversationStorage(temporaryDirectory);

        assertTrue(storage.load(new User("alice", "secret")).isEmpty());
    }

    @Test
    void load_corruptedConversation_replacesFileWithRecoveryMessage() throws IOException {
        User user = new User("alice", "secret");
        Path conversationFile = temporaryDirectory.resolve(user.getUuid() + ".txt");
        Files.writeString(conversationFile, "not a valid conversation");
        ConversationStorage storage = new ConversationStorage(temporaryDirectory);

        List<ConversationMessage> recovered = storage.load(user);

        assertEquals(List.of(new ConversationMessage("Ekko", ConversationStorage.CORRUPTION_MESSAGE)),
                recovered);
        assertEquals(recovered, storage.load(user));
    }

    @Test
    void saveAndLoad_validConversation_roundTripsHistory() throws IOException {
        User user = new User("alice", "secret");
        ConversationStorage storage = new ConversationStorage(temporaryDirectory);
        List<ConversationMessage> messages = List.of(
                new ConversationMessage("You", "list"),
                new ConversationMessage("Ekko", "No active listings."));

        storage.save(user, messages);

        assertEquals(messages, storage.load(user));
    }
}
