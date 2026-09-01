package ekko.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests generic text persistence independently of any particular domain store format.
 */
class StorageTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void load_missingFile_returnsEmptyOptional() throws IOException {
        Storage<String> storage = stringStorage(temporaryDirectory.resolve("missing.txt"));

        assertFalse(storage.load().isPresent());
    }

    @Test
    void saveAndLoad_suppliedFunctions_roundTripsData() throws IOException {
        Path filePath = temporaryDirectory.resolve("nested/users.txt");
        Storage<String> storage = stringStorage(filePath);

        storage.save("Alice");

        assertEquals("alice", Files.readString(filePath));
        assertEquals("ALICE", storage.load().orElseThrow());
    }

    @Test
    void save_existingFile_replacesItsContents() throws IOException {
        Storage<String> storage = stringStorage(temporaryDirectory.resolve("listings.txt"));
        storage.save("first");

        storage.save("second");

        assertEquals("SECOND", storage.load().orElseThrow());
    }

    @Test
    void load_invalidData_propagatesDeserializerException() throws IOException {
        Path filePath = temporaryDirectory.resolve("users.txt");
        Files.writeString(filePath, "invalid");
        Storage<Integer> storage = new Storage<>(filePath, Object::toString, Integer::parseInt);

        assertThrows(IllegalArgumentException.class, storage::load);
    }

    @Test
    void save_nullData_throwsException() {
        Storage<String> storage = stringStorage(temporaryDirectory.resolve("users.txt"));

        assertThrows(NullPointerException.class, () -> storage.save(null));
    }

    @Test
    void save_serializerReturnsNull_throwsException() {
        Storage<String> storage = new Storage<>(temporaryDirectory.resolve("users.txt"), value -> null,
                serialized -> serialized);

        assertThrows(IllegalStateException.class, () -> storage.save("alice"));
    }

    /**
     * Creates storage whose transformations make serializer and deserializer use observable behavior.
     */
    private Storage<String> stringStorage(Path filePath) {
        return new Storage<>(filePath, value -> value.toLowerCase(Locale.ROOT),
                serialized -> serialized.toUpperCase(Locale.ROOT));
    }
}
