package ekko.listing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ekko.users.User;
import ekko.users.UserStore;

/**
 * Resolves expired auctions and distributes funds to the appropriate users.
 *
 * <p>This class contains only pure logic and has no dependency on JavaFX.
 * The caller is responsible for setting up a polling mechanism (e.g. a JavaFX
 * {@code Timeline}) that invokes {@link #process()} at a regular interval.
 *
 * <p>For each expired auction:
 * <ul>
 *   <li>If bids were placed: the listing is marked SOLD, and the seller receives
 *       the winning bid amount. The winner's funds were already deducted when the
 *       bid was placed, so no further deduction is needed.</li>
 *   <li>If no bids were placed: the listing is marked INACTIVE.</li>
 * </ul>
 *
 * <p>{@link #process()} returns a map of username to notification messages so
 * the caller can append them to each user's conversation history and optionally
 * display them if the affected user is currently logged in.
 */
public class AuctionProcessor {

    private final ListingStore listingStore;
    private final UserStore userStore;

    /**
     * Creates a processor operating on the supplied live stores.
     *
     * @param listingStore store containing all listings.
     * @param userStore store containing all users.
     */
    public AuctionProcessor(ListingStore listingStore, UserStore userStore) {
        this.listingStore = listingStore;
        this.userStore = userStore;
    }

    /**
     * Finds all active auctions that have passed their end time and resolves them.
     *
     * @return a map of username to the list of notification messages produced for
     *         that user; only affected users appear in the map.
     */
    public Map<String, List<String>> process() {
        Map<String, List<String>> notifications = new HashMap<>();

        for (AuctionListing auction : listingStore.getExpiredAuctions()) {
            if (auction.hasBids()) {
                resolveSold(auction, notifications);
            } else {
                resolveExpired(auction, notifications);
            }
        }

        return notifications;
    }

    /**
     * Marks a winning auction as SOLD, pays the seller, and notifies both parties.
     */
    private void resolveSold(AuctionListing auction, Map<String, List<String>> notifications) {
        Bid winningBid = auction.getHighestBid();
        String sellerUsername = auction.getOwnerUsername();
        String winnerUsername = winningBid.getBidderUsername();
        int amount = winningBid.getAmount();

        auction.setState(ListingState.SOLD);

        User seller = userStore.get(sellerUsername);
        if (seller != null) {
            seller.addBalance(amount);
            addNotification(notifications, sellerUsername, String.format(
                    "Your auction \"%s\" [%s] was won by %s for %d coins.",
                    auction.getName(), auction.getUuid(), winnerUsername, amount
            ));
        }

        addNotification(notifications, winnerUsername, String.format(
                "You won the auction \"%s\" [%s] for %d coins.",
                auction.getName(), auction.getUuid(), amount
        ));
    }

    /**
     * Marks an auction with no bids as INACTIVE and notifies the seller.
     */
    private void resolveExpired(AuctionListing auction, Map<String, List<String>> notifications) {
        auction.setState(ListingState.INACTIVE);

        addNotification(notifications, auction.getOwnerUsername(), String.format(
                "Your auction \"%s\" [%s] expired with no bids.",
                auction.getName(), auction.getUuid()
        ));
    }

    /**
     * Appends a message to the notification list for the given user.
     */
    private void addNotification(Map<String, List<String>> notifications,
            String username, String message) {
        notifications.computeIfAbsent(username, k -> new ArrayList<>()).add(message);
    }
}
