package ekko.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests BinListing construction, price reporting, and buyer recording.
 */
class BinListingTest {

    private BinListing bin() {
        return new BinListing("a1b2", "alice", "Lamp", "A nice lamp", 100);
    }

    @Test
    void constructor_blankFields_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new BinListing("", "alice", "Lamp", "desc", 100));
        assertThrows(IllegalArgumentException.class,
                () -> new BinListing("a1b2", "", "Lamp", "desc", 100));
        assertThrows(IllegalArgumentException.class,
                () -> new BinListing("a1b2", "alice", "", "desc", 100));
        assertThrows(IllegalArgumentException.class,
                () -> new BinListing("a1b2", "alice", "Lamp", "", 100));
    }

    @Test
    void constructor_zeroOrNegativePrice_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new BinListing("a1b2", "alice", "Lamp", "desc", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BinListing("a1b2", "alice", "Lamp", "desc", -1));
    }

    @Test
    void constructor_validArgs_startsActiveWithNoBuyer() {
        BinListing listing = bin();
        assertTrue(listing.isActive());
        assertNull(listing.getBuyerUsername());
        assertEquals(100, listing.getPrice());
        assertEquals("alice", listing.getOwnerUsername());
    }

    @Test
    void getListingPrice_returnsFixedPrice() {
        assertEquals(100, bin().getListingPrice());
    }

    @Test
    void setBuyerUsername_validUsername_recordsBuyer() {
        BinListing listing = bin();
        listing.setBuyerUsername("bob");
        assertEquals("bob", listing.getBuyerUsername());
    }

    @Test
    void setBuyerUsername_blankUsername_throwsException() {
        BinListing listing = bin();
        for (String username : new String[] {null, "", "  "}) {
            assertThrows(IllegalArgumentException.class, () -> listing.setBuyerUsername(username), username);
        }
        assertNull(listing.getBuyerUsername());
    }

    @Test
    void setState_toInactiveOrSold_isActiveReturnsFalse() {
        BinListing listing = bin();
        listing.setState(ListingState.SOLD);
        assertFalse(listing.isActive());
        listing.setState(ListingState.INACTIVE);
        assertFalse(listing.isActive());
    }

    @Test
    void setState_backToActive_isActiveReturnsTrue() {
        BinListing listing = bin();
        listing.setState(ListingState.SOLD);
        listing.setState(ListingState.ACTIVE);
        assertTrue(listing.isActive());
    }
}
