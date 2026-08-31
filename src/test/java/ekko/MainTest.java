package ekko;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ekko.parser.Command;
import ekko.storage.Storage;

/**
 * Exercises the actual JavaFX controls on a desktop with isolated task storage.
 */
class MainTest {
    private static boolean isToolkitStarted;

    @TempDir
    Path directory;

    private Stage stage;
    private TextField input;
    private Button send;
    private VBox conversation;
    private Label status;

    @BeforeAll
    static void startToolkit() throws Exception {
        assumeTrue(!System.getProperty("os.name").toLowerCase().contains("linux")
                || System.getenv("DISPLAY") != null, "GUI tests need a display (use Xvfb on Linux).");
        FutureTask<Void> ready = new FutureTask<>(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
        Platform.startup(ready);
        ready.get(15, TimeUnit.SECONDS);
        isToolkitStarted = true;
    }

    @AfterAll
    static void stopToolkit() {
        if (isToolkitStarted) {
            Platform.exit();
        }
    }

    @AfterEach
    void closeWindow() throws Exception {
        onFxThread(() -> {
            if (stage != null) {
                stage.close();
            }
            return null;
        });
    }

    @Test
    void submitCommand_delay_showsThinkingPreventsDuplicatesAndThenReplies() throws Exception {
        Path file = directory.resolve("tasks.txt");
        openWindow(new Storage(file));
        long submittedAt = onFxThread(() -> {
            assertTrue(stage.isShowing());
            assertTrue(stage.isResizable());
            assertTrue(conversationText().contains("Hello! I'm Ekko."));
            input.setText("todo GUI task");
            long startedAt = System.nanoTime();
            send.fire();
            assertEquals("", input.getText());
            assertTrue(conversationText().contains("You:\n" + "todo GUI task"));
            assertFalse(conversationText().contains("I've added this task"));
            assertEquals("Ekko is thinking...", status.getText());
            assertTrue(input.isDisabled());
            assertTrue(send.isDisabled());
            assertFalse(Files.exists(file));
            // Neither control may enqueue another command during the pause.
            send.fire();
            input.fireEvent(new ActionEvent());
            return startedAt;
        });
        awaitReply();
        assertTrue(System.nanoTime() - submittedAt >= TimeUnit.MILLISECONDS.toNanos(700));
        onFxThread(() -> {
            assertTrue(conversationText().contains("I've added this task"));
            assertEquals("T | 0 | GUI task", Files.readString(file).strip());
            assertEquals(1, conversationText().split("I've added this task", -1).length - 1);
            assertFalse(input.isDisabled());
            assertFalse(send.isDisabled());
            return null;
        });

        submitAndWait("list", true);
        onFxThread(() -> {
            assertTrue(conversationText().contains("1.[T][ ] GUI task"));
            return null;
        });
        submitAndWait(" ", false);
        onFxThread(() -> {
            assertTrue(conversationText().contains("Please enter a command."));
            assertFalse(input.isDisabled());
            assertEquals(1, conversation.lookupAll(".error-message").size());
            return null;
        });
        submitAndWait("bye", false);
        onFxThread(() -> {
            assertTrue(conversationText().contains("Bye. Hope to see you again soon!"));
            assertTrue(input.isDisabled());
            assertTrue(send.isDisabled());
            assertTrue(stage.isShowing());
            return null;
        });
    }

    @Test
    void start_unreadableStorage_displaysErrorAndDisablesInput() throws Exception {
        openWindow(new Storage(directory));
        onFxThread(() -> {
            assertTrue(conversationText().contains("Could not open the task file"));
            assertEquals(1, conversation.lookupAll(".error-message").size());
            assertTrue(input.isDisabled());
            assertTrue(send.isDisabled());
            return null;
        });
    }

    @Test
    void submitCommand_saveFailure_displaysErrorAndStopsFurtherEdits() throws Exception {
        Path parent = directory.resolve("blocked");
        openWindow(new Storage(parent.resolve("tasks.txt")));
        Files.writeString(parent, "blocks directory creation");
        submitAndWait("todo cannot save", false);
        onFxThread(() -> {
            assertTrue(conversationText().contains("Could not save your changes"));
            assertEquals(1, conversation.lookupAll(".error-message").size());
            assertTrue(input.isDisabled());
            assertTrue(send.isDisabled());
            return null;
        });
    }

    @Test
    void submitCommand_windowClosedDuringDelay_cancelsPendingTask() throws Exception {
        Path file = directory.resolve("tasks.txt");
        openWindow(new Storage(file));
        CompletableFuture<Void> delayElapsed = new CompletableFuture<>();
        onFxThread(() -> {
            input.setText("todo must not save");
            send.fire();
            stage.close();
            PauseTransition wait = new PauseTransition(Duration.seconds(1));
            wait.setOnFinished(event -> delayElapsed.complete(null));
            wait.play();
            return null;
        });
        delayElapsed.get(5, TimeUnit.SECONDS);
        onFxThread(() -> {
            assertFalse(Files.exists(file));
            assertFalse(conversationText().contains("I've added this task"));
            assertEquals("", status.getText());
            return null;
        });
    }

    @Test
    void conversation_longReply_wrapsWhenWindowNarrowsAndHelpCanExpand() throws Exception {
        openWindow(new Storage(directory.resolve("tasks.txt")));
        submitAndWait("todo " + "Prepare the project demonstration and review the task list. ".repeat(8), true);
        double wideHeight = onFxThread(() -> {
            assertEquals(1, conversation.lookupAll(".user-message").size());
            assertEquals(2, conversation.lookupAll(".app-message").size());
            assertTrue(conversation.lookupAll(".error-message").isEmpty());
            Label body = (Label) ((VBox) ((HBox) conversation.getChildren().getLast())
                    .getChildren().getFirst()).getChildren().get(1);
            double height = body.getHeight();
            stage.setWidth(420);
            stage.setHeight(540);
            return height;
        });
        submitAndWait("list", true);
        onFxThread(() -> {
            VBox message = (VBox) ((HBox) conversation.getChildren().get(2)).getChildren().getFirst();
            Label body = (Label) message.getChildren().get(1);
            assertTrue(body.getHeight() > wideHeight, "Long replies should wrap onto more lines.");
            assertTrue(message.getWidth() <= conversation.getWidth());
            assertTrue(send.localToScene(send.getBoundsInLocal()).getMaxX() <= stage.getScene().getWidth());
            assertTrue(input.getWidth() > 100, "The composer should remain usable at minimum width.");
            TitledPane help = (TitledPane) stage.getScene().lookup("#commandHelp");
            assertFalse(help.isExpanded());
            ScrollPane transcript = (ScrollPane) stage.getScene().lookup("#conversationScroll");
            double conversationHeight = transcript.getHeight();
            double scrollPosition = transcript.getVvalue();
            double inputY = input.localToScene(input.getBoundsInLocal()).getMinY();
            help.setExpanded(true);
            stage.getScene().getRoot().layout();
            assertEquals(conversationHeight, transcript.getHeight(), 0.1);
            assertEquals(scrollPosition, transcript.getVvalue(), 0.001);
            assertEquals(inputY, input.localToScene(input.getBoundsInLocal()).getMinY(), 0.1);
            assertTrue(help.getContent().isVisible());
            assertTrue(help.getContent().getBoundsInParent().getHeight() > 150,
                    "The reference should show several rows at the normal window height.");
            help.setExpanded(false);
            stage.getScene().getRoot().layout();
            assertEquals(conversationHeight, transcript.getHeight(), 0.1);
            return null;
        });
    }

    @Test
    void commandReference_expanded_showsUsageAndScrollsToLastCommand() throws Exception {
        openWindow(new Storage(directory.resolve("tasks.txt")));
        onFxThread(() -> {
            stage.setWidth(420);
            stage.setHeight(360);
            TitledPane help = (TitledPane) stage.getScene().lookup("#commandHelp");
            help.setExpanded(true);
            return null;
        });
        // Allow a reply cycle so the window manager can apply the smaller dimensions.
        submitAndWait("list", true);
        onFxThread(() -> {
            ScrollPane suggestions = (ScrollPane) stage.getScene().lookup("#commandSuggestions");
            VBox commands = (VBox) suggestions.getContent();
            assertEquals(Command.values().length, commands.lookupAll(".command-suggestion").size());
            for (Command command : Command.values()) {
                VBox row = (VBox) commands.lookup("#suggestion-" + command.getWord());
                TextFlow rowSyntax = (TextFlow) row.getChildren().getFirst();
                Text usage = (Text) rowSyntax.getChildren().get(1);
                assertEquals(command.getWord(), ((Text) rowSyntax.getChildren().getFirst()).getText());
                assertEquals(command.getUsage().isEmpty() ? "" : "  " + command.getUsage(),
                        usage.getText());
                assertEquals(command.getDescription(), ((Label) row.getChildren().get(1)).getText());
            }
            VBox deadline = (VBox) commands.lookup("#suggestion-deadline");
            TextFlow syntax = (TextFlow) deadline.getChildren().getFirst();
            assertEquals("deadline", ((Text) syntax.getChildren().getFirst()).getText());
            assertEquals("  <description> /by <date/time>", ((Text) syntax.getChildren().get(1)).getText());
            assertTrue(commands.getHeight() > suggestions.getViewportBounds().getHeight());
            Node lastCommand = commands.lookup("#suggestion-bye");
            double beforeScroll = lastCommand.localToScene(lastCommand.getBoundsInLocal()).getMinY();
            suggestions.setVvalue(1);
            stage.getScene().getRoot().layout();
            assertTrue(lastCommand.localToScene(lastCommand.getBoundsInLocal()).getMinY() < beforeScroll);
            assertTrue(commands.getWidth() <= suggestions.getViewportBounds().getWidth() + 1);
            assertTrue(send.localToScene(send.getBoundsInLocal()).getMaxY() <= stage.getScene().getHeight());
            assertFalse(input.isDisabled());
            return null;
        });
    }

    /**
     * Reads displayed message labels in conversation order.
     */
    private String conversationText() {
        StringBuilder text = new StringBuilder();
        for (Node row : conversation.getChildren()) {
            VBox message = (VBox) ((HBox) row).getChildren().getFirst();
            text.append(((Label) message.getChildren().getFirst()).getText()).append(":\n");
            text.append(((Label) message.getChildren().get(1)).getText()).append("\n\n");
        }
        return text.toString();
    }

    /**
     * Creates and looks up the controls on the JavaFX application thread.
     */
    private void openWindow(Storage storage) throws Exception {
        onFxThread(() -> {
            stage = new Stage();
            new Main().start(stage, storage);
            input = (TextField) stage.getScene().lookup("#commandInput");
            send = (Button) stage.getScene().lookup("#sendButton");
            conversation = (VBox) stage.getScene().lookup("#conversation");
            status = (Label) stage.getScene().lookup("#replyStatus");
            return null;
        });
    }

    /**
     * Submits through either control and waits outside the JavaFX thread.
     */
    private void submitAndWait(String command, boolean useEnter) throws Exception {
        onFxThread(() -> {
            input.setText(command);
            if (useEnter) {
                input.fireEvent(new ActionEvent());
            } else {
                send.fire();
            }
            return null;
        });
        awaitReply();
    }

    /**
     * Waits for completion without blocking JavaFX pulses or assuming exact scheduling.
     */
    private void awaitReply() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (onFxThread(() -> status.getText().isEmpty())) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Ekko did not finish its delayed reply.");
    }

    /**
     * Propagates JavaFX results and assertions back to JUnit with a bounded wait.
     */
    private static <T> T onFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}
