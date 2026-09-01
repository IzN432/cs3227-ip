package ekko.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Persists one type of application data in a dedicated text file.
 *
 * <p>The caller supplies the data format through serializer and deserializer functions. This keeps file-system
 * concerns here while allowing each domain store to define its own representation.
 *
 * @param <T> type of data saved in the file.
 */
public class Storage<T> {

    private final Path filePath;
    private final Function<T, String> serializer;
    private final Function<String, T> deserializer;

    /**
     * Creates storage backed by the given file.
     *
     * @param filePath destination file for this type of data.
     * @param serializer converts data into its persisted text representation.
     * @param deserializer reconstructs data from its persisted text representation.
     * @throws NullPointerException if any argument is {@code null}.
     */
    public Storage(Path filePath, Function<T, String> serializer, Function<String, T> deserializer) {
        this.filePath = Objects.requireNonNull(filePath).toAbsolutePath().normalize();
        this.serializer = Objects.requireNonNull(serializer);
        this.deserializer = Objects.requireNonNull(deserializer);
    }

    /**
     * Loads the data stored in this instance's file.
     *
     * @return the deserialized data, or an empty optional if the file does not exist.
     * @throws IOException if the file cannot be read.
     * @throws IllegalArgumentException if the deserializer rejects the stored text.
     */
    public Optional<T> load() throws IOException {
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        String serializedData = Files.readString(filePath, StandardCharsets.UTF_8);
        T data = deserializer.apply(serializedData);
        if (data == null) {
            throw new IllegalArgumentException("Deserialized data cannot be null.");
        }
        return Optional.of(data);
    }

    /**
     * Saves the supplied data, replacing any data previously stored in the file.
     *
     * <p>The content is written to a temporary sibling file first. An atomic move is used when supported so an
     * interrupted write does not leave a partially updated destination file.
     *
     * @param data data to save.
     * @throws IOException if the data cannot be written.
     * @throws NullPointerException if {@code data} is {@code null}.
     * @throws IllegalStateException if the serializer returns {@code null}.
     */
    public void save(T data) throws IOException {
        Objects.requireNonNull(data);
        String serializedData = serializer.apply(data);
        if (serializedData == null) {
            throw new IllegalStateException("Serialized data cannot be null.");
        }

        Path parentDirectory = filePath.getParent();
        Files.createDirectories(parentDirectory);
        Path temporaryFile = Files.createTempFile(parentDirectory, filePath.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporaryFile, serializedData, StandardCharsets.UTF_8);
            replaceFile(temporaryFile);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Replaces the destination with a completed temporary file.
     */
    private void replaceFile(Path temporaryFile) throws IOException {
        try {
            Files.move(temporaryFile, filePath, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporaryFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
