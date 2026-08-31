# Ekko Developer Guide

## Table of Contents

1. [Acknowledgements](#acknowledgements)
2. [Setting up the project](#setting-up-the-project)
3. [Architecture](#architecture)
4. [Component design](#component-design)
   - [ekko (application layer)](#ekko-application-layer)
   - [ekko.parser](#ekkoparser)
   - [ekko.task](#ekkotask)
   - [ekko.storage](#ekkostorage)
   - [ekko.datetime](#ekkodatetime)
   - [ekko.ui](#ekkoui)
5. [Key design decisions](#key-design-decisions)
6. [Testing](#testing)

---

## Acknowledgements

| Resource | Use |
|---|---|
| [NUS CS3227 / CS2103T project template ("Duke")](https://github.com/nus-cs2103-AY2627-S1/ip) | Initial project scaffold, Gradle build, and `add-gradle-support` branch |
| [SE-EDU AddressBook Level 3](https://github.com/se-edu/addressbook-level3) | Checkstyle XML configuration (`config/checkstyle/checkstyle.xml`) |
| [SE-EDU Java coding standard](https://se-education.org/guides/conventions/java/intermediate.html) | Coding-style rules enforced via the `seedu-java-coding-standard` project skill |
| [SE-EDU Git conventions](https://se-education.org/guides/conventions/git.html) | Commit-message and branch-naming rules |
| [OpenJFX / JavaFX 25.0.2](https://openjfx.io/) | GUI toolkit for the desktop window |
| [JUnit 5.14.4](https://junit.org/junit5/) | Automated testing framework |
| [Gradle Shadow 9.5.1](https://gradleup.com/shadow/) | Fat-JAR packaging with bundled dependencies |
| [Checkstyle 11.0.0](https://checkstyle.sourceforge.io/) | Static style analysis integrated into the Gradle build |

---

## Setting up the project

**Prerequisites:** Java 25 and IntelliJ IDEA (or another IDE supporting Gradle projects).

1. Clone or download the repository.
2. Open IntelliJ IDEA → **File → Open** → select the project root.
3. Set the project SDK to **JDK 25** via **File → Project Structure → Project**.
4. Set Gradle JVM to the same SDK via **Settings → Build Tools → Gradle → Gradle JVM**.
5. Click **Reload All Gradle Projects** in the Gradle tool window.

**Running from the IDE:** In the Gradle tool window, run **Tasks → application → run**.

**Building the fat JAR:**
```
.\gradlew.bat shadowJar
```
The output is `build/libs/ekko.jar`. Run it with:
```
java -jar build/libs/ekko.jar
```

**Running tests and style checks:**
```
.\gradlew.bat check
```
This runs the full JUnit suite and both Checkstyle checks (production and test code).

---

## Architecture

Ekko has two entry points sharing the same core logic:

```
ekko.Launcher              ekko.Ekko.main(...)
     |                           |
     v                           v
ekko.Main (JavaFX UI)      ekko.Ui (console)
     \                          /
      \        ekko.Ekko       /     ← application coordinator
              /    |    \
    ekko.Parser  ekko.TaskList  ekko.Storage
         |             |              |
  ekko.ArgumentParser  ekko.task.*   ekko.DateTimeParser
```

**`ekko.Launcher`** exists solely to launch `ekko.Main` as a JavaFX `Application`. This is required when JavaFX is loaded from the classpath rather than the module path, because calling `Application.launch` from the same class that extends `Application` causes a classpath detection failure.

**`ekko.Ekko`** is the application coordinator. It is UI-agnostic: it receives a `Ui` and a `Storage` instance through its constructor, so both the GUI and console entry points reuse identical command-handling logic.

---

## Component design

### ekko (application layer)

**`Ekko`**

The central coordinator. Responsibilities:

- Loads tasks from storage on construction; handles corrupt data by offering to delete the file.
- Exposes `mainLoop()` for the console and `processCommand(String)` for the GUI.
- Delegates to `Parser` for command recognition, `ArgumentParser` for argument extraction, `DateTimeParser` for date/time parsing, `TaskList` for mutations, and `Storage` for persistence.
- Raises `EkkoException` for invalid user input and lets `IOException` propagate for file-system failures.

**`EkkoException`**

A checked exception reserved exclusively for user-command errors (unknown command, missing argument, out-of-range task number, etc.). It is never used for I/O failures or malformed persistence data—those use `IOException` and `IllegalArgumentException` respectively.

**`Main` (JavaFX)**

Builds the conversation window and manages the JavaFX lifecycle:

- Assembles the scene: a heading, a scrollable conversation area, a `TitledPane` command reference overlay, and a bottom composer with a `TextField` and Send button.
- On submission, disables input, shows a 750 ms `PauseTransition` ("PROCESSING COMMAND…"), then calls `Ekko.processCommand`.
- Appends right-aligned user bubbles and left-aligned Ekko replies; error replies receive an additional CSS class for red styling.
- Uses `GuiUi` as its `Ui` implementation, supplying lambdas for normal messages, error messages, and the recovery confirmation dialog.

---

### ekko.parser

**`Command` (enum)**

Defines all supported command words. Each constant carries:
- `word` — the lowercase command string recognised by the parser (derived from the enum name).
- `usage` — argument-syntax hint displayed in the command reference (e.g. `<description> /by <date/time>`).
- `description` — one-line explanation displayed in the command reference.

The static `Command.from(String)` factory throws `IllegalArgumentException` for unrecognised words. `Parser` catches this and converts it to the user-facing `EkkoException`.

**`ArgumentName` (enum)**

Defines the slash-prefixed argument names recognised by the parser (`by`, `from`, `to`). Storing them as an enum prevents typos in the matching logic and makes the set of reserved names explicit.

**`Parser`**

Splits a raw input line into a `ParsedCommand(command, arguments)` record. It trims whitespace, extracts the first token as the command word, and delegates the remainder to the relevant handler in `Ekko`.

**`ArgumentParser`**

Extracts named slash arguments from the text following a command word.

- Builds a regex pattern only from the argument names that the calling command declares as valid. Slash-prefixed words not in that set are treated as ordinary description text.
- Uses a lookahead-based scan so every argument value (including the last one) is extracted by a single code path, avoiding a separate "flush final value" step.
- Throws `IllegalArgumentException` if a recognised argument appears more than once.

**`ParsedArguments`**

A mutable result holder populated by `ArgumentParser`. Provides `getDescription()`, `containsArgument(ArgumentName)`, and `getArgument(ArgumentName)`.

---

### ekko.task

**`Task` (abstract)**

Base class for all task types. Holds a `description` (trimmed, non-null, non-blank, no `|`, no control characters) and an `isMarked` flag.

Key methods:
- `toSerializedString()` — abstract; each subclass produces a `| `-delimited line suitable for one-line file storage.
- `fromSerializedString(String)` — static factory; parses the type marker (`T`, `D`, `E`), validates field counts, constructs the correct subtype, and restores its completion state.
- `hasSameDetails(Task)` — used by `TaskList.add` to reject duplicate tasks. Subclasses override this to also compare their date fields.
- `occursOn(LocalDate)` — returns `false` by default; overridden by dated subtypes.

**`Todo`**

Serialized as `T | 0|1 | description`. `occursOn` is not overridden; todos never appear in `agenda` results.

**`Deadline`**

Holds a `LocalDateTime by`. Serialized as `D | 0|1 | description | ISO-datetime`. `occursOn` matches the due date.

**`Event`**

Holds `LocalDateTime from` and `to`; the constructor enforces `to.isAfter(from)`. Serialized as `E | 0|1 | description | ISO-datetime | ISO-datetime`. `occursOn` returns true for any date within the inclusive `[from.toLocalDate(), to.toLocalDate()]` interval.

**`TaskList`**

Wraps an `ArrayList<Task>`. Responsibilities:
- `add(Task)` — appends a task after checking for duplicates via `hasSameDetails`.
- `mark(int)` / `unmark(int)` — accept one-based task numbers and return a `TaskUpdate` record describing the task and whether its state changed.
- `delete(int)` — removes and returns the task at the given one-based position.
- `find(String)` — case-sensitive description substring search; rejects blank keywords.
- `findOn(LocalDate)` — filters by `occursOn`; used by the `agenda` command.
- `asList()` — returns an immutable snapshot.

---

### ekko.storage

**`Storage`**

A non-instantiable-by-default utility class that persists tasks to a configurable file path (defaulting to `data/ekko.txt` relative to the working directory).

- `loadTasks()` — returns an empty list if the file or its parent directory does not exist. Reads lines and delegates to `Task.fromSerializedString`. Validates the loaded list by constructing a `TaskList` (catching duplicates introduced by manual edits).
- `saveTasks(List<Task>)` — serializes tasks, writes to a sibling temporary file, then atomically replaces the destination using `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`. This prevents partial writes from corrupting the saved file.
- `deleteDataFile()` — used by the recovery path when the user confirms deletion of a corrupt file.

The constructor accepts a `Path` to allow test isolation with JUnit temporary directories.

---

### ekko.datetime

**`DateTimeParser`**

A final utility class for converting between user input strings, `LocalDate`/`LocalDateTime`, and display strings.

Supported input formats for `parse(String)`:

| Format | Example |
|---|---|
| `yyyy-MM-dd HHmm` | `2026-09-02 1800` |
| `d/M/yyyy HHmm` | `2/9/2026 1800` |
| `yyyy-MM-dd'T'HH:mm` or ISO datetime | `2026-09-02T18:00` |
| Date-only fallback (delegates to `parseDate`) | see below |

Supported input formats for `parseDate(String)`:

| Format | Example |
|---|---|
| `yyyy-MM-dd` | `2026-09-02` |
| `d/M/yyyy` | `2/9/2026` |

All formatters use `ResolverStyle.STRICT` so invalid calendar dates (e.g. February 30) are rejected at parse time. Display output is pinned to `Locale.ENGLISH` so month names are consistent regardless of the system locale. Dates stored in the persistence file use `LocalDateTime.toString()` (ISO-8601), which is parseable by both the date-time and date-only paths on load.

---

### ekko.ui

**`Ui`** (console)

Wraps `System.in` / `System.out` via `Scanner` and `PrintStream`. Accepts alternative streams in a package-private constructor so tests can inject `ByteArrayInputStream` / `PrintStream` without touching the real console.

**`GuiUi`**

Implements the same interface as `Ui` but routes output through three lambdas supplied by `Main`: one for normal messages, one for error messages, and one for the recovery confirmation dialog. This keeps all JavaFX code in `Main` and all application logic in `Ekko`.

---

## Key design decisions

**Single coordinator class (`Ekko`) shared by both frontends**

Rather than duplicating command logic in `Main` and a console class, `Ekko` accepts any `Ui` implementation and acts as the sole coordinator. Both entry points share identical parsing, validation, and persistence behaviour.

**`EkkoException` strictly for user-command errors**

`IOException` (file system) and `IllegalArgumentException` (malformed serialized data) are kept separate from `EkkoException` (invalid command input). This prevents startup failures from being mistakenly displayed as "Unknown command" and keeps each exception type meaningful to its caller.

**`ArgumentParser` with regex lookahead**

Named slash arguments (e.g. `/by`, `/from`, `/to`) are recognised only when explicitly listed by the calling command. Unknown slash words stay in the description text. A lookahead-based scan extracts all argument values including the final one through a single code path, removing the need for a separate "flush" step after the loop.

**Atomic file saves**

`saveTasks` writes to a sibling temporary file and uses `Files.move(ATOMIC_MOVE)` to replace the destination. This ensures the saved file is never left in a partially written state if the application or OS is interrupted mid-write.

**`Command` enum carries display metadata**

Usage hints and short descriptions are stored in the `Command` enum rather than hard-coded in the GUI. Adding a new command automatically adds it to the command reference without requiring a separate GUI edit.

**Separate `Launcher` class**

When JavaFX 17+ is loaded from the classpath (rather than as a named module), the JVM cannot detect whether the main class extends `Application`. Starting from a separate non-`Application` class (`Launcher`) bypasses this detection and allows the fat JAR to launch without additional module-path configuration.

**Test-injectable `Ui` and `Storage`**

Both dependencies are accepted through `Ekko`'s constructor. Tests supply a `Ui` backed by `ByteArrayInputStream`/`ByteArrayOutputStream` and a `Storage` backed by a JUnit `@TempDir`, keeping the test suite fully self-contained without touching the real console or file system.

---

## Testing

Tests live under `src/test/java` in the matching package hierarchy and are named `<ClassName>Test`.

**Running the full suite:**
```
.\gradlew.bat test
```

**Test organisation:**

| Class | What it covers |
|---|---|
| `EkkoTest` | Full application round-trips using in-memory UI and temp-dir storage |
| `GuiCommandTest` | JavaFX GUI controls, submission locking, error styling, delayed replies |
| `MainTest` | `Main.start` startup paths and storage-error handling |
| `CommandScenarios` | 45 documented application scenarios reused by `EkkoTest` |
| `DateTimeParserTest` | All four public methods: parsing, date-only parsing, date formatting, datetime formatting |
| `ArgumentParserTest` | Valid commands, duplicate arguments, unknown slash words, boundary cases |
| `ParserTest` | Blank input, all command words, unknown commands, whitespace |
| `CommandTest` / `ArgumentNameTest` / `ParsedArgumentsTest` | Enum behaviour and result-holder accessors |
| `TaskTest` / `TodoTest` / `DeadlineTest` / `EventTest` | Serialization round-trips, `occursOn`, `hasSameDetails`, invalid inputs |
| `TaskListTest` | Add, delete, mark, unmark, find, findOn, duplicate rejection, out-of-range numbers |
| `StorageTest` | Load (missing file, missing directory, malformed data), save, atomic replacement, delete |
| `UiTest` | Console output format including separators, task lists, and error prefix |

Coverage targets the top ~50% highest-value methods, prioritising complex, core, and critical business logic over trivial accessors. The test count grows with each code change as new regression cases are added for affected methods.
