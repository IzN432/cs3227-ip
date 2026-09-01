package ekko.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests UserStore lookup, authentication, and duplicate rejection.
 */
class UserStoreTest {

    private User alice() {
        return new User("alice", "alicehash");
    }

    private User bob() {
        return new User("bob", "bobhash");
    }

    @Test
    void constructor_emptyList_createsEmptyStore() {
        UserStore store = new UserStore(List.of());
        assertTrue(store.isEmpty());
        assertEquals(0, store.size());
    }

    @Test
    void constructor_duplicateUsername_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserStore(List.of(alice(), new User("alice", "other"))));
    }

    @Test
    void add_nullUser_throwsException() {
        UserStore store = new UserStore(List.of());
        assertThrows(IllegalArgumentException.class, () -> store.add(null));
    }

    @Test
    void add_duplicateUsername_throwsException() {
        UserStore store = new UserStore(List.of(alice()));
        assertThrows(IllegalArgumentException.class, () -> store.add(new User("alice", "other")));
    }

    @Test
    void get_existingUsername_returnsUser() {
        UserStore store = new UserStore(List.of(alice(), bob()));
        assertEquals("alice", store.get("alice").getUsername());
    }

    @Test
    void get_unknownUsername_returnsNull() {
        UserStore store = new UserStore(List.of(alice()));
        assertNull(store.get("nobody"));
    }

    @Test
    void contains_existingAndMissing_returnsCorrectly() {
        UserStore store = new UserStore(List.of(alice()));
        assertTrue(store.contains("alice"));
        assertFalse(store.contains("bob"));
    }

    @Test
    void authenticate_correctCredentials_returnsUser() {
        UserStore store = new UserStore(List.of(alice()));
        assertEquals("alice", store.authenticate("alice", "alicehash").getUsername());
    }

    @Test
    void authenticate_wrongPassword_returnsNull() {
        UserStore store = new UserStore(List.of(alice()));
        assertNull(store.authenticate("alice", "wronghash"));
    }

    @Test
    void authenticate_unknownUser_returnsNull() {
        UserStore store = new UserStore(List.of(alice()));
        assertNull(store.authenticate("nobody", "alicehash"));
    }

    @Test
    void size_afterAdds_reflectsCount() {
        UserStore store = new UserStore(List.of());
        assertEquals(0, store.size());
        store.add(alice());
        assertEquals(1, store.size());
        store.add(bob());
        assertEquals(2, store.size());
    }
}
