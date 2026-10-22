package smartfactory.util;

import java.util.Scanner;

/**
 * Reads input from the console safely. Wrong input never crashes the program;
 * the user is simply asked again.
 */
public class InputHelper {

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    /** Reads one full line (may be empty). */
    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            // input stream closed (e.g. Ctrl+D / Ctrl+Z) - exit cleanly instead of looping forever
            System.out.println();
            System.out.println("Input closed. Exiting Smart Factory system.");
            System.exit(0);
        }
        return scanner.nextLine().trim();
    }

    /** Keeps asking until something non-empty is typed. */
    public String readText(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (!text.isEmpty()) {
                return text;
            }
            System.out.println("  This field cannot be empty. Please try again.");
        }
    }

    /** Keeps asking until a whole number between min and max is typed. */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("  Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  '" + text + "' is not a valid whole number. Please try again.");
            }
        }
    }

    /** Returns true for y / yes, false for n / no. */
    public boolean readYesNo(String prompt) {
        while (true) {
            String text = readLine(prompt + " (y/n): ").toLowerCase();
            if (text.equals("y") || text.equals("yes")) {
                return true;
            }
            if (text.equals("n") || text.equals("no")) {
                return false;
            }
            System.out.println("  Please type y or n.");
        }
    }
}
