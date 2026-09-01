package ekko.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests per-user conversation ordering and validation.
 */
class ConversationStoreTest {

    @Test
    void append_multipleUsers_keepsIndependentOrderedHistories() {
        ConversationStore store = new ConversationStore();

        store.append("alice", "You", "list");
        store.append("bob", "Ekko", "Auction ended");
        store.append("alice", "Ekko", "No active listings.");

        assertEquals(2, store.getMessages("alice").size());
        assertEquals("list", store.getMessages("alice").get(0).text());
        assertEquals("No active listings.", store.getMessages("alice").get(1).text());
        assertEquals(1, store.getMessages("bob").size());
    }

    @Test
    void getMessages_unknownUser_returnsEmptyList() {
        assertTrue(new ConversationStore().getMessages("nobody").isEmpty());
    }

    @Test
    void append_blankUserUuid_throwsException() {
        ConversationStore store = new ConversationStore();

        assertThrows(IllegalArgumentException.class, () -> store.append(" ", "Ekko", "Hello"));
    }
}
