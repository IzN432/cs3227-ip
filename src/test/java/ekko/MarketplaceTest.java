package ekko;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ekko.listing.AuctionListing;
import ekko.listing.BinListing;
import ekko.listing.Listing;
import ekko.listing.ListingState;
import ekko.listing.ListingStore;
import ekko.users.User;
import ekko.users.UserStore;

/**
 * Tests marketplace command dispatch, state changes, and error handling.
 *
 * <p>Each test uses a fresh user and message-capturing UI so assertions
 * target the exact output without touching the JavaFX layer.
 */
class MarketplaceTest {

    private User user;
    private List<String> messages;
    private List<String> errors;
    private Marketplace marketplace;
    private ListingStore defaultListingStore;

    /**
     * Captures output to in-memory lists instead of displaying it.
     */
    private ekko.ui.Ui captureUi() {
        return new ekko.ui.GuiUi(
                messages::add,
                errors::add,
                () -> "yes"
        );
    }

    @BeforeEach
    void setUp() {
        user = new User("user", "password");
        messages = new ArrayList<>();
        errors = new ArrayList<>();
        defaultListingStore = new ListingStore(List.of());
        marketplace = new Marketplace(
                captureUi(),
                user,
                new UserStore(List.of(user)),
                defaultListingStore
        );
    }

    // --- bye ---

    @Test
    void processCommand_bye_returnsTrue() {
        assertTrue(marketplace.processCommand("bye"));
    }

    @Test
    void processCommand_byeWithLeadingWhitespace_returnsTrue() {
        assertTrue(marketplace.processCommand("  bye  "));
    }

    // --- balance ---

    @Test
    void processCommand_balance_showsZeroInitially() {
        marketplace.processCommand("balance");
        assertEquals("Balance: 0 coins.", messages.get(0));
    }

    // --- topup ---

    @Test
    void processCommand_topup_addsAmountAndConfirms() {
        marketplace.processCommand("topup 100");
        assertEquals("Topped up 100 coins. Balance: 100 coins.", messages.get(0));
        assertEquals(100, user.getBalance());
    }

    @Test
    void processCommand_topupTwice_accumulatesBalance() {
        marketplace.processCommand("topup 50");
        marketplace.processCommand("topup 75");
        assertEquals(125, user.getBalance());
        assertEquals("Topped up 75 coins. Balance: 125 coins.", messages.get(1));
    }

    @Test
    void processCommand_topupThenBalance_showsUpdatedBalance() {
        marketplace.processCommand("topup 200");
        marketplace.processCommand("balance");
        assertEquals("Balance: 200 coins.", messages.get(1));
    }

    @Test
    void processCommand_topupMissingAmount_showsError() {
        marketplace.processCommand("topup");
        assertEquals("Please provide a top-up amount.", errors.get(0));
        assertEquals(0, user.getBalance());
    }

    @Test
    void processCommand_topupNonInteger_showsError() {
        for (String input : new String[] {"topup abc", "topup 1.5", "topup 10coins"}) {
            errors.clear();
            marketplace.processCommand(input);
            assertEquals("The top-up amount must be a whole number.", errors.get(0), input);
        }
        assertEquals(0, user.getBalance());
    }

    @Test
    void processCommand_topupZeroOrNegative_showsError() {
        for (String input : new String[] {"topup 0", "topup -50"}) {
            errors.clear();
            marketplace.processCommand(input);
            assertEquals("The top-up amount must be positive.", errors.get(0), input);
        }
        assertEquals(0, user.getBalance());
    }

    // --- withdraw ---

    @Test
    void processCommand_withdraw_deductsAmountAndConfirms() {
        marketplace.processCommand("topup 200");
        marketplace.processCommand("withdraw 75");
        assertEquals(125, user.getBalance());
        assertEquals("Withdrew 75 coins. Balance: 125 coins.", messages.get(1));
    }

    @Test
    void processCommand_withdrawExactBalance_leavesZero() {
        marketplace.processCommand("topup 100");
        marketplace.processCommand("withdraw 100");
        assertEquals(0, user.getBalance());
        assertEquals("Withdrew 100 coins. Balance: 0 coins.", messages.get(1));
    }

    @Test
    void processCommand_withdrawInsufficientFunds_showsError() {
        marketplace.processCommand("topup 50");
        marketplace.processCommand("withdraw 51");
        assertEquals(50, user.getBalance());
        assertEquals("Insufficient balance. You have 50 coins.", errors.get(0));
    }

