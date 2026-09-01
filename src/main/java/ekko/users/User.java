package ekko.users;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Represents a registered user of the marketplace.
 *
 * <p>All users start as buyers with a zero balance. Seller status is granted
 * on request via the {@code becomeseller} command. Usernames are immutable
 * after registration; balance and seller status may change during a session.
 *
 * <p>Passwords are stored as a SHA-256 hex digest. The raw password is never
 * retained after construction.
 */
public class User {

    private final String uuid;
    private final String username;
    /** SHA-256 hex digest of the password supplied at construction. */
    private final String hashedPassword;
    private boolean isSeller;
    private long balance;

    /**
     * Creates a new buyer account with a zero balance.
     *
     * <p>The raw password is hashed with SHA-256 before being stored;
     * it is not retained anywhere in this object.
     *
     * @param username    alphabetic username; must not be blank.
     * @param rawPassword plaintext password to hash; must not be blank.
     * @throws IllegalArgumentException if either argument is null or blank.
     */
    public User(String username, String rawPassword) {
        this(UUID.randomUUID().toString(), requireUsername(username), hashRequiredPassword(rawPassword), false, 0);
    }

    /**
     * Creates a user with fully specified validated state.
     */
    private User(String uuid, String username, String hashedPassword, boolean isSeller, long balance) {
        this.uuid = uuid;
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.isSeller = isSeller;
        this.balance = balance;
    }

    /**
     * Reconstructs a user from trusted persisted fields.
     *
     * @param uuid stored user UUID.
     * @param username stored username.
     * @param hashedPassword stored SHA-256 password digest.
     * @param isSeller stored seller status.
     * @param balance stored coin balance.
     * @return reconstructed user.
     */
    public static User fromPersisted(String uuid, String username, String hashedPassword,
            boolean isSeller, long balance) {
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Stored user UUID is invalid.", e);
        }
        requireUsername(username);
        if (hashedPassword == null || !hashedPassword.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Stored password hash is invalid.");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative.");
        }
        return new User(uuid, username, hashedPassword, isSeller, balance);
    }

    /**
     * Validates and hashes a password supplied during registration.
     */
    private static String hashRequiredPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank.");
        }
        return hash(rawPassword);
    }

    /**
     * Returns a valid non-blank username.
     */
    private static String requireUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank.");
        }
        return username;
    }

    /**
     * Returns a SHA-256 hex digest of the given input.
     *
     * @param input text to hash; must not be null.
     * @return lowercase hex string of the digest.
     */
    static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by the Java platform specification and is always available.
            throw new AssertionError("SHA-256 not available", e);
        }
    }

    /**
     * Returns {@code true} if the given raw password matches the stored hash.
     *
     * @param rawPassword plaintext password to verify; must not be null.
     */
    public boolean checkPassword(String rawPassword) {
        return hashedPassword.equals(hash(rawPassword));
    }

    public String getUsername() {
        return username;
    }

    /**
     * Returns the immutable identifier used for user-owned persisted data.
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Returns the password digest for persistence without exposing the raw password.
     */
    public String getHashedPassword() {
        return hashedPassword;
    }

    public boolean isSeller() {
        return isSeller;
    }

    public void setSeller(boolean isSeller) {
        this.isSeller = isSeller;
    }

    public long getBalance() {
        return balance;
    }

    /**
     * Sets the balance directly, used when loading persisted state.
     *
     * @param balance new balance; must not be negative.
     * @throws IllegalArgumentException if the balance is negative.
     */
    public void setBalance(long balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative.");
        }
        this.balance = balance;
    }

    /**
     * Adds the given amount to the user's balance.
     *
     * @param amount amount to add; must be positive.
     * @throws IllegalArgumentException if the amount is not positive.
     * @throws ArithmeticException if the resulting balance would exceed {@link Long#MAX_VALUE}.
     */
    public void addBalance(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Top-up amount must be positive.");
        }
        this.balance = Math.addExact(this.balance, amount);
    }

    /**
     * Deducts the given amount from the balance if sufficient funds exist.
     *
     * @param amount amount to deduct; must be positive.
     * @return {@code true} if the deduction succeeded; {@code false} if the balance was insufficient.
     * @throws IllegalArgumentException if the amount is not positive.
     */
    public boolean deductBalance(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deduction amount must be positive.");
        }
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }
}
