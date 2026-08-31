# Ekko project

Ekko is a chatbot built as part of the CS3227 project. Given below are instructions on how to set it up.

## Setting up in Intellij

Prerequisites: JDK 25, update Intellij to the most recent version.

1. Open Intellij (if you are not in the welcome screen, click `File` > `Close Project` to close the existing project first)
1. Open the project into Intellij as follows:
   1. Click `Open`.
   1. Select the project directory, and click `OK`.
   1. If there are any further prompts, accept the defaults.
1. Configure the project to use **JDK 25** (not other versions) as explained in [here](https://www.jetbrains.com/help/idea/sdk.html#set-up-jdk).<br>
   In the same dialog, set the **Project language level** field to the `SDK default` option.
1. After that, locate the `src/main/java/ekko/Ekko.java` file, right-click it, and choose `Run Ekko.main()` (if the code editor is showing compile errors, try restarting the IDE). For an existing run configuration, change its main class to `ekko.Ekko`. If the setup is correct, you should see something like the below as the output:
   ```
    _______  __  ___  __  ___   ______
   |   ____||  |/  / |  |/  /  /  __  \
   |  |__   |  '  /  |  '  /  |  |  |  |
   |   __|  |    <   |    <   |  |  |  |
   |  |____ |  .  \  |  .  \  |  `--'  |
   |_______||__|\__\ |__|\__\  \______/
   ```

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
