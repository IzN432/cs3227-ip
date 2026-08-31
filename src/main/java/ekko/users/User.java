package ekko.users;

/**
 * Represents a registered user of the marketplace.
 *
 * <p>All users start as buyers with a zero balance. Seller status is granted
 * on request via the {@code becomeseller} command. Usernames are immutable
 * after registration; balance and seller status may change during a session.
 */
public class User {

    private final String username;
    private final String hashedPassword;
    private boolean isSeller;
    private int balance;

    /**
     * Creates a new buyer account with a zero balance.
     *
     * @param username alphabetic username; must not be blank.
     * @param hashedPassword hashed password; must not be blank.
     * @throws IllegalArgumentException if either argument is null or blank.
     */
    public User(String username, String hashedPassword) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank.");
        }
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new IllegalArgumentException("Hashed password cannot be blank.");
        }
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.isSeller = false;
        this.balance = 0;
    }

    public String getUsername() {
        return username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public boolean isSeller() {
        return isSeller;
    }

    public void setSeller(boolean isSeller) {
        this.isSeller = isSeller;
    }

    public int getBalance() {
        return balance;
    }

    /**
     * Sets the balance directly, used when loading persisted state.
     *
     * @param balance new balance; must not be negative.
     * @throws IllegalArgumentException if the balance is negative.
     */
    public void setBalance(int balance) {
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
     */
    public void addBalance(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Top-up amount must be positive.");
        }
        this.balance += amount;
    }

    /**
     * Deducts the given amount from the balance if sufficient funds exist.
     *
     * @param amount amount to deduct; must be positive.
     * @return {@code true} if the deduction succeeded; {@code false} if the balance was insufficient.
     * @throws IllegalArgumentException if the amount is not positive.
     */
    public boolean deductBalance(int amount) {
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
