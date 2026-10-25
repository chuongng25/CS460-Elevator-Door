import java.util.Scanner;

/**
 * Interactive CLI & Test Harness for the Elevator Door Assembly.
 * Connects directly to DoorTwin in Phase 2 for interactive hardware verification,
 * featuring a persistent clean-screen dashboard and 7-line ASCII visualization.
 */
public class Main {

    public static void main(String[] args) {
        DoorTwin door = new DoorTwin();
        Scanner scanner = new Scanner(System.in);
        String lastMessage = null;

        boolean running = true;
        while (running) {
            clearScreen();
            System.out.println("==================================================");
            System.out.println("  CS460 Team #01: Elevator Door Controller CLI");
            System.out.println("==================================================");
            if (lastMessage != null) {
                System.out.println(lastMessage);
            }
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
                            lastMessage = "-> Door is already fully open.";
                        } else {
                            door.open();
                            lastMessage = "-> Command sent: open()";
                            clearScreen();
                            System.out.println("==================================================");
                            System.out.println("  CS460 Team #01: Elevator Door Controller CLI");
                            System.out.println("==================================================");
                            System.out.println(lastMessage);
                            System.out.println("--------------------------------------------------");
                            animateDoor(door);
                        }
                        break;
                    case "2":
                        if (door.isFullyClosed()) {
                            lastMessage = "-> Door is already fully closed.";
                        } else if (door.hasObstruction()) {
                            lastMessage = "-> Cannot close: optical obstruction sensor is triggered!";
                        } else {
                            door.close();
                            lastMessage = "-> Command sent: close()";
                            clearScreen();
                            System.out.println("==================================================");
                            System.out.println("  CS460 Team #01: Elevator Door Controller CLI");
                            System.out.println("==================================================");
                            System.out.println(lastMessage);
                            System.out.println("--------------------------------------------------");
                            animateDoor(door);
                        }
                        break;
                    case "3":
                        lastMessage = "-> isFullyOpen(): " + door.isFullyOpen();
                        break;
                    case "4":
                        lastMessage = "-> isFullyClosed(): " + door.isFullyClosed();
                        break;
                    case "5":
                        lastMessage = "-> hasObstruction(): " + door.hasObstruction();
                        break;
                    case "0":
                    case "q":
                    case "exit":
                        clearScreen();
                        System.out.println("Exiting Elevator Door CLI. Goodbye!");
                        running = false;
                        break;
                    default:
                        lastMessage = "Invalid choice. Please enter a number between 0 and 5.";
                }
            } catch (UnsupportedOperationException e) {
                lastMessage = "[NOT IMPLEMENTED] " + e.getMessage();
            } catch (Exception e) {
                lastMessage = "[ERROR] " + e.getMessage();
            }
        }
        scanner.close();
        door.stop();
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void printMenu(DoorTwin door) {
        System.out.println("--------------------------------------------------");
        System.out.println(door.toAscii());
        System.out.println("--------------------------------------------------");
        System.out.println(" [1] open()          - Command door leaves to slide open");
        System.out.println(" [2] close()         - Command door leaves to slide shut");
        System.out.println(" [3] isFullyOpen()   - Query if aperture is 100% open");
        System.out.println(" [4] isFullyClosed() - Query if aperture is completely sealed");
        System.out.println(" [5] hasObstruction()- Query optical obstruction sensor");
        System.out.println(" [0] Exit CLI");
    }

    private static void animateDoor(DoorTwin door) {
        boolean first = true;
        while (door.getState() == DoorTwin.State.OPENING || door.getState() == DoorTwin.State.CLOSING) {
            if (!first) {
                System.out.print("\033[7A\r");
            }
            System.out.println(door.toAscii());
            first = false;
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                break;
            }
        }
        if (!first) {
            System.out.print("\033[7A\r");
        }
        System.out.println(door.toAscii());
    }
}
