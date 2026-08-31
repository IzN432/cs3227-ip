package ekko.users;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the application's user collection and its lookup operations.
 *
 * <p>Users are keyed by username for O(1) lookup. Order is not guaranteed.
 */
public class UserStore {

    private final Map<String, User> users;

    /**
     * Creates a user store containing the supplied users.
     *
     * @param users initial users; must not contain duplicates or null entries.
     * @throws IllegalArgumentException if the collection contains a null or duplicate username.
     */
    public UserStore(List<User> users) {
        this.users = new HashMap<>();
        users.forEach(this::add);
    }

    /**
     * Adds a user to the store.
     *
     * @param user user to add; must not be null or share a username with an existing user.
     * @throws IllegalArgumentException if the user is null or the username is already registered.
     */
    public void add(User user) {
        if (user == null) {
            throw new IllegalArgumentException("A user is required.");
        }
        if (users.containsKey(user.getUsername())) {
            throw new IllegalArgumentException("Username '" + user.getUsername() + "' is already registered.");
        }
        users.put(user.getUsername(), user);
    }

    /**
     * Returns the user with the given username, or {@code null} if not found.
     *
     * @param username username to look up.
     * @return matching user, or {@code null}.
     */
    public User get(String username) {
        return users.get(username);
    }

    /**
     * Returns whether a username is already registered.
     *
     * @param username username to check.
     */
    public boolean contains(String username) {
        return users.containsKey(username);
    }

    /**
     * Authenticates a login attempt.
     *
     * @param username username supplied by the user.
     * @param hashedPassword hashed password supplied by the user.
     * @return the authenticated {@link User}, or {@code null} if the credentials are invalid.
     */
    public User authenticate(String username, String hashedPassword) {
        User user = users.get(username);
        if (user == null || !user.getHashedPassword().equals(hashedPassword)) {
            return null;
        }
        return user;
    }

    /**
     * Returns whether the store contains no users.
     */
    public boolean isEmpty() {
        return users.isEmpty();
    }

    /**
     * Returns the number of registered users.
     */
    public int size() {
        return users.size();
    }

    /**
     * Returns an immutable snapshot of all users for persistence.
     */
    public Collection<User> asCollection() {
        return Collections.unmodifiableCollection(users.values());
    }
}
