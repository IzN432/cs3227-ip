package ekko.listing;

import java.time.LocalDateTime;

/**
 * An auction listing where buyers compete by placing bids until a deadline.
 *
 * <p>The highest bid at the end of the auction wins. The winner's payment was
 * already deducted when the bid was placed; all other bidders are refunded
 * immediately when outbid.
 */
public class AuctionListing extends Listing {

    private final int basePrice;
    private final LocalDateTime endDateTime;
    /** The current highest bid, or {@code null} if no bids have been placed. */
    private Bid highestBid;

    /**
     * Creates an active auction listing with a starting price and end time.
     *
     * @param uuid short hex UUID identifying this listing.
     * @param ownerUsername username of the seller.
     * @param name item name.
     * @param description item description.
     * @param basePrice minimum opening bid; must be positive.
     * @param endDateTime auction deadline; must be in the future at the time of creation.
     * @throws IllegalArgumentException if the base price is not positive or the end time is null.
     */
    public AuctionListing(String uuid, String ownerUsername, String name, String description,
            int basePrice, LocalDateTime endDateTime) {
        super(uuid, ownerUsername, name, description);
        if (basePrice <= 0) {
            throw new IllegalArgumentException("Auction base price must be positive.");
        }
        if (endDateTime == null) {
            throw new IllegalArgumentException("Auction end date/time cannot be null.");
        }
        this.basePrice = basePrice;
        this.endDateTime = endDateTime;
        this.highestBid = null;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    /**
     * Returns the current highest bid, or {@code null} if no bids have been placed.
     */
    public Bid getHighestBid() {
        return highestBid;
    }

    /**
     * Replaces the current highest bid after the previous bidder has been refunded.
     *
     * @param bid the new highest bid.
     */
    public void setHighestBid(Bid bid) {
        this.highestBid = bid;
    }

    /**
     * Returns whether at least one bid has been placed on this auction.
     */
    public boolean hasBids() {
        return highestBid != null;
    }

    /**
     * Returns whether the auction deadline has passed.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDateTime);
    }

    /**
     * Returns the current highest bid amount, or the base price if no bids have been placed.
     */
    @Override
    public int getListingPrice() {
        return highestBid != null ? highestBid.getAmount() : basePrice;
    }
}
