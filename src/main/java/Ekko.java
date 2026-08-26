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
        instance.sendMessage("Bye. Hope to see you again soon!");
        instance.printSeparator();
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
