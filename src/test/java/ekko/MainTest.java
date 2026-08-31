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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    private TextArea conversation;
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
            assertFalse(conversation.isEditable());
            assertTrue(conversation.getText().contains("Hello! I'm Ekko."));
            input.setText("todo GUI task");
            long startedAt = System.nanoTime();
            send.fire();
            assertEquals("", input.getText());
            assertTrue(conversation.getText().contains("You:\n" + "todo GUI task"));
            assertFalse(conversation.getText().contains("I've added this task"));
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
            assertTrue(conversation.getText().contains("I've added this task"));
            assertEquals("T | 0 | GUI task", Files.readString(file).strip());
            assertEquals(1, conversation.getText().split("I've added this task", -1).length - 1);
            assertFalse(input.isDisabled());
            assertFalse(send.isDisabled());
            return null;
        });

        submitAndWait("list", true);
        onFxThread(() -> {
            assertTrue(conversation.getText().contains("1.[T][ ] GUI task"));
            return null;
        });
        submitAndWait(" ", false);
        onFxThread(() -> {
            assertTrue(conversation.getText().contains("Please enter a command."));
            assertFalse(input.isDisabled());
            return null;
        });
        submitAndWait("bye", false);
        onFxThread(() -> {
            assertTrue(conversation.getText().contains("Bye. Hope to see you again soon!"));
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
            assertTrue(conversation.getText().contains("Could not open the task file"));
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
            assertTrue(conversation.getText().contains("Could not save your changes"));
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
            assertFalse(conversation.getText().contains("I've added this task"));
            assertEquals("", status.getText());
            return null;
        });
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
            conversation = (TextArea) stage.getScene().lookup("#conversation");
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
