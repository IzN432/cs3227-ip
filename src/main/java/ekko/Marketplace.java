package ekko;

import ekko.listing.ListingStore;
import ekko.parser.Command;
import ekko.parser.Parser;
import ekko.ui.Ui;
import ekko.users.User;
import ekko.users.UserStore;

/**
 * Coordinates marketplace commands for a logged-in user.
 *
 * <p>Storage is not yet wired; all state is in-memory and resets on each launch.
 */
public class Marketplace {

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
            case BALANCE -> showBalance();
            case BECOMESELLER -> becomeSeller();
            case TOPUP -> topUp(arguments);
            case BYE -> {
                return true;
            }
            default -> throw new AppException("This command is not yet implemented.");
        }
        return false;
    }

    /**
     * Adds coins to the current user's balance.
     *
     * @param arguments raw argument text containing the amount.
     * @throws AppException if the amount is missing, not a whole number, or not positive.
     */
    private void topUp(String arguments) throws AppException {
        int amount = parsePositiveInt(arguments, "top-up amount");
        currentUser.addBalance(amount);
        ui.showMessage("Topped up " + amount + " coins. Balance: " + currentUser.getBalance() + " coins.");
    }

    /**
     * Displays the current user's coin balance.
     */
    private void showBalance() {
        ui.showMessage("Balance: " + currentUser.getBalance() + " coins.");
    }

    /**
     * Parses a positive integer from a raw argument string.
     *
     * @param text raw text to parse.
     * @param fieldName human-readable field name used in error messages.
     * @return the parsed value.
     * @throws AppException if the text is blank, not an integer, or not positive.
     */
    private int parsePositiveInt(String text, String fieldName) throws AppException {
        if (text.isBlank()) {
            throw new AppException("Please provide a " + fieldName + ".");
        }
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new AppException("The " + fieldName + " must be a whole number.");
        }
        if (value <= 0) {
            throw new AppException("The " + fieldName + " must be positive.");
        }
        return value;
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
