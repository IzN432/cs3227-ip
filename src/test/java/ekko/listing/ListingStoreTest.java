package ekko.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests ListingStore filtering, search, and duplicate rejection.
 */
class ListingStoreTest {

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusHours(1);
    private static final LocalDateTime PAST = LocalDateTime.now().minusHours(1);

    private ListingStore store;
    private BinListing bin;
    private AuctionListing activeAuction;
    private AuctionListing expiredAuction;

    @BeforeEach
    void setUp() {
        bin = new BinListing("b001", "alice", "Lamp", "A nice lamp", 100);
        activeAuction = new AuctionListing("a001", "alice", "Watch", "A nice watch", 50, FUTURE);
        expiredAuction = new AuctionListing("a002", "bob", "Ring", "A gold ring", 200, PAST);
        store = new ListingStore(List.of(bin, activeAuction, expiredAuction));
    }

    // --- add / get / contains ---

    @Test
    void add_nullListing_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> store.add(null));
    }

    @Test
    void add_duplicateUuid_throwsException() {
        BinListing duplicate = new BinListing("b001", "bob", "Table", "A table", 80);
        assertThrows(IllegalArgumentException.class, () -> store.add(duplicate));
    }

    @Test
    void get_existingUuid_returnsListing() {
        assertEquals(bin, store.get("b001"));
    }

    @Test
    void get_unknownUuid_returnsNull() {
        assertNull(store.get("xxxx"));
    }

    @Test
    void contains_existingAndMissing_returnsCorrectly() {
        assertTrue(store.contains("b001"));
        assertFalse(store.contains("xxxx"));
    }

    @Test
    void contains_inactiveListing_stillReturnsTrue() {
        bin.setState(ListingState.SOLD);
        assertTrue(store.contains("b001"));
    }

    // --- getActive ---

    @Test
    void getActive_mixedStates_returnsOnlyActive() {
        bin.setState(ListingState.SOLD);
        List<Listing> active = store.getActive();
        assertFalse(active.contains(bin));
        assertTrue(active.contains(activeAuction));
        assertTrue(active.contains(expiredAuction));
    }

    // --- getActiveBin / getActiveAuctions ---

    @Test
    void getActiveBin_returnsOnlyActiveBinListings() {
        List<BinListing> bins = store.getActiveBin();
        assertEquals(1, bins.size());
        assertEquals("b001", bins.get(0).getUuid());
    }

    @Test
    void getActiveAuctions_returnsOnlyActiveAuctions() {
        List<AuctionListing> auctions = store.getActiveAuctions();
        assertEquals(2, auctions.size());
    }

    // --- getExpiredAuctions ---

    @Test
    void getExpiredAuctions_returnsOnlyExpiredActiveAuctions() {
        List<AuctionListing> expired = store.getExpiredAuctions();
        assertEquals(1, expired.size());
        assertEquals("a002", expired.get(0).getUuid());
    }

    @Test
    void getExpiredAuctions_afterMarkingInactive_excludesIt() {
        expiredAuction.setState(ListingState.INACTIVE);
        assertTrue(store.getExpiredAuctions().isEmpty());
    }

    // --- getByOwner ---

    @Test
    void getByOwner_returnsAllStatesForThatOwner() {
        bin.setState(ListingState.SOLD);
        List<Listing> aliceListings = store.getByOwner("alice");
        assertEquals(2, aliceListings.size());
        assertTrue(aliceListings.contains(bin));
        assertTrue(aliceListings.contains(activeAuction));
    }

    @Test
    void getByOwner_unknownOwner_returnsEmpty() {
        assertTrue(store.getByOwner("nobody").isEmpty());
    }

    // --- search(keyword) ---

    @Test
    void search_matchingName_returnsListing() {
        List<Listing> results = store.search("Lamp");
        assertEquals(1, results.size());
        assertEquals("b001", results.get(0).getUuid());
    }

    @Test
    void search_matchingDescription_returnsListing() {
        List<Listing> results = store.search("gold");
        assertEquals(1, results.size());
        assertEquals("a002", results.get(0).getUuid());
    }

    @Test
    void search_noMatch_returnsEmpty() {
        assertTrue(store.search("bicycle").isEmpty());
    }

    @Test
    void search_inactiveListing_excluded() {
        bin.setState(ListingState.SOLD);
        assertTrue(store.search("Lamp").isEmpty());
    }

    @Test
    void search_blankKeyword_throwsException() {
        for (String keyword : new String[] {null, "", "  "}) {
            assertThrows(IllegalArgumentException.class, () -> store.search(keyword), keyword);
        }
    }

    // --- search(keyword, low, high) ---

    @Test
    void search_priceRange_filtersCorrectly() {
        // bin is 100, expiredAuction base is 200
        List<Listing> results = store.search("nice", 50, 150);
        assertEquals(2, results.size()); // lamp (100) and watch (50)
        assertFalse(results.contains(expiredAuction));
    }

    @Test
    void search_negativeLow_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> store.search("Lamp", -1, 100));
    }

    @Test
    void search_highBelowLow_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> store.search("Lamp", 100, 50));
    }

    // --- size / isEmpty ---

    @Test
    void size_andIsEmpty_reflectContents() {
        assertFalse(store.isEmpty());
        assertEquals(3, store.size());

        ListingStore empty = new ListingStore(List.of());
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
    }
}
