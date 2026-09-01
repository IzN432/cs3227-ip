package ekko.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests User construction, balance operations, and seller status.
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
        User user = new User("alice", "hash");
        assertEquals("alice", user.getUsername());
        assertEquals("hash", user.getHashedPassword());
        assertEquals(0, user.getBalance());
        assertFalse(user.isSeller());
    }

    @Test
    void addBalance_positiveAmount_increasesBalance() {
        User user = new User("alice", "hash");
        user.addBalance(100);
        assertEquals(100, user.getBalance());
        user.addBalance(50);
        assertEquals(150, user.getBalance());
    }

    @Test
    void addBalance_zeroOrNegative_throwsException() {
        User user = new User("alice", "hash");
        assertThrows(IllegalArgumentException.class, () -> user.addBalance(0));
        assertThrows(IllegalArgumentException.class, () -> user.addBalance(-1));
        assertEquals(0, user.getBalance());
    }

    @Test
    void deductBalance_sufficientFunds_deductsAndReturnsTrue() {
        User user = new User("alice", "hash");
        user.addBalance(200);
        assertTrue(user.deductBalance(150));
        assertEquals(50, user.getBalance());
    }

    @Test
    void deductBalance_exactBalance_deductsToZeroAndReturnsTrue() {
        User user = new User("alice", "hash");
        user.addBalance(100);
        assertTrue(user.deductBalance(100));
        assertEquals(0, user.getBalance());
    }

    @Test
    void deductBalance_insufficientFunds_returnsFalseAndLeavesBalanceUnchanged() {
        User user = new User("alice", "hash");
        user.addBalance(50);
        assertFalse(user.deductBalance(51));
        assertEquals(50, user.getBalance());
    }

    @Test
    void deductBalance_zeroOrNegative_throwsException() {
        User user = new User("alice", "hash");
        user.addBalance(100);
        assertThrows(IllegalArgumentException.class, () -> user.deductBalance(0));
        assertThrows(IllegalArgumentException.class, () -> user.deductBalance(-10));
        assertEquals(100, user.getBalance());
    }

    @Test
    void setBalance_validAmount_replacesBalance() {
        User user = new User("alice", "hash");
        user.setBalance(500);
        assertEquals(500, user.getBalance());
        user.setBalance(0);
        assertEquals(0, user.getBalance());
    }

    @Test
    void setBalance_negativeAmount_throwsException() {
        User user = new User("alice", "hash");
        assertThrows(IllegalArgumentException.class, () -> user.setBalance(-1));
        assertEquals(0, user.getBalance());
    }

    @Test
    void setSeller_togglesBothWays() {
        User user = new User("alice", "hash");
        user.setSeller(true);
        assertTrue(user.isSeller());
        user.setSeller(false);
        assertFalse(user.isSeller());
    }
}
