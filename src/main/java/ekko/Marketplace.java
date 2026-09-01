package ekko;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;

import ekko.datetime.DateTimeParser;
import ekko.listing.AuctionListing;
import ekko.listing.Bid;
import ekko.listing.BinListing;
import ekko.listing.Listing;
import ekko.listing.ListingState;
import ekko.listing.ListingStore;
import ekko.parser.ArgumentName;
import ekko.parser.ArgumentParser;
import ekko.parser.Command;
import ekko.parser.Parser;
import ekko.parser.ParsedArguments;
import ekko.ui.Ui;
import ekko.users.User;
import ekko.users.UserStore;

/**
 * Coordinates marketplace commands for a logged-in user.
 *
 * <p>Storage is not yet wired; all state is in-memory and resets on each launch.
 */
public class Marketplace {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Ui ui;
    private final User currentUser;
    private final UserStore userStore;
    private final ListingStore listingStore;

    /**
     * Creates a marketplace session for the given logged-in user.
     *
     * @param ui user interaction endpoint.
     * @param currentUser the authenticated user for this session.
     * @param userStore all registered users.
     * @param listingStore all marketplace listings.
     */
    public Marketplace(Ui ui, User currentUser, UserStore userStore, ListingStore listingStore) {
        this.ui = ui;
        this.currentUser = currentUser;
        this.userStore = userStore;
        this.listingStore = listingStore;
    }

    /**
     * Processes a single command entered by the user.
     *
     * @param input raw command text.
     * @return {@code true} if the session should end.
     */
    public boolean processCommand(String input) {
        try {
            return handleInput(input.trim());
        } catch (AppException e) {
            ui.showError(e.getMessage());
            return false;
        }
    }

    /**
     * Parses and dispatches the command to the appropriate handler.
     */
    private boolean handleInput(String input) throws AppException {
        Parser.ParsedCommand parsed = Parser.parse(input);
        Command command = parsed.command();
        String arguments = parsed.arguments();

        switch (command) {
            case AUCTION -> createAuction(arguments);
            case BALANCE -> showBalance();
            case BECOMESELLER -> becomeSeller();
            case BID -> placeBid(arguments);
            case BIN -> createBin(arguments);
            case BUY -> buyListing(arguments);
            case FIND -> findListings(arguments);
            case LIST -> listListings();
            case MYLISTINGS -> myListings();
            case TOPUP -> topUp(arguments);
            case WITHDRAW -> withdraw(arguments);
            case BYE -> {
                return true;
            }
            default -> throw new AppException("This command is not yet implemented.");
        }
        return false;
    }

    /**
     * Purchases an active BIN listing immediately at its fixed price.
     *
     * @param arguments raw argument text containing the listing UUID.
     * @throws AppException if the UUID is missing or unknown, the listing is not an active BIN,
     *         the buyer owns the listing, or funds are insufficient.
     */
    private void buyListing(String arguments) throws AppException {
        String uuid = requireField(arguments, "listing UUID");
        Listing listing = listingStore.get(uuid);
        if (listing == null) {
            throw new AppException("No listing found with UUID [" + uuid + "].");
        }
        if (!(listing instanceof BinListing bin)) {
            throw new AppException("Listing [" + uuid + "] is not a BIN listing. Use 'bid' for auctions.");
        }
        if (!bin.isActive()) {
            throw new AppException("Listing [" + uuid + "] is no longer available.");
        }
        if (bin.getOwnerUsername().equals(currentUser.getUsername())) {
            throw new AppException("You cannot buy your own listing.");
        }
        long price = bin.getPrice();
        User seller = userStore.get(bin.getOwnerUsername());
        ensureCanCredit(seller, price, "The seller cannot receive this payment.");
        if (!currentUser.deductBalance(price)) {
            throw new AppException("Insufficient balance. You have " + currentUser.getBalance()
                    + " coins, but this listing costs " + price + " coins.");
        }
        if (seller != null) {
            seller.addBalance(price);
        }
        bin.setState(ListingState.SOLD);
        bin.setBuyerUsername(currentUser.getUsername());
        ui.showMessage("Purchased [" + uuid + "]: " + bin.getName() + " for " + price
                + " coins. Balance: " + currentUser.getBalance() + " coins.");
    }

