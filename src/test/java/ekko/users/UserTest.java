package ekko.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests User construction, password hashing, balance operations, and seller status.
 */
class UserTest {

    @Test
    void constructor_blankUsername_throwsException() {
        for (String username : new String[] {null, "", "  "}) {
            assertThrows(IllegalArgumentException.class, () -> new User(username, "password"), username);
        }
    }

    @Test
    void constructor_blankPassword_throwsException() {
        for (String password : new String[] {null, "", "  "}) {
            assertThrows(IllegalArgumentException.class, () -> new User("user", password), password);
        }
    }

    @Test
    void constructor_validArgs_startsWithZeroBalanceAndBuyerStatus() {
        User user = new User("alice", "password");
        assertEquals("alice", user.getUsername());
        assertEquals(0, user.getBalance());
        assertFalse(user.isSeller());
    }

    @Test
    void checkPassword_correctPassword_returnsTrue() {
        User user = new User("alice", "secret");
        assertTrue(user.checkPassword("secret"));
    }

    @Test
    void checkPassword_wrongPassword_returnsFalse() {
        User user = new User("alice", "secret");
        assertFalse(user.checkPassword("wrong"));
        assertFalse(user.checkPassword(""));
        assertFalse(user.checkPassword("SECRET"));
    }

    @Test
    void hash_sameInput_returnsSameDigest() {
        assertEquals(User.hash("hello"), User.hash("hello"));
    }

    @Test
    void hash_differentInputs_returnDifferentDigests() {
        assertNotEquals(User.hash("hello"), User.hash("world"));
    }

    @Test
    void hash_producesHexString_of64Characters() {
        String digest = User.hash("test");
        // SHA-256 produces 32 bytes = 64 hex characters
        assertEquals(64, digest.length());
        assertTrue(digest.matches("[0-9a-f]+"));
    }

    @Test
    void addBalance_positiveAmount_increasesBalance() {
        User user = new User("alice", "password");
        user.addBalance(100);
        assertEquals(100, user.getBalance());
        user.addBalance(50);
        assertEquals(150, user.getBalance());
    }

    @Test
    void addBalance_zeroOrNegative_throwsException() {
        User user = new User("alice", "password");
        assertThrows(IllegalArgumentException.class, () -> user.addBalance(0));
        assertThrows(IllegalArgumentException.class, () -> user.addBalance(-1));
        assertEquals(0, user.getBalance());
    }

    @Test
    void addBalance_resultExceedsLongMaximum_throwsExceptionAndPreservesBalance() {
        User user = new User("alice", "password");
        user.setBalance(Long.MAX_VALUE);

        assertThrows(ArithmeticException.class, () -> user.addBalance(1));
        assertEquals(Long.MAX_VALUE, user.getBalance());
    }

    @Test
    void addBalance_resultExceedsIntegerMaximum_usesLongBalance() {
        User user = new User("alice", "password");
        user.setBalance(Integer.MAX_VALUE);

        user.addBalance(1);

        assertEquals((long) Integer.MAX_VALUE + 1, user.getBalance());
    }

    @Test
    void deductBalance_sufficientFunds_deductsAndReturnsTrue() {
        User user = new User("alice", "password");
        user.addBalance(200);
        assertTrue(user.deductBalance(150));
        assertEquals(50, user.getBalance());
    }

    @Test
    void deductBalance_exactBalance_deductsToZeroAndReturnsTrue() {
        User user = new User("alice", "password");
        user.addBalance(100);
        assertTrue(user.deductBalance(100));
        assertEquals(0, user.getBalance());
    }

    @Test
    void deductBalance_insufficientFunds_returnsFalseAndLeavesBalanceUnchanged() {
        User user = new User("alice", "password");
        user.addBalance(50);
        assertFalse(user.deductBalance(51));
        assertEquals(50, user.getBalance());
    }

    @Test
    void deductBalance_zeroOrNegative_throwsException() {
        User user = new User("alice", "password");
        user.addBalance(100);
        assertThrows(IllegalArgumentException.class, () -> user.deductBalance(0));
        assertThrows(IllegalArgumentException.class, () -> user.deductBalance(-10));
        assertEquals(100, user.getBalance());
    }

    @Test
    void setBalance_validAmount_replacesBalance() {
        User user = new User("alice", "password");
        user.setBalance(500);
        assertEquals(500, user.getBalance());
        user.setBalance(0);
        assertEquals(0, user.getBalance());
    }

    @Test
    void setBalance_negativeAmount_throwsException() {
        User user = new User("alice", "password");
        assertThrows(IllegalArgumentException.class, () -> user.setBalance(-1));
        assertEquals(0, user.getBalance());
    }

    @Test
    void setSeller_togglesBothWays() {
        User user = new User("alice", "password");
        user.setSeller(true);
        assertTrue(user.isSeller());
        user.setSeller(false);
        assertFalse(user.isSeller());
    }
}
