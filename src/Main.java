import java.util.Scanner;

/**
 * Interactive CLI & Test Harness for the Elevator Door Assembly.
 * Provides an interactive terminal menu offering the 5 standardized DoorApi operations.
 */
public class Main {

    public static void main(String[] args) {
        DoorApi door = new DoorManager();
        Scanner scanner = new Scanner(System.in);

        System.out.println("==================================================");
        System.out.println("  CS460 Team #01: Elevator Door Controller CLI");
        System.out.println("==================================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Select an option [0-5]: ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String input = scanner.nextLine().trim();

            try {
                switch (input) {
                    case "1":
                        door.open();
                        System.out.println("-> Command sent: open()");
                        break;
                    case "2":
                        door.close();
                        System.out.println("-> Command sent: close()");
                        break;
                    case "3":
                        System.out.println("-> isFullyOpen(): " + door.isFullyOpen());
                        break;
                    case "4":
                        System.out.println("-> isFullyClosed(): " + door.isFullyClosed());
                        break;
                    case "5":
                        System.out.println("-> hasObstruction(): " + door.hasObstruction());
                        break;
                    case "0":
                    case "q":
                    case "exit":
                        System.out.println("Exiting Elevator Door CLI. Goodbye!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a number between 0 and 5.");
                }
            } catch (UnsupportedOperationException e) {
                System.out.println("[NOT IMPLEMENTED] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
            }

            System.out.println();
        }
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("--- Door Operations Menu ---");
        System.out.println(" [1] open()          - Command door leaves to slide open");
        System.out.println(" [2] close()         - Command door leaves to slide shut");
        System.out.println(" [3] isFullyOpen()   - Query if aperture is 100% open");
        System.out.println(" [4] isFullyClosed() - Query if aperture is completely sealed");
        System.out.println(" [5] hasObstruction()- Query optical obstruction sensor");
        System.out.println(" [0] Exit CLI");
    }
}

