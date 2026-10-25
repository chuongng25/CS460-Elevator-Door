import java.util.Scanner;

/**
 * Interactive CLI & Test Harness for the Elevator Door Assembly.
 * Connects exclusively via DoorManager over a TCP socket connection
 * adhering to the CS460 Digital Twin Hardware Decoupling invariant.
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--test")) {
            runAutomatedSocketTests();
            return;
        }

        DoorTwin embeddedTwin = null;
        DoorManager door = new DoorManager();

        // Check if an external DoorTwin server is already running, or start embedded server
        try {
            door.connect(DoorManager.DEFAULT_HOST, DoorManager.DEFAULT_PORT);
        } catch (Exception e) {
            try {
                embeddedTwin = new DoorTwin();
                embeddedTwin.startServer(DoorTwin.DEFAULT_PORT);
                Thread.sleep(80); // Wait briefly for ServerSocket bind
                door.connect(DoorManager.DEFAULT_HOST, DoorManager.DEFAULT_PORT);
            } catch (Exception ex) {
                System.err.println("Failed to start or connect to DoorTwin server: " + ex.getMessage());
                return;
            }
        }

        Scanner scanner = new Scanner(System.in);
        String lastMessage = null;

        boolean running = true;
        while (running) {
            clearScreen();
            System.out.println("==================================================");
            System.out.println("  CS460 Team #01: Elevator Door Controller CLI");
            System.out.println("  Bridge: TCP Socket (localhost:" + DoorTwin.DEFAULT_PORT + ")");
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
                            lastMessage = "-> Command sent: open() [TCP -> OPEN]";
                            clearScreen();
                            System.out.println("==================================================");
                            System.out.println("  CS460 Team #01: Elevator Door Controller CLI");
                            System.out.println("  Bridge: TCP Socket (localhost:" + DoorTwin.DEFAULT_PORT + ")");
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
                            lastMessage = "-> Command sent: close() [TCP -> CLOSE]";
                            clearScreen();
                            System.out.println("==================================================");
                            System.out.println("  CS460 Team #01: Elevator Door Controller CLI");
                            System.out.println("  Bridge: TCP Socket (localhost:" + DoorTwin.DEFAULT_PORT + ")");
                            System.out.println("==================================================");
                            System.out.println(lastMessage);
                            System.out.println("--------------------------------------------------");
                            animateDoor(door);
                        }
                        break;
                    case "3":
                        lastMessage = "-> isFullyOpen(): " + door.isFullyOpen() + " [TCP -> IS_OPEN]";
                        break;
                    case "4":
                        lastMessage = "-> isFullyClosed(): " + door.isFullyClosed() + " [TCP -> IS_CLOSED]";
                        break;
                    case "5":
                        lastMessage = "-> hasObstruction(): " + door.hasObstruction() + " [TCP -> HAS_OBSTRUCTION]";
                        break;
                    case "0":
                    case "q":
                    case "exit":
                        clearScreen();
                        System.out.println("Disconnecting and exiting Elevator Door CLI. Goodbye!");
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
        door.disconnect();
        if (embeddedTwin != null) {
            embeddedTwin.stop();
        }
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void printMenu(DoorManager door) {
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

    private static void animateDoor(DoorManager door) {
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

    private static void runAutomatedSocketTests() {
        System.out.println("[TestHarness] Starting automated headless socket tests...");
        DoorTwin twin = new DoorTwin();
        DoorManager client = new DoorManager();
        try {
            twin.startServer(DoorTwin.DEFAULT_PORT);
            Thread.sleep(100);
            client.connect(DoorManager.DEFAULT_HOST, DoorTwin.DEFAULT_PORT);
            System.out.println("[TestHarness] Connected to DoorTwin over TCP port " + DoorTwin.DEFAULT_PORT);

            // Test 1: Initial State
            assertEqual("Initial isFullyClosed", true, client.isFullyClosed());
            assertEqual("Initial isFullyOpen", false, client.isFullyOpen());
            assertEqual("Initial hasObstruction", false, client.hasObstruction());

            // Test 2: Open Transition
            System.out.println("[TestHarness] Testing open() cycle...");
            client.open();
            long start = System.currentTimeMillis();
            while (!client.isFullyOpen() && System.currentTimeMillis() - start < 3500) {
                Thread.sleep(50);
            }
            assertEqual("Aperture 100% after open()", true, client.isFullyOpen());
            assertEqual("Not fully closed when open", false, client.isFullyClosed());

            // Test 3: Close Transition with Obstruction Safety Reversal
            System.out.println("[TestHarness] Testing close() with safety reversal...");
            client.close();
            Thread.sleep(500); // Allow closing to reach midway
            client.setObstruction(true); // Trip laser sensor
            assertEqual("Sensor reported obstruction", true, client.hasObstruction());

            // Verification of instant reversal to OPEN
            start = System.currentTimeMillis();
            while (!client.isFullyOpen() && System.currentTimeMillis() - start < 3500) {
                Thread.sleep(50);
            }
            assertEqual("Door safety-reversed to OPEN on obstruction", true, client.isFullyOpen());

            // Test 4: Clear obstruction and complete closing
            client.setObstruction(false);
            assertEqual("Obstruction cleared", false, client.hasObstruction());
            client.close();
            start = System.currentTimeMillis();
            while (!client.isFullyClosed() && System.currentTimeMillis() - start < 3500) {
                Thread.sleep(50);
            }
            assertEqual("Aperture fully sealed after close()", true, client.isFullyClosed());

            System.out.println("==================================================");
            System.out.println("  All Socket Bridge & FSM Tests PASSED! (4/4)");
            System.out.println("==================================================");
        } catch (Exception e) {
            System.err.println("[TestHarness] FAIL: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            client.disconnect();
            twin.stop();
        }
    }

    private static void assertEqual(String testName, boolean expected, boolean actual) {
        if (expected == actual) {
            System.out.println("  [PASS] " + testName + " (expected=" + expected + ", actual=" + actual + ")");
        } else {
            System.err.println("  [FAIL] " + testName + " (expected=" + expected + ", actual=" + actual + ")");
            System.exit(1);
        }
    }
}
