package ekko;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import ekko.listing.ListingStore;
import ekko.parser.Command;
import ekko.ui.GuiUi;
import ekko.users.User;
import ekko.users.UserStore;

/**
 * Displays a resizable JavaFX conversation window for the Ekko chatbot.
 */
public class Main extends Application {
    private static final Duration RESPONSE_DELAY = Duration.millis(750);

    private UserStore userStore;

    private VBox conversation;
    private ScrollPane conversationScroll;
    private TextField input;
    private Button send;
    private Marketplace marketplace;
    private User currentUser;
    private Label status;
    private Label identity;
    private Label userLabel;
    private Label balanceLabel;
    /** Schedules a reply without blocking the JavaFX application thread. */
    private PauseTransition pendingReply;

    @Override
    public void start(Stage stage) {
        User defaultUser = new User("user", "password");
        userStore = new UserStore(java.util.List.of(defaultUser));

        stage.setTitle("Ekko");
        stage.setOnHidden(event -> stopSession());
        showLoginScene(stage);
        stage.show();
    }

    /**
     * Creates and displays the login scene on the given stage.
     */
    private void showLoginScene(Stage stage) {
        VBox root = createLoginContent(stage);
        Scene scene = new Scene(root, 420, 340);
        scene.getStylesheets().add(Main.class.getResource("/ekko/gui.css").toExternalForm());
        stage.setMinWidth(320);
        stage.setMinHeight(260);
        stage.setScene(scene);
    }

    /**
     * Creates and displays the marketplace scene on the given stage for the authenticated user.
     */
    private void showMarketplaceScene(Stage stage, User user) {
        VBox root = createContent();
        Scene scene = new Scene(root, 720, 540);
        scene.getStylesheets().add(Main.class.getResource("/ekko/gui.css").toExternalForm());
        stage.setMinWidth(420);
        stage.setMinHeight(360);
        stage.setScene(scene);
        startMarketplace(user);
    }

    /**
     * Builds the login form with username, password, and an inline error label.
     */
    private VBox createLoginContent(Stage stage) {
        Label title = new Label("EKKO");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Sign in to continue.");
        subtitle.getStyleClass().add("muted");

        TextField usernameField = new TextField();
        usernameField.setId("loginUsername");
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setId("loginPassword");
        passwordField.setPromptText("Password");

        Label errorLabel = new Label();
        errorLabel.setId("loginError");
        errorLabel.getStyleClass().add("muted");
        errorLabel.setWrapText(true);
        errorLabel.managedProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.visibleProperty().bind(errorLabel.managedProperty());

        Button loginButton = new Button("Sign in");
        loginButton.setId("loginButton");
        loginButton.setDefaultButton(true);
        loginButton.setMaxWidth(Double.MAX_VALUE);

        Runnable attemptLogin = () -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            User user = userStore.authenticate(username, password);
            if (user == null) {
                errorLabel.setText("Invalid username or password.");
                passwordField.clear();
                passwordField.requestFocus();
            } else {
                showMarketplaceScene(stage, user);
            }
        };

        Button createAccountButton = new Button("Create account");
        createAccountButton.setId("createAccountButton");
        createAccountButton.setMaxWidth(Double.MAX_VALUE);

        Label firstTimeLabel = new Label("First time?");
        firstTimeLabel.getStyleClass().add("muted");
        firstTimeLabel.setMaxWidth(Double.MAX_VALUE);
        firstTimeLabel.setAlignment(Pos.CENTER);

