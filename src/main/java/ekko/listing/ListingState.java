package ekko.listing;

/**
 * Represents the lifecycle state of a marketplace listing.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — the listing is available for purchase or bidding.</li>
 *   <li>{@link #SOLD} — the listing was purchased (BIN) or won at auction.</li>
 *   <li>{@link #INACTIVE} — the listing was removed by the seller, or an auction
 *       expired with no bids.</li>
 * </ul>
 */
public enum ListingState {
    ACTIVE,
    SOLD,
    INACTIVE
}
