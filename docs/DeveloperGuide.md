# Ekko Developer Guide

## Table of contents

1. [Acknowledgements](#acknowledgements)
2. [Setting up the project](#setting-up-the-project)
3. [Architecture](#architecture)
4. [Component design](#component-design)
5. [Key workflows](#key-workflows)
6. [Design decisions](#design-decisions)
7. [Persistence](#persistence)
8. [Testing](#testing)

## Acknowledgements

| Resource | Use |
| --- | --- |
| [NUS CS3227 / CS2103T Duke template](https://github.com/nus-cs2103-AY2627-S1/ip) | Initial project and Gradle scaffold |
| [SE-EDU AddressBook Level 3](https://github.com/se-edu/addressbook-level3) | Checkstyle configuration |
| [SE-EDU Java coding standard](https://se-education.org/guides/conventions/java/intermediate.html) | Project coding conventions |
| [OpenJFX 25.0.2](https://openjfx.io/) | Desktop user interface |
| [JUnit 5.14.4](https://junit.org/junit5/) | Automated tests |
| [Gradle Shadow 9.5.1](https://gradleup.com/shadow/) | Executable fat-JAR packaging |

## Setting up the project

**Prerequisites:** JDK 25 and a current IntelliJ IDEA version.

1. Clone or download the repository.
2. Open the project root in IntelliJ IDEA.
3. Set both the project SDK and Gradle JVM to **JDK 25**.
4. Reload the Gradle project.
5. Run `ekko.Launcher`, or use the Gradle `run` task.

On Windows:

```powershell
.\gradlew.bat run
.\gradlew.bat test
.\gradlew.bat check
```

On macOS/Linux, replace `.\gradlew.bat` with `./gradlew`. On macOS, select the required JDK with
`sdk use java 25.0.3.fx-zulu` when applicable.

Build and run the executable JAR with:

```powershell
.\gradlew.bat shadowJar
java -jar build/libs/ekko.jar
```

`check` runs JUnit plus Checkstyle for production and test sources. JavaFX GUI tests require a desktop
display; use Xvfb in a headless Linux environment.

## Architecture

Ekko is a JavaFX application with a UI/application coordinator, domain stores, and generic persistence:

```text
ekko.Launcher
    |
    v
ekko.Main ------------------------------------------------+
    | login/session UI                                    |
    | auction polling and persistence orchestration       |
    v                                                     v
ekko.Marketplace --> ekko.parser.*                 ekko.conversation.*
    |                  command parsing                    |
    +--> ekko.users.*                                     |
    +--> ekko.listing.* <--- ekko.listing.AuctionProcessor
    |                                                     |
    +---------------- ekko.storage.Storage<T> ------------+
                              |
                    ekko.storage.PersistenceCodec
```

`Main` owns the live stores and the JavaFX lifecycle. `Marketplace` contains synchronous command behavior
for one authenticated user. Domain classes do not depend on JavaFX, allowing most behavior to be tested
without a display.

## Component design

### `ekko` application layer

**`Launcher`** is the fat-JAR entry point. It delegates to `Main` so JavaFX can be loaded from the classpath.

**`Main`** builds the sign-in, registration, and marketplace scenes. It loads all persisted state, restores
per-user conversations, delays command replies by 750 ms, saves state after commands, and polls for expired
auctions every second. It also records notifications for signed-in and signed-out users.

**`Marketplace`** parses and dispatches commands for one `currentUser`. It enforces seller permissions,
listing ownership, balances, purchase rules, bidding rules, and search filters. Output is routed through
the injected `Ui`; notifications are routed through an injected `BiConsumer<String, String>`.

**`AppException`** represents recoverable user-command errors. File-system failures remain `IOException`,
while invalid persisted structures generally use `IllegalArgumentException`.

### `ekko.parser`

**`Command`** defines the 14 marketplace commands and stores the usage and description shown by the GUI's
command reference. `Parser` separates the first token from the remaining arguments and rejects blank or
unknown commands.

**`ArgumentParser`** extracts only the slash arguments allowed by a command. Unknown slash-prefixed text
remains part of the description, and duplicate recognised arguments are rejected. `ParsedArguments` stores
the free-text description and named values. `ArgumentName` centralises the supported argument names.

### `ekko.users`

**`User`** contains an immutable UUID and username plus a SHA-256 password digest, seller flag, and coin
balance. New users begin as buyers with zero coins. Balance methods reject invalid values and detect overflow.

**`UserStore`** indexes users by their case-sensitive username. It owns uniqueness checks, authentication,
lookup, registration rollback, and immutable snapshots for persistence.

### `ekko.listing`

**`Listing`** is the common base for an immutable UUID, owner, name, and description, plus mutable
`ListingState`. `ACTIVE`, `SOLD`, and `INACTIVE` distinguish listings available for transactions from their
completed history.

**`BinListing`** adds a fixed price and an optional buyer username. **`AuctionListing`** adds a starting
price, future end date-time, and optional highest `Bid`. Its effective listing price is the highest bid when
present, otherwise the starting price.

**`ListingStore`** indexes listings by their short hexadecimal UUID. It provides active/type filters,
owner and purchaser queries, expired-auction lookup, and case-sensitive name/description search with
inclusive price bounds. Hash-map iteration means result order is not guaranteed.

**`AuctionProcessor`** resolves expired auctions independently of JavaFX. Auctions with bids become sold
and credit their seller; auctions without bids become inactive. The returned username-to-messages map lets
`Main` persist and display notifications.

### `ekko.conversation`

**`ConversationMessage`** is one speaker/text pair. **`ConversationStore`** keeps ordered messages keyed by
user UUID. **`ConversationStorage`** maps each user to a separate UUID-named file and isolates recovery: a
damaged conversation becomes a single explanatory message without preventing the application from starting.

### `ekko.storage`

**`Storage<T>`** separates file I/O from domain serialization. `load` returns an empty `Optional` for a
missing file. `save` writes UTF-8 to a sibling temporary file, then replaces the destination atomically where
supported and falls back to a regular replacement when atomic moves are unavailable.

**`PersistenceCodec`** defines versioned, line-oriented representations for users, listings, and
conversations. Free text is URL-safe Base64 encoded so tabs, line breaks, and Unicode round-trip safely.

### `ekko.datetime` and `ekko.ui`

**`DateTimeParser`** accepts strict ISO or day-first dates with optional 24-hour times and formats output
using an English locale. Marketplace auctions require a future value that includes a time.

**`Ui`** provides the console-oriented message API and injectable streams. **`GuiUi`** extends it by routing
messages and recovery prompts through callbacks, keeping command logic independent of JavaFX controls.

## Key workflows

### Account registration and sign-in

Registration validates a non-blank unique username, a non-blank password, and matching confirmation. `Main`
first creates an empty conversation file, adds the user, and saves `users.txt`; if persistence fails, it
removes the in-memory user. Authentication performs a username lookup and password-hash comparison.

### Fixed-price purchase

`Marketplace` verifies that the UUID identifies an active BIN listing owned by another user. It checks that
the seller's balance can receive the payment before deducting the buyer. It then credits the seller, marks
the listing sold, records the buyer, and emits a seller notification. `Main` saves the shared stores after
the command.

### Auction bidding and resolution

A bid must meet the starting price or exceed the existing highest bid. Its coins are reserved immediately.
When a new highest bid is accepted, the previous bidder is refunded and notified before the bid is replaced.
Deleting an unexpired auction similarly refunds its highest bidder.

`Main` invokes `AuctionProcessor` at startup and once per second. At expiry, an auction with bids transfers
the already-reserved winning amount to the seller and becomes sold; one without bids becomes inactive.
Notifications are appended to each recipient's history and shown immediately when that user is signed in.

## Design decisions

**Injected command dependencies.** `Marketplace` receives its UI, current user, stores, and notification
handler. Tests can therefore exercise complete commands without launching JavaFX or touching production data.

**Immediate bid reservation.** Deducting a bid when placed prevents users from committing the same coins to
multiple purchases. Refunds on outbid or deletion keep balances consistent.

**Per-user conversation files.** A corrupted history affects one account only, and notifications can be
persisted for users who are signed out.

**Versioned codecs and encoded free text.** A leading format version permits future migration, while Base64
keeps the line-oriented files unambiguous without restricting ordinary user text.

**Separate launcher.** A non-`Application` entry point avoids JavaFX classpath launch detection problems in
the bundled JAR.

## Persistence

Paths are relative to the process working directory:

| Path | Contents |
| --- | --- |
| `data/users.txt` | User UUID, encoded username, password digest, seller status, and balance |
| `data/listings.txt` | BIN and auction subtype fields, state, buyer or highest bid |
| `data/conversations/<uuid>.txt` | Ordered encoded speaker/text pairs for one user |

Every file starts with storage version `1`. Missing shared files produce empty stores. Invalid shared files
stop startup without modification; invalid conversation files are replaced through the isolated recovery
path. Shared user and listing stores are saved after each submitted command and after auction resolution.

## Testing

Tests are under `src/test/java` in matching packages and use the `<ClassName>Test` naming convention.

| Area | Principal coverage |
| --- | --- |
| `MarketplaceTest` | Commands, permissions, balances, buying, bidding, deletion, and search |
| `UserTest`, `UserStoreTest` | Password hashing/authentication, user validation, and balances |
| Listing tests | BIN/auction validation, stores, bid rules, and auction resolution |
| Parser tests | Command recognition, slash arguments, duplicates, and invalid input |
| `PersistenceCodecTest`, `StorageTest` | Round trips, malformed data, missing files, and replacement behavior |
| Conversation tests | Per-user ordering, persistence, and corruption recovery |
| `DateTimeParserTest` | Strict parsing and stable display formatting |

After any code update, review affected high-value behavior and add normal, boundary, invalid-input, and
regression tests where appropriate. Run the authoritative complete suite with Java 25:

```powershell
.\gradlew.bat test
```

Run `.\gradlew.bat check` before delivery to include both JUnit and Checkstyle.