    @Test
    void processCommand_withdrawMissingAmount_showsError() {
        marketplace.processCommand("withdraw");
        assertEquals("Please provide a withdrawal amount.", errors.get(0));
    }

    @Test
    void processCommand_withdrawNonInteger_showsError() {
        marketplace.processCommand("withdraw abc");
        assertEquals("The withdrawal amount must be a whole number.", errors.get(0));
    }

    @Test
    void processCommand_withdrawZeroOrNegative_showsError() {
        for (String input : new String[] {"withdraw 0", "withdraw -10"}) {
            errors.clear();
            marketplace.processCommand(input);
            assertEquals("The withdrawal amount must be positive.", errors.get(0), input);
        }
    }

    // --- list ---

    @Test
    void processCommand_list_emptyStore_showsEmptyMessage() {
        marketplace.processCommand("list");
        assertEquals("No active listings.", messages.get(0));
    }

    @Test
    void processCommand_list_withListings_showsCountAndDetails() {
        LocalDateTime future = LocalDateTime.now().plusHours(1);
        BinListing bin = new BinListing("b001", "user", "Lamp", "A nice lamp", 100);
        AuctionListing auction = new AuctionListing("a001", "seller2", "Watch", "A nice watch", 50, future);
        Marketplace mp = marketplaceWith(List.of(bin, auction));

        mp.processCommand("list");

        String output = messages.get(0);
        assertTrue(output.startsWith("Active listings (2):"), output);
        assertTrue(output.contains("b001"), output);
        assertTrue(output.contains("Lamp"), output);
        assertTrue(output.contains("[BIN]"), output);
        assertTrue(output.contains("100 coins"), output);
        assertTrue(output.contains("a001"), output);
        assertTrue(output.contains("Watch"), output);
        assertTrue(output.contains("[AUC]"), output);
    }

    @Test
    void processCommand_list_inactiveListingsExcluded() {
        BinListing sold = new BinListing("b001", "user", "Lamp", "desc", 100);
        sold.setState(ListingState.SOLD);
        Marketplace mp = marketplaceWith(List.of(sold));

        mp.processCommand("list");

        assertEquals("No active listings.", messages.get(0));
    }

    // --- mylistings ---

    @Test
    void processCommand_myListings_noListings_showsEmptyMessage() {
        marketplace.processCommand("mylistings");
        assertEquals("You have no listings.", messages.get(0));
    }

    @Test
    void processCommand_myListings_showsOwnListingsOnly() {
        BinListing mine = new BinListing("b001", "user", "Lamp", "desc", 100);
        BinListing theirs = new BinListing("b002", "other", "Table", "desc", 80);
        Marketplace mp = marketplaceWith(List.of(mine, theirs));

        mp.processCommand("mylistings");

        String output = messages.get(0);
        assertTrue(output.contains("b001"), output);
        assertFalse(output.contains("b002"), output);
    }

    @Test
    void processCommand_myListings_includesInactiveAndSold() {
        BinListing active = new BinListing("b001", "user", "Lamp", "desc", 100);
        BinListing sold = new BinListing("b002", "user", "Chair", "desc", 60);
        sold.setState(ListingState.SOLD);
        Marketplace mp = marketplaceWith(List.of(active, sold));

        mp.processCommand("mylistings");

        String output = messages.get(0);
        assertTrue(output.contains("Your listings (2):"), output);
        assertTrue(output.contains("[ACTIVE]"), output);
        assertTrue(output.contains("[SOLD]"), output);
    }

    /**
     * Creates a fresh marketplace wired to the current user and the given listings.
     */
    private Marketplace marketplaceWith(List<Listing> listings) {
        messages.clear();
        errors.clear();
        return new Marketplace(
                captureUi(),
                user,
                new UserStore(List.of(user)),
                new ListingStore(listings)
        );
    }

    // --- bin ---

    @Test
    void processCommand_bin_createsBinListingAndConfirms() {
        marketplace.processCommand("becomeseller");
        marketplace.processCommand("bin Lamp /desc A nice lamp /price 100");
        String output = messages.get(1);
        assertTrue(output.startsWith("BIN listing created ["), output);
        assertTrue(output.contains("Lamp"), output);
        assertTrue(output.contains("100 coins"), output);
        assertEquals(1, listingStore().getActiveBin().size());
    }

    @Test
    void processCommand_bin_listingAppearsInList() {
        marketplace.processCommand("becomeseller");
        marketplace.processCommand("bin Lamp /desc A nice lamp /price 100");
        messages.clear();
        marketplace.processCommand("list");
        assertTrue(messages.get(0).contains("Lamp"), messages.get(0));
    }

