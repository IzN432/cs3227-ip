package ekko.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Tests AuctionListing construction, price reporting, bid tracking, and expiry detection.
 */
class AuctionListingTest {

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusHours(1);
    private static final LocalDateTime PAST = LocalDateTime.now().minusHours(1);

    private AuctionListing auction() {
        return new AuctionListing("a1b2", "alice", "Watch", "A nice watch", 50, FUTURE);
    }

    @Test
    void constructor_zeroOrNegativeBasePrice_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuctionListing("a1b2", "alice", "Watch", "desc", 0, FUTURE));
        assertThrows(IllegalArgumentException.class,
                () -> new AuctionListing("a1b2", "alice", "Watch", "desc", -1, FUTURE));
    }

    @Test
    void constructor_nullEndDateTime_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuctionListing("a1b2", "alice", "Watch", "desc", 50, null));
    }

    @Test
    void constructor_validArgs_startsActiveWithNoBids() {
        AuctionListing listing = auction();
        assertTrue(listing.isActive());
        assertFalse(listing.hasBids());
        assertNull(listing.getHighestBid());
        assertEquals(50, listing.getBasePrice());
    }

    @Test
    void getListingPrice_noBids_returnsBasePrice() {
        assertEquals(50, auction().getListingPrice());
    }

    @Test
    void getListingPrice_withBid_returnsBidAmount() {
        AuctionListing listing = auction();
        listing.setHighestBid(new Bid("bob", 120));
        assertEquals(120, listing.getListingPrice());
    }

    @Test
    void hasBids_afterSetHighestBid_returnsTrue() {
        AuctionListing listing = auction();
        assertFalse(listing.hasBids());
        listing.setHighestBid(new Bid("bob", 75));
        assertTrue(listing.hasBids());
        assertEquals("bob", listing.getHighestBid().getBidderUsername());
    }

    @Test
    void isExpired_futureEndTime_returnsFalse() {
        assertFalse(auction().isExpired());
    }

    @Test
    void isExpired_pastEndTime_returnsTrue() {
        AuctionListing listing = new AuctionListing("a1b2", "alice", "Watch", "desc", 50, PAST);
        assertTrue(listing.isExpired());
    }

    @Test
    void getListingPrice_bidReplacedByHigher_returnsLatestBidAmount() {
        AuctionListing listing = auction();
        listing.setHighestBid(new Bid("bob", 75));
        listing.setHighestBid(new Bid("carol", 120));
        assertEquals(120, listing.getListingPrice());
        assertEquals("carol", listing.getHighestBid().getBidderUsername());
    }
}
