package ekko.storage;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import ekko.conversation.ConversationMessage;
import ekko.listing.AuctionListing;
import ekko.listing.Bid;
import ekko.listing.BinListing;
import ekko.listing.Listing;
import ekko.listing.ListingState;
import ekko.listing.ListingStore;
import ekko.users.User;
import ekko.users.UserStore;

/**
 * Serializes marketplace domain stores into versioned, line-oriented text formats.
 *
 * <p>Free-text fields are Base64 encoded so tabs, line breaks, and Unicode text round-trip safely.
 */
public final class PersistenceCodec {

    private static final String VERSION = "1";

    private PersistenceCodec() {
    }

    /**
     * Serializes all registered users.
     */
    public static String serializeUsers(UserStore store) {
        StringBuilder result = new StringBuilder(VERSION);
        for (User user : store.asCollection()) {
            result.append('\n').append(String.join("\t",
                    user.getUuid(),
                    encode(user.getUsername()),
                    user.getHashedPassword(),
                    Boolean.toString(user.isSeller()),
                    Long.toString(user.getBalance())));
        }
        return result.toString();
    }

    /**
     * Deserializes a complete user store.
     */
    public static UserStore deserializeUsers(String text) {
        List<String> lines = dataLines(text);
        List<User> users = new ArrayList<>();
        for (String line : lines) {
            String[] fields = fields(line, 5);
            users.add(User.fromPersisted(fields[0], decode(fields[1]), fields[2], parseBoolean(fields[3]),
                    parseNonNegativeLong(fields[4])));
        }
        return new UserStore(users);
    }

    /**
     * Serializes all marketplace listings.
     */
    public static String serializeListings(ListingStore store) {
        StringBuilder result = new StringBuilder(VERSION);
        for (Listing listing : store.asCollection()) {
            result.append('\n');
            if (listing instanceof BinListing bin) {
                result.append(String.join("\t", "BIN", encode(bin.getUuid()),
                        encode(bin.getOwnerUsername()), encode(bin.getName()), encode(bin.getDescription()),
                        Long.toString(bin.getPrice()), bin.getState().name(), encodeNullable(bin.getBuyerUsername())));
            } else {
                AuctionListing auction = (AuctionListing) listing;
                Bid bid = auction.getHighestBid();
                result.append(String.join("\t", "AUC", encode(auction.getUuid()),
                        encode(auction.getOwnerUsername()), encode(auction.getName()),
                        encode(auction.getDescription()), Long.toString(auction.getBasePrice()),
                        auction.getEndDateTime().toString(), auction.getState().name(),
                        encodeNullable(bid == null ? null : bid.getBidderUsername()),
                        bid == null ? "" : Long.toString(bid.getAmount())));
            }
        }
        return result.toString();
    }

    /**
     * Deserializes a complete listing store.
     */
    public static ListingStore deserializeListings(String text) {
        List<Listing> listings = new ArrayList<>();
        for (String line : dataLines(text)) {
            String[] rawFields = line.split("\t", -1);
            if (rawFields.length == 8 && rawFields[0].equals("BIN")) {
                BinListing bin = new BinListing(decode(rawFields[1]), decode(rawFields[2]),
                        decode(rawFields[3]), decode(rawFields[4]), parsePositiveLong(rawFields[5]));
                bin.setState(ListingState.valueOf(rawFields[6]));
                String buyer = decodeNullable(rawFields[7]);
                if (buyer != null) {
                    bin.setBuyerUsername(buyer);
                }
                listings.add(bin);
            } else if (rawFields.length == 10 && rawFields[0].equals("AUC")) {
                AuctionListing auction = new AuctionListing(decode(rawFields[1]), decode(rawFields[2]),
                        decode(rawFields[3]), decode(rawFields[4]), parsePositiveLong(rawFields[5]),
                        LocalDateTime.parse(rawFields[6]));
                auction.setState(ListingState.valueOf(rawFields[7]));
                String bidder = decodeNullable(rawFields[8]);
                if (bidder != null) {
                    auction.setHighestBid(new Bid(bidder, parsePositiveLong(rawFields[9])));
                }
                listings.add(auction);
            } else {
                throw new IllegalArgumentException("Stored listing record is invalid.");
            }
        }
        return new ListingStore(listings);
    }

    /**
     * Serializes one user's ordered conversation messages.
     */
    public static String serializeConversation(List<ConversationMessage> messages) {
        StringBuilder result = new StringBuilder(VERSION);
        for (ConversationMessage message : messages) {
            result.append('\n').append(encode(message.speaker())).append('\t').append(encode(message.text()));
        }
        return result.toString();
    }

    /**
     * Deserializes one user's ordered conversation messages.
     */
    public static List<ConversationMessage> deserializeConversation(String text) {
        List<ConversationMessage> messages = new ArrayList<>();
        for (String line : dataLines(text)) {
            String[] values = fields(line, 2);
            messages.add(new ConversationMessage(decode(values[0]), decode(values[1])));
        }
        return messages;
    }

    private static List<String> dataLines(String text) {
        String[] lines = text.split("\\R", -1);
        if (lines.length == 0 || !lines[0].equals(VERSION)) {
            throw new IllegalArgumentException("Unsupported storage format version.");
        }
        List<String> data = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                data.add(lines[i]);
            }
        }
        return data;
    }

    private static String[] fields(String line, int expectedCount) {
        String[] values = line.split("\t", -1);
        if (values.length != expectedCount) {
            throw new IllegalArgumentException("Stored record has the wrong number of fields.");
        }
        return values;
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String encodeNullable(String value) {
        return value == null ? "" : encode(value);
    }

    private static String decode(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Stored text field is invalid.", e);
        }
    }

    private static String decodeNullable(String value) {
        return value.isEmpty() ? null : decode(value);
    }

    private static boolean parseBoolean(String value) {
        if (!value.equals("true") && !value.equals("false")) {
            throw new IllegalArgumentException("Stored boolean is invalid.");
        }
        return Boolean.parseBoolean(value);
    }

    private static long parseNonNegativeLong(String value) {
        long parsed = Long.parseLong(value);
        if (parsed < 0) {
            throw new IllegalArgumentException("Stored number cannot be negative.");
        }
        return parsed;
    }

    private static long parsePositiveLong(String value) {
        long parsed = Long.parseLong(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException("Stored number must be positive.");
        }
        return parsed;
    }
}