    @Test
    void processCommand_bin_notSeller_showsError() {
        marketplace.processCommand("bin Lamp /desc A nice lamp /price 100");
        assertTrue(errors.get(0).contains("becomeseller"), errors.get(0));
        assertTrue(listingStore().isEmpty());
    }

    @Test
    void processCommand_bin_missingName_showsError() {
        marketplace.processCommand("becomeseller");
        marketplace.processCommand("bin /desc A lamp /price 100");
        assertFalse(errors.isEmpty());
        assertTrue(listingStore().isEmpty());
    }

    @Test
    void processCommand_bin_missingDesc_mentionsSlashDesc() {
        marketplace.processCommand("becomeseller");
        marketplace.processCommand("bin Lamp /price 100");
        assertTrue(errors.get(0).contains("/desc"), errors.get(0));
        assertTrue(listingStore().isEmpty());
    }

    @Test
    void processCommand_bin_missingPrice_mentionsSlashPrice() {
        marketplace.processCommand("becomeseller");
        marketplace.processCommand("bin Lamp /desc A lamp");
        assertTrue(errors.get(0).contains("/price"), errors.get(0));
        assertTrue(listingStore().isEmpty());
    }

    @Test
    void processCommand_bin_invalidPrice_showsError() {
        marketplace.processCommand("becomeseller");
        marketplace.processCommand("bin Lamp /desc A lamp /price free");
        assertFalse(errors.isEmpty());
        assertTrue(listingStore().isEmpty());
    }

    // --- auction ---

    @Test
    void processCommand_auction_createsAuctionListingAndConfirms() {
        marketplace.processCommand("becomeseller");
        marketplace.processCommand("auction Watch /desc A nice watch /price 50 /end 2099-01-01 1200");
        String output = messages.get(1);
        assertTrue(output.startsWith("Auction listing created ["), output);
        assertTrue(output.contains("Watch"), output);
        assertTrue(output.contains("50 coins"), output);
        assertEquals(1, listingStore().getActiveAuctions().size());
    }

    @Test
    void processCommand_auction_notSeller_showsError() {
        marketplace.processCommand("auction Watch /desc desc /price 50 /end 2099-01-01 1200");
        assertFalse(errors.isEmpty());
    }

    @Test
    void processCommand_auction_missingEnd_mentionsSlashEnd() {
        marketplace.processCommand("becomeseller");
        marketplace.processCommand("auction Watch /desc desc /price 50");
        assertTrue(errors.get(0).contains("/end"), errors.get(0));
        assertTrue(listingStore().isEmpty());
    }

    @Test
    void processCommand_auction_invalidEndFormat_showsError() {
        marketplace.processCommand("becomeseller");
        marketplace.processCommand("auction Watch /desc desc /price 50 /end notadate");
        assertFalse(errors.isEmpty());
        assertTrue(listingStore().isEmpty());
    }

    @Test
    void processCommand_auction_pastEndTime_showsError() {
        marketplace.processCommand("becomeseller");
        marketplace.processCommand("auction Watch /desc desc /price 50 /end 2000-01-01 1200");
        assertTrue(errors.get(0).contains("future"), errors.get(0));
        assertTrue(listingStore().isEmpty());
    }

    /** Returns the ListingStore used by the default marketplace instance. */
    private ListingStore listingStore() {
        return defaultListingStore;
    }

    // --- becomeseller ---

    @Test
    void processCommand_becomeSeller_grantsStatusAndConfirms() {
        assertFalse(user.isSeller());
        marketplace.processCommand("becomeseller");
        assertTrue(user.isSeller());
        assertEquals("Seller status granted. You may now list items for sale.", messages.get(0));
    }

    @Test
    void processCommand_becomeSellerTwice_rejectsSecondRequest() {
        marketplace.processCommand("becomeseller");
        marketplace.processCommand("becomeseller");
        assertEquals("You are already a seller.", messages.get(1));
        assertTrue(user.isSeller());
    }

    // --- unknown / unimplemented ---

    @Test
    void processCommand_blankInput_showsError() {
        marketplace.processCommand("   ");
        assertFalse(errors.isEmpty());
    }

    @Test
    void processCommand_unknownCommand_showsError() {
        marketplace.processCommand("foobar");
        assertFalse(errors.isEmpty());
    }

    @Test
    void processCommand_unimplementedCommand_showsErrorAndReturnsFalse() {
        assertFalse(marketplace.processCommand("find keyword"));
        assertFalse(errors.isEmpty());
    }
}
