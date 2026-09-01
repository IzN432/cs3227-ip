package ekko.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ekko.conversation.ConversationMessage;
import ekko.listing.AuctionListing;
import ekko.listing.Bid;
import ekko.listing.BinListing;
import ekko.listing.ListingState;
import ekko.listing.ListingStore;
import ekko.users.User;
import ekko.users.UserStore;

/**
 * Tests complete persistence round trips for each stored domain type.
 */
class PersistenceCodecTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void users_roundTrip_preservesIdentityCredentialsAndMutableState() {
        User alice = new User("alice", "secret");
        alice.setSeller(true);
        alice.addBalance(125);

        UserStore restored = PersistenceCodec.deserializeUsers(
                PersistenceCodec.serializeUsers(new UserStore(List.of(alice))));
        User restoredAlice = restored.get("alice");

        assertEquals(alice.getUuid(), restoredAlice.getUuid());
        assertTrue(restoredAlice.checkPassword("secret"));
        assertTrue(restoredAlice.isSeller());
        assertEquals(125, restoredAlice.getBalance());
    }

    @Test
    void listings_roundTrip_preservesSubtypeStateAndTransactionDetails() {
        BinListing bin = new BinListing("bin1", "alice", "Desk\nLamp", "Warm\tlight", 40);
        bin.setBuyerUsername("bob");
        bin.setState(ListingState.SOLD);
        AuctionListing auction = new AuctionListing("auc1", "alice", "Watch", "Old watch", 10,
                LocalDateTime.of(2030, 1, 2, 12, 30));
        auction.setHighestBid(new Bid("bob", 25));

        ListingStore restored = PersistenceCodec.deserializeListings(
                PersistenceCodec.serializeListings(new ListingStore(List.of(bin, auction))));
        BinListing restoredBin = (BinListing) restored.get("bin1");
        AuctionListing restoredAuction = (AuctionListing) restored.get("auc1");

        assertEquals("Desk\nLamp", restoredBin.getName());
        assertEquals("Warm\tlight", restoredBin.getDescription());
        assertEquals("bob", restoredBin.getBuyerUsername());
        assertFalse(restoredBin.isActive());
        assertEquals(25, restoredAuction.getHighestBid().getAmount());
        assertEquals(LocalDateTime.of(2030, 1, 2, 12, 30), restoredAuction.getEndDateTime());
    }

    @Test
    void conversation_roundTrip_preservesSpeakersUnicodeAndLineBreaks() {
        List<ConversationMessage> messages = List.of(
                new ConversationMessage("You", "find café"),
                new ConversationMessage("Ekko", "First line\nSecond line"));

        List<ConversationMessage> restored = PersistenceCodec.deserializeConversation(
                PersistenceCodec.serializeConversation(messages));

        assertEquals(messages, restored);
    }

    @Test
    void unsupportedVersion_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> PersistenceCodec.deserializeUsers("99"));
    }

    @Test
    void conversationStorage_userUuidFile_roundTripsHistory() throws IOException {
        User user = new User("alice", "secret");
        Path conversationFile = temporaryDirectory.resolve(user.getUuid() + ".txt");
        Storage<List<ConversationMessage>> storage = new Storage<>(conversationFile,
                PersistenceCodec::serializeConversation, PersistenceCodec::deserializeConversation);
        List<ConversationMessage> messages = List.of(new ConversationMessage("Ekko", "You won."));

        storage.save(messages);

        assertTrue(conversationFile.getFileName().toString().startsWith(user.getUuid()));
        assertEquals(messages, storage.load().orElseThrow());
    }

    @Test
    void delimiterInUserControlledFields_roundTripsWithoutSplittingRecords() {
        String delimiter = "\t";
        User user = new User("ali" + delimiter + "ce", "secret");
        UserStore restoredUsers = PersistenceCodec.deserializeUsers(
                PersistenceCodec.serializeUsers(new UserStore(List.of(user))));

        BinListing bin = new BinListing("bin" + delimiter + "1", "sell" + delimiter + "er",
                "Desk" + delimiter + "Lamp", "Warm" + delimiter + "light", 40);
        bin.setBuyerUsername("buy" + delimiter + "er");
        AuctionListing auction = new AuctionListing("auc" + delimiter + "1", "seller",
                "Watch", "Old watch", 10, LocalDateTime.of(2030, 1, 2, 12, 30));
        auction.setHighestBid(new Bid("bid" + delimiter + "der", 25));
        ListingStore restoredListings = PersistenceCodec.deserializeListings(
                PersistenceCodec.serializeListings(new ListingStore(List.of(bin, auction))));

        List<ConversationMessage> messages = List.of(
                new ConversationMessage("Ek" + delimiter + "ko", "First" + delimiter + "second"));
        List<ConversationMessage> restoredMessages = PersistenceCodec.deserializeConversation(
                PersistenceCodec.serializeConversation(messages));

        assertEquals(user.getUuid(), restoredUsers.get("ali" + delimiter + "ce").getUuid());
        assertEquals("sell" + delimiter + "er",
                restoredListings.get("bin" + delimiter + "1").getOwnerUsername());
        BinListing restoredBin = (BinListing) restoredListings.get("bin" + delimiter + "1");
        AuctionListing restoredAuction = (AuctionListing) restoredListings.get("auc" + delimiter + "1");
        assertEquals("buy" + delimiter + "er", restoredBin.getBuyerUsername());
        assertEquals("bid" + delimiter + "der", restoredAuction.getHighestBid().getBidderUsername());
        assertEquals(messages, restoredMessages);
    }
}
