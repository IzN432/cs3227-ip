package ekko.parser;

import java.util.Locale;

/**
 * Defines the commands supported by the Ekko marketplace and their interface metadata.
 */
public enum Command {
    BIN("<name> /desc <description> /price <price>", "List an item for immediate purchase."),
    AUCTION("<name> /desc <description> /price <starting bid> /end <date/time>",
            "List an item for auction with a deadline."),
    BID("<uuid> /price <amount>", "Place a bid on an auction."),
    BUY("<uuid>", "Purchase a BIN listing immediately."),
    LIST("", "Browse all active listings."),
    FIND("<keyword> [/low <min>] [/high <max>]", "Search listings by name or description."),
    MYLISTINGS("", "View your own listings."),
    MYPURCHASES("", "View your completed BIN and auction purchases."),
    TOPUP("<amount>", "Add coins to your balance."),
    WITHDRAW("<amount>", "Withdraw coins from your balance."),
    BALANCE("", "Check your current coin balance."),
    BECOMESELLER("", "Apply for seller status."),
    BYE("", "End the session.");

    private final String usage;
    private final String description;

    Command(String usage, String description) {
        this.usage = usage;
        this.description = description;
    }

    /**
     * Returns the lowercase command word used for parsing and display.
     */
    public String getWord() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String getUsage() {
        return usage;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Converts a command word to its enum value.
     *
     * @param commandWord word entered by the user.
     * @return the matching command.
     * @throws IllegalArgumentException if the word is not a supported command.
     */
    public static Command from(String commandWord) {
        for (Command command : values()) {
            if (command.getWord().equals(commandWord)) {
                return command;
            }
        }
        throw new IllegalArgumentException("Unknown command: " + commandWord);
    }
}