    /**
     * Places a bid on an active auction on behalf of the current user.
     *
     * <p>The bid amount is deducted immediately. If the bidder is subsequently
     * outbid, their funds are refunded at that point.
     *
     * @param arguments raw argument text: {@code <uuid> /price <amount>}.
     * @throws AppException if the UUID is missing or unknown, the listing is not an active auction,
     *         the auction has expired, the bidder already holds the highest bid,
     *         the amount does not exceed the current highest bid, or funds are insufficient.
     */
    private void placeBid(String arguments) throws AppException {
        ParsedArguments parsed = ArgumentParser.parse(arguments, Set.of(ArgumentName.PRICE));
        String uuid = requireField(parsed.getDescription(), "listing UUID");
        if (!parsed.containsArgument(ArgumentName.PRICE)) {
            throw new AppException("Please provide a bid amount: bid <uuid> /price <amount>");
        }
        long amount = parsePositiveLong(parsed.getArgument(ArgumentName.PRICE), "bid amount after /price");

        Listing listing = listingStore.get(uuid);
        if (listing == null) {
            throw new AppException("No listing found with UUID [" + uuid + "].");
        }
        if (!(listing instanceof AuctionListing auction)) {
            throw new AppException("Listing [" + uuid + "] is not an auction.");
        }
        if (!auction.isActive()) {
            throw new AppException("Auction [" + uuid + "] is no longer active.");
        }
        if (auction.isExpired()) {
            throw new AppException("Auction [" + uuid + "] has already ended.");
        }
        if (auction.hasBids() && auction.getHighestBid().getBidderUsername().equals(currentUser.getUsername())) {
            throw new AppException("You already hold the highest bid on [" + uuid + "].");
        }
        if (auction.hasBids() && auction.getListingPrice() == Long.MAX_VALUE) {
            throw new AppException("Auction [" + uuid + "] has reached the maximum supported bid.");
        }
        long minimumBid = auction.hasBids() ? auction.getListingPrice() + 1 : auction.getBasePrice();
        if (amount < minimumBid) {
            throw new AppException("Your bid must be at least " + minimumBid + " coins"
                    + (auction.hasBids() ? " to beat the current highest bid." : " (the starting bid)."));
        }

        User previousBidder = null;
        Bid previousBid = null;
        if (auction.hasBids()) {
            previousBid = auction.getHighestBid();
            previousBidder = userStore.get(previousBid.getBidderUsername());
            ensureCanCredit(previousBidder, previousBid.getAmount(),
                    "The previous bidder cannot receive their refund.");
        }
        if (!currentUser.deductBalance(amount)) {
            throw new AppException("Insufficient balance. You have " + currentUser.getBalance() + " coins.");
        }

        // Refund the previous highest bidder before replacing the bid.
        if (previousBidder != null) {
            previousBidder.addBalance(previousBid.getAmount());
        }

        auction.setHighestBid(new Bid(currentUser.getUsername(), amount));
        ui.showMessage("Bid placed on [" + uuid + "]: " + auction.getName()
                + " — " + amount + " coins. Balance: " + currentUser.getBalance() + " coins.");
    }

    /**
     * Creates a BIN listing for the current user.
     *
     * @param arguments raw argument text: {@code <name> /desc <desc> /price <price>}.
     * @throws AppException if the user is not a seller, required fields are missing or invalid.
     */
    private void createBin(String arguments) throws AppException {
        requireSeller();
        ParsedArguments parsed = ArgumentParser.parse(arguments, Set.of(ArgumentName.DESC, ArgumentName.PRICE));
        String name = requireField(parsed.getDescription(), "item name");
        if (!parsed.containsArgument(ArgumentName.DESC)) {
            throw new AppException("Please provide a description: bin <name> /desc <description> /price <price>");
        }
        String desc = requireField(parsed.getArgument(ArgumentName.DESC), "description after /desc");
        if (!parsed.containsArgument(ArgumentName.PRICE)) {
            throw new AppException("Please provide a price: bin <name> /desc <description> /price <price>");
        }
        long price = parsePositiveLong(parsed.getArgument(ArgumentName.PRICE), "price after /price");
        String uuid = generateUuid();
        listingStore.add(new BinListing(uuid, currentUser.getUsername(), name, desc, price));
        ui.showMessage("BIN listing created [" + uuid + "]: " + name + " — " + price + " coins.");
    }

