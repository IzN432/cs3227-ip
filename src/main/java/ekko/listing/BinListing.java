package ekko.listing;

/**
 * A Buy It Now listing that can be purchased immediately at a fixed price.
 */
public class BinListing extends Listing {

    private final long price;
    /** Username of the buyer, or {@code null} if the listing has not been purchased yet. */
    private String buyerUsername;

    /**
     * Creates an active BIN listing at the given fixed price.
     *
     * @param uuid short hex UUID identifying this listing.
     * @param ownerUsername username of the seller.
     * @param name item name.
     * @param description item description.
     * @param price fixed sale price; must be positive.
     * @throws IllegalArgumentException if the price is not positive.
     */
    public BinListing(String uuid, String ownerUsername, String name, String description, long price) {
        super(uuid, ownerUsername, name, description);
        if (price <= 0) {
            throw new IllegalArgumentException("BIN price must be positive.");
        }
        this.price = price;
        this.buyerUsername = null;
    }

    public long getPrice() {
        return price;
    }

    @Override
    public long getListingPrice() {
        return price;
    }

    /**
     * Returns the username of the buyer, or {@code null} if not yet purchased.
     */
    public String getBuyerUsername() {
        return buyerUsername;
    }

    /**
     * Records the buyer when the listing is purchased.
     *
     * @param buyerUsername username of the buyer; must not be blank.
     * @throws IllegalArgumentException if the username is blank.
     */
    public void setBuyerUsername(String buyerUsername) {
        if (buyerUsername == null || buyerUsername.isBlank()) {
            throw new IllegalArgumentException("Buyer username cannot be blank.");
        }
        this.buyerUsername = buyerUsername;
    }
}
