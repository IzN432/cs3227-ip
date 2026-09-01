package ekko.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ekko.users.User;
import ekko.users.UserStore;

/**
 * Tests AuctionProcessor auction resolution, fund distribution, and notifications.
 */
class AuctionProcessorTest {

    private static final LocalDateTime PAST = LocalDateTime.now().minusHours(1);
    private static final LocalDateTime FUTURE = LocalDateTime.now().plusHours(1);

    private User seller() {
        return new User("seller", "hash");
    }

    private User winner() {
        return new User("winner", "hash");
    }

    @Test
    void process_emptyStore_returnsEmptyMap() {
        AuctionProcessor processor = new AuctionProcessor(
                new ListingStore(List.of()), new UserStore(List.of()));
        assertTrue(processor.process().isEmpty());
    }

    @Test
    void process_activeNonExpiredAuction_doesNothing() {
        AuctionListing active = new AuctionListing("a001", "seller", "Watch", "desc", 50, FUTURE);
        AuctionProcessor processor = new AuctionProcessor(
                new ListingStore(List.of(active)), new UserStore(List.of(seller())));
        assertTrue(processor.process().isEmpty());
        assertTrue(active.isActive());
    }

    @Test
    void process_expiredNoBids_marksInactiveAndNotifiesSeller() {
        AuctionListing auction = new AuctionListing("a001", "seller", "Watch", "desc", 50, PAST);
        User sellerUser = seller();
        AuctionProcessor processor = new AuctionProcessor(
                new ListingStore(List.of(auction)), new UserStore(List.of(sellerUser)));

        Map<String, List<String>> notifications = processor.process();

        assertEquals(ListingState.INACTIVE, auction.getState());
        assertEquals(0, sellerUser.getBalance());
        assertTrue(notifications.containsKey("seller"));
        assertEquals(1, notifications.get("seller").size());
        assertTrue(notifications.get("seller").get(0).contains("expired with no bids"));
    }

    @Test
    void process_expiredWithBid_marksSoldPaysSellerAndNotifiesBoth() {
        AuctionListing auction = new AuctionListing("a001", "seller", "Watch", "desc", 50, PAST);
        auction.setHighestBid(new Bid("winner", 150));
        User sellerUser = seller();
        User winnerUser = winner();
        AuctionProcessor processor = new AuctionProcessor(
                new ListingStore(List.of(auction)),
                new UserStore(List.of(sellerUser, winnerUser)));

        Map<String, List<String>> notifications = processor.process();

        assertEquals(ListingState.SOLD, auction.getState());
        assertEquals(150, sellerUser.getBalance());
        assertTrue(notifications.containsKey("seller"));
        assertTrue(notifications.containsKey("winner"));
        assertTrue(notifications.get("seller").get(0).contains("won by winner"));
        assertTrue(notifications.get("winner").get(0).contains("You won"));
    }

    @Test
    void process_multipleExpiredAuctions_resolvesAll() {
        AuctionListing noBids = new AuctionListing("a001", "seller", "Watch", "desc", 50, PAST);
        AuctionListing withBid = new AuctionListing("a002", "seller", "Ring", "desc", 100, PAST);
        withBid.setHighestBid(new Bid("winner", 200));
        User sellerUser = seller();
        User winnerUser = winner();
        AuctionProcessor processor = new AuctionProcessor(
                new ListingStore(List.of(noBids, withBid)),
                new UserStore(List.of(sellerUser, winnerUser)));

        Map<String, List<String>> notifications = processor.process();

        assertEquals(ListingState.INACTIVE, noBids.getState());
        assertEquals(ListingState.SOLD, withBid.getState());
        assertEquals(200, sellerUser.getBalance());
        assertEquals(2, notifications.get("seller").size());
        assertTrue(notifications.containsKey("winner"));
    }

    @Test
    void process_alreadyResolvedAuction_notProcessedAgain() {
        AuctionListing auction = new AuctionListing("a001", "seller", "Watch", "desc", 50, PAST);
        User sellerUser = seller();
        AuctionProcessor processor = new AuctionProcessor(
                new ListingStore(List.of(auction)), new UserStore(List.of(sellerUser)));

        processor.process(); // first call marks INACTIVE
        Map<String, List<String>> second = processor.process(); // second call finds no expired active

        assertTrue(second.isEmpty());
        assertEquals(0, sellerUser.getBalance());
    }
}