    /**
     * Creates an auction listing for the current user.
     *
     * @param arguments raw argument text:
     *        {@code <name> /desc <desc> /price <starting bid> /end <date/time>}.
     * @throws AppException if the user is not a seller, required fields are missing, invalid,
     *         or the end time is not in the future.
     */
    private void createAuction(String arguments) throws AppException {
        requireSeller();
        ParsedArguments parsed = ArgumentParser.parse(arguments,
                Set.of(ArgumentName.DESC, ArgumentName.PRICE, ArgumentName.END));
        String usage = "auction <name> /desc <description> /price <starting bid> /end <date/time>";
        String name = requireField(parsed.getDescription(), "item name");
        if (!parsed.containsArgument(ArgumentName.DESC)) {
            throw new AppException("Please provide a description: " + usage);
        }
        String desc = requireField(parsed.getArgument(ArgumentName.DESC), "description after /desc");
        if (!parsed.containsArgument(ArgumentName.PRICE)) {
            throw new AppException("Please provide a starting bid: " + usage);
        }
        long basePrice = parsePositiveLong(parsed.getArgument(ArgumentName.PRICE), "starting bid after /price");
        if (!parsed.containsArgument(ArgumentName.END)) {
            throw new AppException("Please provide an end date/time: " + usage);
        }
        String endText = parsed.getArgument(ArgumentName.END);
        try {
            DateTimeParser.parseDate(endText);
            throw new AppException("Please include a time with the end date. Example: /end 2026-09-02 1800");
        } catch (AppException e) {
            throw e;
        } catch (Exception ignored) {
            // Not a date-only string — proceed to parse as date/time below.
        }
        LocalDateTime endDateTime;
        try {
            endDateTime = DateTimeParser.parse(endText);
        } catch (Exception e) {
            throw new AppException("Invalid end date/time. Example: /end 2026-09-02 1800");
        }
        if (!endDateTime.isAfter(LocalDateTime.now())) {
            throw new AppException("End date/time must be in the future.");
        }
        String uuid = generateUuid();
        listingStore.add(new AuctionListing(uuid, currentUser.getUsername(), name, desc, basePrice, endDateTime));
        ui.showMessage("Auction listing created [" + uuid + "]: " + name
                + " — starting at " + basePrice + " coins"
                + ", ends " + DateTimeParser.format(endDateTime) + ".");
    }

    /**
     * Generates a unique 4-byte hex UUID not already present in the listing store.
     */
    private String generateUuid() {
        byte[] bytes = new byte[4];
        String uuid;
        do {
            RANDOM.nextBytes(bytes);
            uuid = HexFormat.of().formatHex(bytes);
        } while (listingStore.contains(uuid));
        return uuid;
    }

    /**
     * Throws if the current user does not have seller status.
     *
     * @throws AppException if the user is not a seller.
     */
    private void requireSeller() throws AppException {
        if (!currentUser.isSeller()) {
            throw new AppException("You must be a seller to list items. Use 'becomeseller' first.");
        }
    }

    /**
     * Returns the value if non-blank, otherwise throws with a field-specific message.
     *
     * @param value value to check.
     * @param fieldName human-readable field name for the error message.
     * @return the trimmed value.
     * @throws AppException if the value is blank.
     */
    private String requireField(String value, String fieldName) throws AppException {
        if (value == null || value.isBlank()) {
            throw new AppException("Please provide a " + fieldName + ".");
        }
        return value.trim();
    }

    /**
     * Searches active listings by keyword, with optional price range filtering.
     *
     * <p>Without {@code /low} or {@code /high} the search covers all prices.
     * Either bound may be omitted independently; the missing bound defaults to
     * 0 (for {@code /low}) or no upper limit (for {@code /high}).
     *
     * @param arguments raw argument text: {@code <keyword> [/low <min>] [/high <max>]}.
     * @throws AppException if the keyword is missing, either price bound is invalid,
     *         or the low bound exceeds the high bound.
     */
    private void findListings(String arguments) throws AppException {
        ParsedArguments parsed = ArgumentParser.parse(arguments, Set.of(ArgumentName.LOW, ArgumentName.HIGH));
        String keyword = requireField(parsed.getDescription(), "search keyword");

        long low = 0;
        long high = Long.MAX_VALUE;
        if (parsed.containsArgument(ArgumentName.LOW)) {
            low = parsePositiveLong(parsed.getArgument(ArgumentName.LOW), "minimum price after /low");
        }
        if (parsed.containsArgument(ArgumentName.HIGH)) {
            high = parsePositiveLong(parsed.getArgument(ArgumentName.HIGH), "maximum price after /high");
        }
        if (high < low) {
            throw new AppException("Maximum price (" + high + ") cannot be less than minimum price (" + low + ").");
        }

        java.util.List<Listing> results;
        try {
            results = listingStore.search(keyword, low, high);
        } catch (IllegalArgumentException e) {
            throw new AppException(e.getMessage());
        }

        if (results.isEmpty()) {
            ui.showMessage("No listings found for \"" + keyword + "\".");
            return;
        }
        StringBuilder sb = new StringBuilder("Results for \"" + keyword + "\" (" + results.size() + "):");
        for (Listing listing : results) {
            sb.append("\n\n").append(formatListing(listing, false));
        }
        ui.showMessage(sb.toString());
    }

    /**
     * Displays all active listings.
     */
    private void listListings() {
        java.util.List<Listing> listings = listingStore.getActive();
        if (listings.isEmpty()) {
            ui.showMessage("No active listings.");
            return;
        }
        StringBuilder sb = new StringBuilder("Active listings (" + listings.size() + "):");
        for (Listing listing : listings) {
            sb.append("\n\n").append(formatListing(listing, false));
        }
        ui.showMessage(sb.toString());
    }

