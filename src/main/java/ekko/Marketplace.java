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
            case BECOMESELLER -> becomeSeller();
            case BYE -> {
                return true;
            }
            default -> throw new AppException("This command is not yet implemented.");
        }
        return false;
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
