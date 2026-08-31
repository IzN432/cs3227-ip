package ekko.ui;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Routes existing chatbot output and recovery prompts to a graphical interface.
 */
public class GuiUi extends Ui {
    private final Consumer<String> display;
    private final Consumer<String> displayError;
    private final Supplier<String> recoveryResponse;

    /**
     * Creates a UI that never reads from or writes to the console.
     *
     * @param display receives each chatbot message.
     * @param recoveryResponse asks the user whether invalid data should be deleted.
     */
    public GuiUi(Consumer<String> display, Supplier<String> recoveryResponse) {
        this(display, display, recoveryResponse);
    }

    /**
     * Creates a graphical UI with a separate error presentation.
     *
     * @param display receives normal messages.
     * @param displayError receives errors without relying on their wording.
     * @param recoveryResponse asks whether invalid data should be deleted.
     */
    public GuiUi(Consumer<String> display, Consumer<String> displayError, Supplier<String> recoveryResponse) {
        super(InputStream.nullInputStream(), new PrintStream(OutputStream.nullOutputStream()));
        this.display = display;
        this.displayError = displayError;
        this.recoveryResponse = recoveryResponse;
    }

    @Override
    public String readOptionalResponse() {
        return recoveryResponse.get();
    }

    @Override
    public String readCommand() {
        throw new UnsupportedOperationException("GUI commands arrive through the Send button or Enter key.");
    }

    @Override
    public void showWelcome(String name) {
        showMessage(name + " online. Welcome to the marketplace.");
    }

    @Override
    public void showMessage(String message) {
        display.accept(message);
    }

    @Override
    public void showError(String message) {
        displayError.accept(message);
    }

    @Override
    public void showSeparator() {
        // Spacing between conversation messages replaces console separators.
    }
}
