# Ekko project

Ekko is a chatbot built as part of the CS3227 project. Given below are instructions on how to set it up.

Ekko is a deadpan robot administrator offering "Human task supervision."
Expect dry remarks about human memory, an amber-on-charcoal interface, and clear
task details. Validation and file-access errors stay direct so you can act on them.

## Setting up in Intellij

Prerequisites: JDK 25, update Intellij to the most recent version.

1. Open Intellij (if you are not in the welcome screen, click `File` > `Close Project` to close the existing project first)
1. Open the project into Intellij as follows:
   1. Click `Open`.
   1. Select the project directory, and click `OK`.
   1. If there are any further prompts, accept the defaults.
1. Configure the project to use **JDK 25** (not other versions) as explained in [here](https://www.jetbrains.com/help/idea/sdk.html#set-up-jdk).<br>
   In the same dialog, set the **Project language level** field to the `SDK default` option.
1. Reload the Gradle project to download JavaFX dependencies.
1. Run `src/main/java/ekko/Launcher.java` from IntelliJ, or set an existing run configuration's main class to `ekko.Launcher`. A window titled **Ekko** should appear.
1. For a direct IntelliJ Application run configuration, open **Run > Edit Configurations**,
   select the launcher configuration, choose **Modify options > Add VM options**, and enter
   `--enable-native-access=ALL-UNNAMED`. Gradle runs already include this option.

## Using the chatbot GUI

Run with Java 25:

```powershell
.\gradlew.bat run
```

On macOS/Linux, use `./gradlew run`. The JavaFX `Main` application is started through a separate
`Launcher`, following the [JavaFX tutorial Part 1](https://se-education.org/guides/tutorials/javaFxPart1.html).

Gradle runs, tests, generated launch scripts, and the executable JAR enable native access for
JavaFX's platform libraries. The JAR carries this permission in its manifest, so `java -jar`
needs no extra flag. This grants native access to all classpath code; only use trusted dependencies.
The tutorial's `Unsupported JavaFX configuration: classes were loaded from 'unnamed module'`
warning can still appear because this project uses classpath packaging. The tutorial explicitly
allows ignoring it; removing its cause requires loading JavaFX on the module path instead.

Type a command into the bottom field, then press **Enter** or click **Send**. Your command
appears immediately, followed by a 750 ms pause showing **PROCESSING COMMAND...** before the reply.
Input is disabled during the pause to prevent duplicate submissions. Closing the window during
the pause cancels the pending command. The scrollable conversation shows your commands and
Ekko's replies. Examples:

```text
todo read a book
deadline report /by 2026-09-02
event lunch /from 2026-09-02 1200 /to 2026-09-02 1300
list
mark 1
unmark 1
find book
agenda 2026-09-02
delete 1
bye
```

`find` and `agenda` retain each task's full-list number, so results may have gaps.
Use those numbers directly with `mark`, `unmark`, or `delete`. After a deletion,
run the search or `list` again to refresh the numbers.

Tasks are saved after changes to `data/ekko.txt`, relative to the working directory, and loaded
on startup. If saved data is invalid, a confirmation dialog asks before deleting it; choosing
No or closing the dialog preserves the file and ends the session. File-access errors appear
in the conversation and disable commands so that unsaved edits cannot accumulate.
After `bye`, the farewell stays visible; close the window to exit.

Commands tolerate leading/trailing spaces and repeated whitespace between the command and its arguments.
Required descriptions, dates, and task numbers must be present. Specify each `/by`, `/from`, or `/to`
argument only once. `list` and `bye` do not accept arguments. Descriptions cannot contain `|` or control
characters such as embedded newlines. Ordinary punctuation and Unicode text are allowed.

Dates must exist in the calendar, and an event must end strictly after it starts. A duplicate has the same
task type, case-sensitive description (after trimming its edges), and date/time values; completion status
does not distinguish tasks. The same description with a different task type or schedule is allowed.
These rules also apply to saved data. If an older file contains duplicates, blank descriptions, or invalid
event intervals, choose **No** at the recovery prompt to preserve it, then correct the file before restarting.

Saving writes a temporary file in the data directory and atomically replaces the original only after writing
finishes. If atomic replacement is unsupported or fails, Ekko reports a save error instead of falling back
to overwriting the original directly. This protects against partial writes, but is not a backup system.

To build and run the bundled application:

```powershell
.\gradlew.bat shadowJar
java -jar build/libs/duke.jar
```

The original console interface remains available by running `ekko.Ekko` directly in IntelliJ.
JavaFX control tests require a desktop display; on headless Linux use Xvfb. Command and storage
tests run without a display.

**Warning:** Keep the `src\main\java` folder as the root folder for Java files (i.e., don't rename those folders or move Java files to another folder outside of this folder path), as this is the default location some tools (e.g., Gradle) expect to find Java files.

## Checking code style

Use JDK 25 for all Gradle commands. Checkstyle 11.0.0 checks production and test Java code:

```powershell
.\gradlew.bat checkstyleMain checkstyleTest
```

Run style checks and the complete JUnit suite together with `.\gradlew.bat check`.
On macOS/Linux, use `./gradlew` instead of `.\gradlew.bat`.
The `test` task alone runs JUnit, not Checkstyle. Both `check` and `build` include Checkstyle.
Any unsuppressed warning or error fails the style check. HTML reports are written to
`build/reports/checkstyle/main.html` and `build/reports/checkstyle/test.html`.

The files in `config/checkstyle/` follow the
[SE-EDU tutorial](https://se-education.org/guides/tutorials/checkstyle.html) and are adapted from
[AddressBook Level 3](https://github.com/se-edu/addressbook-level3/tree/master/config/checkstyle).
Project adaptations match the `seedu-java-coding-standard` skill and `AGENTS.md`:

- Imports are grouped as static, Java platform, third-party, then `ekko`, alphabetically within each group.
- Wrapped lines use eight extra spaces, and operators (including assignments) start the continuation line.
- All types, including non-public test helpers, require Javadoc; test methods do not.
- Lambda opening parentheses may start a new line without disabling the rule for method calls.

Checkstyle catches mechanical violations; the skill and code review still cover judgment-based rules.
Do not broadly suppress violations to make the build pass. Any necessary exception should be narrow and explained.

### Optional IntelliJ integration

Install the **CheckStyle-IDEA** plugin, then open **Settings > Tools > Checkstyle**.
Select Checkstyle **11.0.0**, scan **Only Java sources (including tests)**, and add
`config/checkstyle/checkstyle.xml` as a local configuration. Mark it active.
If prompted for `config_loc`, set it to this project's absolute `config/checkstyle` directory.
Gradle remains the shared, reproducible check regardless of IDE settings.

## Package structure

All packages are below the `src/main/java` source root:

- `ekko`: application entry point and shared exception.
- `ekko.ui`: console input and output.
- `ekko.storage`: task-file loading and saving.
- `ekko.parser`: command and argument interpretation.
- `ekko.task`: task types and task-list operations.
- `ekko.datetime`: shared date/time parsing and formatting.
