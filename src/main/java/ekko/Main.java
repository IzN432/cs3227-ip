package ekko;

import java.io.IOException;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import ekko.parser.Command;
import ekko.storage.Storage;
import ekko.ui.GuiUi;

/**
 * Displays a resizable JavaFX conversation window for the Ekko chatbot.
 */
public class Main extends Application {
    private static final Duration RESPONSE_DELAY = Duration.millis(750);

    private VBox conversation;
    private ScrollPane conversationScroll;
    private TextField input;
    private Button send;
    private Ekko ekko;
    private Label status;
    private Label identity;
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
        VBox root = createContent();
        stage.setTitle("Ekko");
        stage.setScene(new Scene(root, 720, 540));
        stage.getScene().getStylesheets().add(Main.class.getResource("/ekko/gui.css").toExternalForm());
        stage.setMinWidth(420);
        stage.setMinHeight(360);
        stage.setOnHidden(event -> stopSession());
        stage.show();

        startSession(stage, storage);
    }

    /**
     * Assembles the conversation controls in their displayed order.
     */
    private VBox createContent() {
        identity = new Label("EKKO ONLINE");
        identity.setId("identityStatus");
        identity.getStyleClass().add("app-title");
        Label subtitle = new Label("Human task supervision.");
        subtitle.getStyleClass().add("muted");
        VBox heading = new VBox(4, identity, subtitle);
        ScrollPane suggestions = createCommandSuggestions();
        TitledPane commandHelp = new TitledPane("Command reference", suggestions);
        commandHelp.setId("commandHelp");
        commandHelp.setExpanded(false);
        commandHelp.setAnimated(false);
        commandHelp.setMinHeight(Region.USE_PREF_SIZE);
        commandHelp.setMaxHeight(Region.USE_PREF_SIZE);

        createConversation();
        VBox transcript = new VBox(conversationScroll);
        // Reserve only the collapsed reference header, regardless of its expanded state.
        transcript.setPadding(new Insets(0, 0, 28, 0));
        StackPane conversationLayer = new StackPane(transcript, commandHelp);
        conversationLayer.setMinHeight(32);
        conversationLayer.setPrefHeight(0);
        StackPane.setAlignment(commandHelp, Pos.BOTTOM_LEFT);
        VBox.setVgrow(conversationLayer, Priority.ALWAYS);
        HBox commandBar = createCommandBar();
        status = new Label();
        status.setId("replyStatus");
        status.getStyleClass().add("muted");
        status.managedProperty().bind(status.textProperty().isNotEmpty());
        status.visibleProperty().bind(status.managedProperty());
        VBox composer = new VBox(8, status, commandBar);
        composer.getStyleClass().add("composer");
        VBox root = new VBox(12, heading, conversationLayer, composer);
        root.setPadding(new Insets(16));
        // Limit the overlay to the conversation area so it never covers the composer or heading.
        suggestions.prefHeightProperty().bind(Bindings.min(250,
                Bindings.max(0, conversationLayer.heightProperty().subtract(40))));
        return root;
    }

    /**
     * Builds a compact reference that scrolls independently of the conversation.
     */
    private ScrollPane createCommandSuggestions() {
        VBox commands = new VBox(4);
        commands.getStyleClass().add("command-suggestions");
        for (Command command : Command.values()) {
            commands.getChildren().add(createCommandSuggestion(command));
        }
        Label hint = new Label("Replace <placeholders> with your own values.\n"
                + "Date: 2026-09-02 · Date/time: 2026-09-02 1200");
        hint.setWrapText(true);
        hint.getStyleClass().add("command-format-hint");
        commands.getChildren().add(hint);
        ScrollPane suggestions = new ScrollPane(commands);
        suggestions.setId("commandSuggestions");
        suggestions.setAccessibleText("Commands and usage hints");
        suggestions.setFitToWidth(true);
        suggestions.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        suggestions.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        suggestions.setMinHeight(0);
        return suggestions;
    }

    /**
     * Presents a command name, wrapping usage syntax, and a brief explanation.
     */
    private VBox createCommandSuggestion(Command command) {
        Text name = new Text(command.getWord());
        name.getStyleClass().add("command-name");
        Text arguments = new Text(command.getUsage().isEmpty() ? "" : "  " + command.getUsage());
        arguments.getStyleClass().add("command-usage");
        TextFlow syntax = new TextFlow(name, arguments);
        syntax.setMinWidth(0);
        Label explanation = new Label(command.getDescription());
        explanation.setWrapText(true);
        explanation.setMinWidth(0);
        explanation.getStyleClass().add("command-description");
        VBox row = new VBox(5, syntax, explanation);
        row.setId("suggestion-" + command.getWord());
        row.getStyleClass().add("command-suggestion");
        return row;
    }

    /**
     * Creates the read-only conversation area that grows with the window.
     */
    private void createConversation() {
        conversation = new VBox(16);
        conversation.setId("conversation");
        conversation.setAccessibleText("Conversation with Ekko");
        conversation.setPadding(new Insets(12, 12, 12, 0));
        conversationScroll = new ScrollPane(conversation);
        conversationScroll.setId("conversationScroll");
        conversationScroll.setFitToWidth(true);
        conversationScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        conversationScroll.setMinHeight(32);
        VBox.setVgrow(conversationScroll, Priority.ALWAYS);
    }

    /**
     * Creates the command field and connects both submission controls.
     */
    private HBox createCommandBar() {
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
        HBox commandBar = new HBox(8, input, send);
        commandBar.setAlignment(Pos.CENTER);
        return commandBar;
    }

    /**
     * Loads saved tasks after the window is shown, disabling input if startup fails.
     */
    private void startSession(Stage stage, Storage storage) {
        GuiUi ui = new GuiUi(message -> appendMessage("Ekko", message),
                message -> appendMessage("Error", message), () -> confirmRecovery(stage));
        try {
            ekko = new Ekko(ui, storage);
            if (ekko.canStart()) {
                ui.showWelcome("Ekko");
                input.requestFocus();
            } else {
                stopSession();
            }
        } catch (IOException e) {
            appendMessage("Error", "Could not open the task file: " + e.getMessage()
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
        // Enabled input means the previous reply has finished or was never scheduled.
        assert pendingReply == null : "An enabled command field must not have a pending reply";
        String command = input.getText();
        input.clear();
        appendMessage("You", command);
        input.setDisable(true);
        send.setDisable(true);
        status.setText("PROCESSING COMMAND...");
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
        // Only a successfully initialized session may schedule a command callback.
        assert ekko != null && ekko.canStart() : "Delayed commands require a ready chatbot";
        // The submission lock must remain held throughout the delay and command execution.
        assert input.isDisabled() && send.isDisabled() : "Input must stay disabled until the reply completes";
        try {
            if (ekko.processCommand(command)) {
                stopSession();
                return;
            }
        } catch (IOException e) {
            appendMessage("Error", "Could not save your changes: " + e.getMessage()
                    + "\nChanges may not be saved. Fix the file access problem and restart Ekko.");
            stopSession();
            return;
        }
        input.setDisable(false);
        send.setDisable(false);
        input.requestFocus();
    }

    /**
     * Adds a wrapping message and scrolls to it after JavaFX lays out its height.
     */
    private void appendMessage(String speaker, String message) {
        // Startup, event handlers, and delayed callbacks must all update controls on the FX thread.
        assert Platform.isFxApplicationThread() : "Conversation updates must run on the JavaFX thread";
        boolean isUser = speaker.equals("You");
        Label author = new Label(speaker.equals("Error") ? "Ekko · Error" : speaker);
        author.getStyleClass().add("message-author");
        Label body = new Label(message);
        body.setWrapText(true);
        body.setMinWidth(0);
        body.setMaxWidth(Double.MAX_VALUE);
        body.getStyleClass().add("message-body");
        VBox messageBox = new VBox(5, author, body);
        messageBox.getStyleClass().add(isUser ? "user-message" : "app-message");
        if (speaker.equals("Error")) {
            messageBox.getStyleClass().add("error-message");
        }
        messageBox.maxWidthProperty().bind(conversation.widthProperty().subtract(12)
                .multiply(isUser ? 0.85 : 1.0));
        HBox row = new HBox(messageBox);
        row.setAlignment(isUser ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        HBox.setHgrow(messageBox, Priority.ALWAYS);
        conversation.getChildren().add(row);
        Platform.runLater(() -> {
            conversationScroll.applyCss();
            conversationScroll.layout();
            conversationScroll.setVvalue(1);
        });
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
        identity.setText("EKKO OFFLINE");
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