    /**
     * Displays all listings owned by the current user, including inactive and sold ones.
     */
    private void myListings() {
        java.util.List<Listing> listings = listingStore.getByOwner(currentUser.getUsername());
        if (listings.isEmpty()) {
            ui.showMessage("You have no listings.");
            return;
        }
        StringBuilder sb = new StringBuilder("Your listings (" + listings.size() + "):");
        for (Listing listing : listings) {
            sb.append("\n\n").append(formatListing(listing, true));
        }
        ui.showMessage(sb.toString());
    }

    /**
     * Returns a multi-line summary of a single listing.
     *
     * @param listing listing to format.
     * @param showState whether to include the listing state (used for the owner's own view).
     */
    private String formatListing(Listing listing, boolean showState) {
        String typeTag;
        String priceLine;

        if (listing instanceof BinListing bin) {
            typeTag = "[BIN]";
            priceLine = "Price: " + bin.getPrice() + " coins";
            if (showState && bin.getBuyerUsername() != null) {
                priceLine += " · Sold to: " + bin.getBuyerUsername();
            }
        } else {
            AuctionListing auction = (AuctionListing) listing;
            typeTag = "[AUC]";
            if (auction.hasBids()) {
                priceLine = "Current bid: " + auction.getListingPrice() + " coins";
            } else {
                priceLine = "Starting bid: " + auction.getBasePrice() + " coins";
            }
            priceLine += " · Ends: " + DateTimeParser.format(auction.getEndDateTime());
        }

        String stateTag = showState ? " [" + listing.getState() + "]" : "";
        return typeTag + stateTag + " [" + listing.getUuid() + "]: " + listing.getName()
                + "\n" + listing.getDescription()
                + "\n" + priceLine;
    }

    /**
     * Adds coins to the current user's balance.
     *
     * @param arguments raw argument text containing the amount.
     * @throws AppException if the amount is missing, not a whole number, or not positive.
     */
    private void topUp(String arguments) throws AppException {
        long amount = parsePositiveLong(arguments, "top-up amount");
        try {
            currentUser.addBalance(amount);
        } catch (ArithmeticException e) {
            throw new AppException("The top-up would exceed the maximum supported balance.");
        }
        ui.showMessage("Topped up " + amount + " coins. Balance: " + currentUser.getBalance() + " coins.");
    }

    /**
     * Deducts coins from the current user's balance.
     *
     * @param arguments raw argument text containing the amount.
     * @throws AppException if the amount is missing, not a whole number, not positive,
     *         or exceeds the current balance.
     */
    private void withdraw(String arguments) throws AppException {
        long amount = parsePositiveLong(arguments, "withdrawal amount");
        if (!currentUser.deductBalance(amount)) {
            throw new AppException("Insufficient balance. You have " + currentUser.getBalance() + " coins.");
        }
        ui.showMessage("Withdrew " + amount + " coins. Balance: " + currentUser.getBalance() + " coins.");
    }

    /**
     * Displays the current user's coin balance.
     */
    private void showBalance() {
        ui.showMessage("Balance: " + currentUser.getBalance() + " coins.");
    }

    /**
     * Parses a positive 64-bit integer from a raw argument string.
     *
     * @param text raw text to parse.
     * @param fieldName human-readable field name used in error messages.
     * @return the parsed value.
     * @throws AppException if the text is blank, not an integer, or not positive.
     */
    private long parsePositiveLong(String text, String fieldName) throws AppException {
        if (text.isBlank()) {
            throw new AppException("Please provide a " + fieldName + ".");
        }
        long value;
        try {
            value = Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new AppException("The " + fieldName + " must be a whole number.");
        }
        if (value <= 0) {
            throw new AppException("The " + fieldName + " must be positive.");
        }
        return value;
    }

    /**
     * Verifies that a user can receive a credit without overflowing their balance.
     *
     * @param user user receiving the credit, or {@code null} if the account is unavailable.
     * @param amount positive amount to credit.
     * @param errorMessage message shown when the credit cannot be represented.
     * @throws AppException if crediting the user would exceed the supported balance.
     */
    private void ensureCanCredit(User user, long amount, String errorMessage) throws AppException {
        if (user == null) {
            return;
        }
        try {
            Math.addExact(user.getBalance(), amount);
        } catch (ArithmeticException e) {
            throw new AppException(errorMessage);
        }
    }

    /**
     * Grants seller status to the current user, or informs them if already a seller.
     */
    private void becomeSeller() {
        if (currentUser.isSeller()) {
            ui.showMessage("You are already a seller.");
            return;
        }
        currentUser.setSeller(true);
        ui.showMessage("Seller status granted. You may now list items for sale.");
    }
}
