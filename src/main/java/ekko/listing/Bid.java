package ekko.listing;

/**
 * Represents a single bid placed on an auction listing.
 *
 * <p>The bidder is identified by username rather than a {@code User} object reference
 * so that bids can be serialized and deserialized without requiring access to the
 * user store.
 */
public class Bid {

    private final String bidderUsername;
    private final long amount;

    /**
     * Creates a bid with the given bidder and amount.
     *
     * @param bidderUsername username of the bidder.
     * @param amount bid amount; must be greater than zero.
     * @throws IllegalArgumentException if the username is blank or the amount is not positive.
     */
    public Bid(String bidderUsername, long amount) {
        if (bidderUsername == null || bidderUsername.isBlank()) {
            throw new IllegalArgumentException("Bidder username cannot be blank.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Bid amount must be positive.");
        }
        this.bidderUsername = bidderUsername;
        this.amount = amount;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public long getAmount() {
        return amount;
    }
}