        loginButton.setOnAction(e -> attemptLogin.run());
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> attemptLogin.run());
        createAccountButton.setOnAction(e -> showRegisterScene(stage));

        VBox form = new VBox(10, usernameField, passwordField, errorLabel, loginButton,
                new Separator(), createAccountButton, firstTimeLabel);
        form.getStyleClass().add("composer");
        form.setMaxWidth(280);

        VBox heading = new VBox(4, title, subtitle);
        heading.setAlignment(Pos.CENTER);
        VBox root = new VBox(20, heading, form);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        usernameField.requestFocus();
        return root;
    }

    /**
     * Creates and displays the registration scene on the given stage.
     */
    private void showRegisterScene(Stage stage) {
        VBox root = createRegisterContent(stage);
        Scene scene = new Scene(root, 420, 380);
        scene.getStylesheets().add(Main.class.getResource("/ekko/gui.css").toExternalForm());
        stage.setScene(scene);
    }

    /**
     * Builds the registration form with username, password, confirm-password, and an error label.
     * On success, adds the new user to the store and transitions to the marketplace.
     */
    private VBox createRegisterContent(Stage stage) {
        Label title = new Label("EKKO");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Create your account.");
        subtitle.getStyleClass().add("muted");

        TextField usernameField = new TextField();
        usernameField.setId("registerUsername");
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setId("registerPassword");
        passwordField.setPromptText("Password");

        PasswordField confirmField = new PasswordField();
        confirmField.setId("registerConfirm");
        confirmField.setPromptText("Confirm password");

        Label errorLabel = new Label();
        errorLabel.setId("registerError");
        errorLabel.getStyleClass().add("muted");
        errorLabel.setWrapText(true);
        errorLabel.managedProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.visibleProperty().bind(errorLabel.managedProperty());

        Button registerButton = new Button("Create account");
        registerButton.setId("registerButton");
        registerButton.setDefaultButton(true);
        registerButton.setMaxWidth(Double.MAX_VALUE);

        Button backButton = new Button("Back to sign in");
        backButton.setId("registerBackButton");
        backButton.setMaxWidth(Double.MAX_VALUE);

        Runnable attemptRegister = () -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirm = confirmField.getText();
            if (username.isBlank()) {
                errorLabel.setText("Please enter a username.");
                usernameField.requestFocus();
            } else if (userStore.contains(username)) {
                errorLabel.setText("That username is already taken.");
                usernameField.requestFocus();
            } else if (password.isBlank()) {
                errorLabel.setText("Please enter a password.");
                passwordField.requestFocus();
            } else if (!password.equals(confirm)) {
                errorLabel.setText("Passwords do not match.");
                confirmField.clear();
                confirmField.requestFocus();
            } else {
                User newUser = new User(username, password);
                userStore.add(newUser);
                showMarketplaceScene(stage, newUser);
            }
        };

        Label haveAccountLabel = new Label("Have an account?");
        haveAccountLabel.getStyleClass().add("muted");
        haveAccountLabel.setMaxWidth(Double.MAX_VALUE);
        haveAccountLabel.setAlignment(Pos.CENTER);

        registerButton.setOnAction(e -> attemptRegister.run());
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmField.requestFocus());
        confirmField.setOnAction(e -> attemptRegister.run());
        backButton.setOnAction(e -> showLoginScene(stage));

        VBox form = new VBox(10, usernameField, passwordField, confirmField, errorLabel,
                registerButton, new Separator(), backButton, haveAccountLabel);
        form.getStyleClass().add("composer");
        form.setMaxWidth(280);

        VBox heading = new VBox(4, title, subtitle);
        heading.setAlignment(Pos.CENTER);
        VBox root = new VBox(20, heading, form);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        usernameField.requestFocus();
        return root;
    }

    /**
     * Assembles the conversation controls in their displayed order.
     */
    private VBox createContent() {
        identity = new Label("EKKO ONLINE");
        identity.setId("identityStatus");
        identity.getStyleClass().add("app-title");
        Label subtitle = new Label("Ekko is here for your shopping.");
        subtitle.getStyleClass().add("muted");
        VBox headingText = new VBox(4, identity, subtitle);
        HBox.setHgrow(headingText, Priority.ALWAYS);
        userLabel = new Label();
        userLabel.setId("userLabel");
        userLabel.getStyleClass().add("app-title");
        balanceLabel = new Label();
        balanceLabel.setId("balanceLabel");
        balanceLabel.getStyleClass().add("app-title");
        HBox userInfo = new HBox(8, balanceLabel, userLabel);
        userInfo.setAlignment(Pos.TOP_RIGHT);
        HBox heading = new HBox(headingText, userInfo);
        heading.setAlignment(Pos.TOP_LEFT);
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
     * Initialises the marketplace session for the authenticated user.
     */
    private void startMarketplace(User user) {
        GuiUi ui = new GuiUi(message -> appendMessage("Ekko", message),
                message -> appendMessage("Error", message), () -> "yes");
        currentUser = user;
        ListingStore listingStore = new ListingStore(java.util.List.of());
        marketplace = new Marketplace(ui, currentUser, userStore, listingStore);
        userLabel.setText(currentUser.getUsername());
        updateBalanceLabel();
        ui.showWelcome("Ekko");
        input.requestFocus();
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
        assert marketplace != null : "Delayed commands require a ready marketplace";
        // The submission lock must remain held throughout the delay and command execution.
        assert input.isDisabled() && send.isDisabled() : "Input must stay disabled until the reply completes";
        if (marketplace.processCommand(command)) {
            stopSession();
            return;
        }
        updateBalanceLabel();
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
        messageBox.setCursor(Cursor.HAND);
        Tooltip.install(messageBox, new Tooltip("Click to copy"));
        messageBox.setOnMouseClicked(event -> copyToClipboard(message));
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
     * Updates the balance label to reflect the current user's coin balance.
     */
    private void updateBalanceLabel() {
        balanceLabel.setText("\uD83E\uDE99 " + currentUser.getBalance());
    }

    /**
     * Copies the given text to the system clipboard and briefly shows a confirmation.
     */
    private void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        String previous = status.getText();
        status.setText("COPIED TO CLIPBOARD");
        PauseTransition flash = new PauseTransition(Duration.millis(1200));
        flash.setOnFinished(e -> status.setText(previous));
        flash.play();
    }

    /**
     * Keeps the farewell or error visible while preventing further edits.
     */
    private void stopSession() {
        // No-op if the marketplace scene was never shown (e.g. closed on login screen).
        if (status == null) {
            return;
        }
        if (pendingReply != null) {
            pendingReply.stop();
            pendingReply = null;
        }
        status.setText("");
        input.setDisable(true);
        send.setDisable(true);
        identity.setText("EKKO OFFLINE");
        userLabel.setText("");
        balanceLabel.setText("");
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
