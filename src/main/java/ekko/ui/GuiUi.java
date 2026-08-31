package ekko.ui;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ekko.task.Task;

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
        showMessage("Hello! I'm " + name + ".\nWhat can I do for you?");
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
    public void showTasks(String heading, List<Task> tasks) {
        StringBuilder message = new StringBuilder(heading);
        for (int i = 0; i < tasks.size(); i++) {
            message.append('\n').append(i + 1).append('.').append(tasks.get(i));
        }
        showMessage(message.toString());
    }

    @Override
    public void showSeparator() {
        // Spacing between conversation messages replaces console separators.
    }
}
