package ekko;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        marketplace = new Marketplace(
                captureUi(),
                user,
                new UserStore(List.of(user)),
                new ListingStore(List.of())
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
        assertFalse(marketplace.processCommand("list"));
        assertFalse(errors.isEmpty());
    }
}
