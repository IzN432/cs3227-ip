package ekko.listing;

/**
 * Represents a marketplace listing created by a seller.
 *
 * <p>Each listing has a short UUID for user-facing identification, an owner
 * (identified by username), a name, a description, and a lifecycle state.
 * Concrete subclasses define the sale mechanism (BIN or auction).
 */
public abstract class Listing {

    private final String uuid;
    private final String ownerUsername;
    private final String name;
    private final String description;
    private ListingState state;

    /**
     * Creates an active listing with the supplied identifiers and content.
     *
     * @param uuid short hex UUID identifying this listing.
     * @param ownerUsername username of the seller who created the listing.
     * @param name item name; must not be blank.
     * @param description item description; must not be blank.
     * @throws IllegalArgumentException if any argument is null or blank.
     */
    protected Listing(String uuid, String ownerUsername, String name, String description) {
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalArgumentException("Listing UUID cannot be blank.");
        }
        if (ownerUsername == null || ownerUsername.isBlank()) {
            throw new IllegalArgumentException("Owner username cannot be blank.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Listing name cannot be blank.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Listing description cannot be blank.");
        }
        this.uuid = uuid;
        this.ownerUsername = ownerUsername;
        this.name = name;
        this.description = description;
        this.state = ListingState.ACTIVE;
    }

    public String getUuid() {
        return uuid;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ListingState getState() {
        return state;
    }

    /**
     * Updates the lifecycle state of this listing.
     *
     * @param state new state.
     */
    public void setState(ListingState state) {
        if (state == null) {
            throw new IllegalArgumentException("Listing state cannot be null.");
        }
        this.state = state;
    }

    /**
     * Returns whether this listing is currently available for purchase or bidding.
     */
    public boolean isActive() {
        return state == ListingState.ACTIVE;
    }

    /**
     * Returns the effective price used for filtering and display.
     *
     * <p>For BIN listings this is the fixed sale price. For auction listings
     * this is the current highest bid, or the base price if no bids have been placed.
     */
    public abstract long getListingPrice();
}
