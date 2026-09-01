package ekko.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests Bid construction and field access.
 */
class BidTest {

    @Test
    void constructor_blankUsername_throwsException() {
        for (String username : new String[] {null, "", "  "}) {
            assertThrows(IllegalArgumentException.class, () -> new Bid(username, 100), username);
        }
    }

    @Test
    void constructor_zeroOrNegativeAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new Bid("alice", 0));
        assertThrows(IllegalArgumentException.class, () -> new Bid("alice", -1));
    }

    @Test
    void constructor_validArgs_storesFields() {
        Bid bid = new Bid("alice", 250);
        assertEquals("alice", bid.getBidderUsername());
        assertEquals(250, bid.getAmount());
    }

    @Test
    void constructor_minimumPositiveAmount_accepted() {
        Bid bid = new Bid("alice", 1);
        assertEquals(1, bid.getAmount());
    }
}
