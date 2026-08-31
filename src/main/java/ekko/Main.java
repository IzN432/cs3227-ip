package ekko;

import java.io.IOException;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import ekko.storage.Storage;
import ekko.ui.GuiUi;

/**
 * Displays a resizable JavaFX conversation window for the Ekko chatbot.
 */
public class Main extends Application {
    private static final Duration RESPONSE_DELAY = Duration.millis(750);

    private TextArea conversation;
    private TextField input;
    private Button send;
    private Ekko ekko;
    private Label status;
    /** Schedules a reply without blocking the JavaFX application thread. */
    private PauseTransition pendingReply;

    @Override
    public void start(Stage stage) {
        start(stage, new Storage());
    }

    /**
     * Opens the window using supplied storage, allowing isolated GUI tests.
     */
    void start(Stage stage, Storage storage) {
        Label greeting = new Label("Hello World! Welcome to Ekko.");
        greeting.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label help = new Label("Try: todo read a book | list | mark 1 | find book | bye\n"
                + "Also: deadline report /by 2026-09-02 | event lunch /from 2026-09-02 1200"
                + " /to 2026-09-02 1300\n"
                + "agenda 2026-09-02 | unmark 1 | delete 1");
        help.setWrapText(true);

        conversation = new TextArea();
        conversation.setId("conversation");
        conversation.setAccessibleText("Conversation with Ekko");
        conversation.setEditable(false);
        conversation.setWrapText(true);
        VBox.setVgrow(conversation, Priority.ALWAYS);

        input = new TextField();
        input.setId("commandInput");
        input.setPromptText("Type a command and press Enter");
        input.setAccessibleText("Command");
        HBox.setHgrow(input, Priority.ALWAYS);
        send = new Button("Send");
        send.setId("sendButton");
        send.setDefaultButton(true);
        send.setOnAction(event -> submitCommand());
        input.setOnAction(event -> submitCommand());

        status = new Label();
        status.setId("replyStatus");
        VBox root = new VBox(12, greeting, help, conversation, status, new HBox(8, input, send));
        root.setPadding(new Insets(16));
        stage.setTitle("Ekko");
        stage.setScene(new Scene(root, 720, 540));
        stage.setMinWidth(420);
        stage.setMinHeight(360);
        stage.setOnHidden(event -> stopSession());
        stage.show();

        GuiUi ui = new GuiUi(message -> appendMessage("Ekko", message), () -> confirmRecovery(stage));
        try {
            ekko = new Ekko(ui, storage);
            if (ekko.canStart()) {
                ui.showWelcome("Ekko");
                input.requestFocus();
            } else {
                stopSession();
            }
        } catch (IOException e) {
            appendMessage("Ekko", "Could not open the task file: " + e.getMessage()
                    + "\nPlease fix the file access problem and restart Ekko.");
            stopSession();
        }
    }

    /**
     * Echoes a command immediately, then schedules its execution after a visible pause.
     */
    private void submitCommand() {
        if (input.isDisabled()) {
            return;
        }
        String command = input.getText();
        input.clear();
        appendMessage("You", command);
        input.setDisable(true);
        send.setDisable(true);
        status.setText("Ekko is thinking...");
        pendingReply = new PauseTransition(RESPONSE_DELAY);
        pendingReply.setOnFinished(event -> {
            pendingReply = null;
            status.setText("");
            executeCommand(command);
        });
        pendingReply.play();
    }

    /**
     * Displays the delayed reply and restores input unless the session has ended.
     */
    private void executeCommand(String command) {
        try {
            if (ekko.processCommand(command)) {
                stopSession();
                return;
            }
        } catch (IOException e) {
            appendMessage("Ekko", "Could not save your changes: " + e.getMessage()
                    + "\nChanges may not be saved. Fix the file access problem and restart Ekko.");
            stopSession();
            return;
        }
        input.setDisable(false);
        send.setDisable(false);
        input.requestFocus();
    }

    private void appendMessage(String speaker, String message) {
        conversation.appendText(speaker + ":\n" + message + "\n\n");
        conversation.positionCaret(conversation.getLength());
    }

    /**
     * Keeps the farewell or error visible while preventing further edits.
     */
    private void stopSession() {
        if (pendingReply != null) {
            pendingReply.stop();
            pendingReply = null;
        }
        status.setText("");
        input.setDisable(true);
        send.setDisable(true);
        input.setPromptText("Session ended. Close this window to exit.");
    }

    /**
     * Requires explicit confirmation before deleting malformed saved data.
     */
    private String confirmRecovery(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete the invalid task file and start with an empty list? This cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        alert.initOwner(stage);
        alert.setTitle("Invalid saved tasks");
        alert.setHeaderText("Ekko could not read the stored task data.");
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES ? "yes" : "no";
    }
}
