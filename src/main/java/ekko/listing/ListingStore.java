package ekko.listing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the application's listing collection and its lookup and filter operations.
 *
 * <p>Listings are keyed by their short hex UUID for O(1) lookup. Order is not guaranteed.
 */
public class ListingStore {

    private final Map<String, Listing> listings;

    /**
     * Creates a listing store containing the supplied listings.
     *
     * @param listings initial listings; must not contain duplicates or null entries.
     * @throws IllegalArgumentException if the collection contains a null or duplicate UUID.
     */
    public ListingStore(List<Listing> listings) {
        this.listings = new HashMap<>();
        listings.forEach(this::add);
    }

    /**
     * Adds a listing to the store.
     *
     * @param listing listing to add; must not be null or share a UUID with an existing listing.
     * @throws IllegalArgumentException if the listing is null or its UUID is already registered.
     */
    public void add(Listing listing) {
        if (listing == null) {
            throw new IllegalArgumentException("A listing is required.");
        }
        if (listings.containsKey(listing.getUuid())) {
            throw new IllegalArgumentException("A listing with UUID '" + listing.getUuid() + "' already exists.");
        }
        listings.put(listing.getUuid(), listing);
    }

    /**
     * Returns the listing with the given UUID, or {@code null} if not found.
     *
     * @param uuid UUID to look up.
     * @return matching listing, or {@code null}.
     */
    public Listing get(String uuid) {
        return listings.get(uuid);
    }

    /**
     * Returns whether a UUID is already in use by any listing, including inactive ones.
     *
     * @param uuid UUID to check.
     */
    public boolean contains(String uuid) {
        return listings.containsKey(uuid);
    }

    /**
     * Returns all active listings of any type.
     */
    public List<Listing> getActive() {
        return listings.values().stream()
                .filter(Listing::isActive)
                .toList();
    }

    /**
     * Returns all active BIN listings.
     */
    public List<BinListing> getActiveBin() {
        return listings.values().stream()
                .filter(Listing::isActive)
                .filter(l -> l instanceof BinListing)
                .map(l -> (BinListing) l)
                .toList();
    }

    /**
     * Returns all active auction listings.
     */
    public List<AuctionListing> getActiveAuctions() {
        return listings.values().stream()
                .filter(Listing::isActive)
                .filter(l -> l instanceof AuctionListing)
                .map(l -> (AuctionListing) l)
                .toList();
    }

    /**
     * Returns all active auctions that have passed their end time and need processing.
     */
    public List<AuctionListing> getExpiredAuctions() {
        return listings.values().stream()
                .filter(Listing::isActive)
                .filter(l -> l instanceof AuctionListing)
                .map(l -> (AuctionListing) l)
                .filter(AuctionListing::isExpired)
                .toList();
    }

    /**
     * Returns all listings owned by the given seller, regardless of state.
     *
     * @param ownerUsername username of the seller.
     */
    public List<Listing> getByOwner(String ownerUsername) {
        return listings.values().stream()
                .filter(l -> l.getOwnerUsername().equals(ownerUsername))
                .toList();
    }

    /**
     * Returns all completed purchases made by the given buyer.
     *
     * <p>A purchase is either a sold BIN listing that records the buyer, or a sold
     * auction whose highest bidder is the winner.
     *
     * @param buyerUsername username of the buyer.
     */
    public List<Listing> getPurchasesByBuyer(String buyerUsername) {
        return listings.values().stream()
                .filter(listing -> listing.getState() == ListingState.SOLD)
                .filter(listing -> isPurchasedBy(listing, buyerUsername))
                .toList();
    }

    /**
     * Returns whether the listing records the given user as its buyer or auction winner.
     */
    private boolean isPurchasedBy(Listing listing, String buyerUsername) {
        if (listing instanceof BinListing bin) {
            return buyerUsername.equals(bin.getBuyerUsername());
        }
        AuctionListing auction = (AuctionListing) listing;
        return auction.hasBids()
                && buyerUsername.equals(auction.getHighestBid().getBidderUsername());
    }

    /**
     * Returns active listings whose name or description contains the given
     * case-sensitive substring, with no price filter applied.
     *
     * @param keyword search term; must not be blank.
     * @throws IllegalArgumentException if the keyword is blank.
     */
    public List<Listing> search(String keyword) {
        return search(keyword, 0, Long.MAX_VALUE);
    }

    /**
     * Returns active listings whose name or description contains the given
     * case-sensitive substring and whose effective price falls within the given range.
     *
     * <p>For BIN listings the effective price is the fixed sale price. For auctions
     * it is the current highest bid, or the base price if no bids have been placed.
     *
     * @param keyword search term; must not be blank.
     * @param lowPrice minimum price, inclusive; must be zero or greater.
     * @param highPrice maximum price, inclusive; must be greater than or equal to {@code lowPrice}.
     * @throws IllegalArgumentException if the keyword is blank, either price is negative,
     *         or {@code highPrice} is less than {@code lowPrice}.
     */
    public List<Listing> search(String keyword, long lowPrice, long highPrice) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Search keyword cannot be blank.");
        }
        if (lowPrice < 0) {
            throw new IllegalArgumentException("Minimum price cannot be negative.");
        }
        if (highPrice < lowPrice) {
            throw new IllegalArgumentException("Maximum price cannot be less than minimum price.");
        }
        return listings.values().stream()
                .filter(Listing::isActive)
                .filter(l -> l.getName().contains(keyword) || l.getDescription().contains(keyword))
                .filter(l -> l.getListingPrice() >= lowPrice && l.getListingPrice() <= highPrice)
                .toList();
    }

    /**
     * Returns whether the store contains no listings.
     */
    public boolean isEmpty() {
        return listings.isEmpty();
    }

    /**
     * Returns the total number of listings, including inactive ones.
     */
    public int size() {
        return listings.size();
    }

    /**
     * Returns an immutable snapshot of all listings for persistence.
     */
    public Collection<Listing> asCollection() {
        return Collections.unmodifiableCollection(listings.values());
    }
}
