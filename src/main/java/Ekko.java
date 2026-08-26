import java.util.Scanner;

/**
 * Starts the Ekko chatbot application.
 */
public class Ekko {

    public static void main(String[] args) {
        Ekko instance = new Ekko();
        instance.printSeparator();
        instance.printBanner();
        instance.sendMessage(String.format("Hello! I'm %s.\nWhat can I do for you?", instance.getName()));
        instance.printSeparator();

        String input = instance.getInput();
        instance.printSeparator();
        while (!input.equals("bye")) {
            instance.sendMessage(input);
            instance.printSeparator();
            input = instance.getInput();
            instance.printSeparator();
        }
        instance.sendMessage("Bye. Hope to see you again soon!");
        instance.printSeparator();
    }

    private final Scanner scanner;

    public Ekko() {
        scanner = new Scanner(System.in);
    }

    public String getInput() {
        String input = scanner.nextLine();
        System.out.println();
        return input;
    }

    public void printBanner() {
        String banner = " _______  __  ___  __  ___   ______   \n"
                + "|   ____||  |/  / |  |/  /  /  __  \\  \n"
                + "|  |__   |  '  /  |  '  /  |  |  |  | \n"
                + "|   __|  |    <   |    <   |  |  |  | \n"
                + "|  |____ |  .  \\  |  .  \\  |  `--'  | \n"
                + "|_______||__|\\__\\ |__|\\__\\  \\______/  \n";
        System.out.println(banner);
    }

    public void sendMessage(String message) {
        System.out.println(message);
        System.out.println();
    }

    public void printSeparator() {
        System.out.println("─".repeat(80));
    }

    public String getName() {
        return "Ekko";
    }
}
