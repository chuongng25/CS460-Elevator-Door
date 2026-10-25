import java.util.Scanner;

/**
 * Interactive CLI & Test Harness for the Elevator Door Assembly.
 * Connects directly to DoorTwin in Phase 2 for interactive hardware verification,
 * featuring real-time ASCII visualization of the center-opening sliding doors.
 */
public class Main {

    public static void main(String[] args) {
        DoorTwin door = new DoorTwin();
        Scanner scanner = new Scanner(System.in);

        System.out.println("==================================================");
        System.out.println("  CS460 Team #01: Elevator Door Controller CLI");
        System.out.println("==================================================");

        boolean running = true;
        while (running) {
            printMenu(door);
            System.out.print("Select an option [0-5]: ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String input = scanner.nextLine().trim();

            try {
                switch (input) {
                    case "1":
                        if (door.isFullyOpen()) {
                            System.out.println("-> Door is already fully open.");
                        } else {
                            door.open();
                            System.out.println("-> Command sent: open()");
                            animateDoor(door);
                        }
                        break;
                    case "2":
                        if (door.isFullyClosed()) {
                            System.out.println("-> Door is already fully closed.");
                        } else if (door.hasObstruction()) {
                            System.out.println("-> Cannot close: optical obstruction sensor is triggered!");
                        } else {
                            door.close();
                            System.out.println("-> Command sent: close()");
                            animateDoor(door);
                        }
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
        door.stop();
    }

    private static void printMenu(DoorTwin door) {
        System.out.println("--------------------------------------------------");
        System.out.println("  Door Visual: " + door.toAscii());
        System.out.println("--------------------------------------------------");
        System.out.println(" [1] open()          - Command door leaves to slide open");
        System.out.println(" [2] close()         - Command door leaves to slide shut");
        System.out.println(" [3] isFullyOpen()   - Query if aperture is 100% open");
        System.out.println(" [4] isFullyClosed() - Query if aperture is completely sealed");
        System.out.println(" [5] hasObstruction()- Query optical obstruction sensor");
        System.out.println(" [0] Exit CLI");
    }

    private static void animateDoor(DoorTwin door) {
        // Poll dynamically while door is actively moving
        while (door.getState() == DoorTwin.State.OPENING || door.getState() == DoorTwin.State.CLOSING) {
            System.out.print("\r  Visual: " + door.toAscii() + "   ");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                break;
            }
        }
        // Print the final resting state and advance line
        System.out.println("\r  Visual: " + door.toAscii() + "   ");
    }
}
