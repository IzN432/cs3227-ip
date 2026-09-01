# Ekko

Ekko is a Java desktop marketplace where users buy and sell items with coins through a chatbot-style
interface. It was developed as an individual project for CS3227 MP1.

Users can create accounts, become sellers, create fixed-price or auction listings, search the marketplace,
buy items, place bids, and review their listings and purchases. Ekko persists account, listing, bid, and
conversation data between sessions.

## Requirements

- Java Development Kit (JDK) 25
- Windows, Linux, or macOS

The bundled JAR includes the JavaFX libraries for all three supported operating systems.

## Run the release

1. Download [the latest `ekko.jar`](release/ekko.jar).
2. Place it in a folder where Ekko may create a `data` directory.
3. Open a terminal in that folder.
4. Run:

```text
java -jar ekko.jar
```

Create an account, then try these commands:

```text
topup 500
becomeseller
bin Mechanical keyboard /desc Barely used, blue switches /price 80
list
```

See the [User Guide](docs/UserGuide.md) for the complete command reference, data-storage behavior, and
troubleshooting advice.

## Set up for development

1. Clone this repository.
2. Open the project root in IntelliJ IDEA.
3. Configure both the project SDK and Gradle JVM to use JDK 25.
4. Reload the Gradle project.
5. Run `ekko.Launcher`, or use the Gradle wrapper:

On Windows:

```powershell
.\gradlew.bat run
```

On Linux or macOS:

```bash
./gradlew run
```

On macOS, use `sdk use java 25.0.3.fx-zulu` first when that SDKMAN installation is available.

## Test and build

Use Java 25 for all Gradle commands. On Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat check
.\gradlew.bat shadowJar
```

On Linux or macOS, replace `.\gradlew.bat` with `./gradlew`.

- `test` runs the complete JUnit suite.
- `check` runs JUnit and Checkstyle on production and test code.
- `shadowJar` creates the executable fat JAR at `build/libs/ekko.jar`.

The submission-ready copy of the latest JAR is stored under `release/`.

See the [Developer Guide](docs/DeveloperGuide.md) for the architecture, design decisions, persistence
format, testing strategy, and acknowledgements. Reflections on the AI-assisted development process are in
[Reflections](docs/Reflections.md), with verified interaction summaries under [`logs/`](logs/).
