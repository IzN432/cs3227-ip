package ekko;

import javafx.application.Application;

/**
 * Launches JavaFX through a separate entry point to avoid classpath launch issues.
 */
public class Launcher {
    /**
     * Starts the graphical chatbot.
     *
     * @param args arguments passed to JavaFX.
     */
    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
