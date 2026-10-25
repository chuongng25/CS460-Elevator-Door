import java.util.Scanner;

/**
 * Interactive CLI & Test Harness for the Elevator Door Assembly.
 * Connects exclusively via DoorManager over a TCP socket connection.
 * Features an outer Connection Menu (Connect / Disconnect) and an inner
 * Door Control Menu with 7-line ASCII visualization.
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--test")) {
            runAutomatedSocketTests();
            return;
        }

        Scanner scanner = new Scanner(System.in);
        DoorManager door = new DoorManager();
        String outerMessage = null;

        boolean appRunning = true;
        while (appRunning) {
            clearScreen();
            System.out.println("==================================================");
            System.out.println("  CS460 Team #01: Elevator Door Controller CLI");
            System.out.println("  Network Status: DISCONNECTED");
            System.out.println("==================================================");
            if (outerMessage != null) {
                System.out.println(outerMessage);
                outerMessage = null;
            }
            System.out.println("--------------------------------------------------");
            System.out.println(" [1] Connect to default server (localhost:5556)");
            System.out.println(" [2] Connect to custom host & port");
            System.out.println(" [0] Exit Application");
            System.out.println("--------------------------------------------------");
            System.out.print("Select an option [0-2]: ");

            if (!scanner.hasNextLine()) break;
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    try {
                        door.connect(DoorManager.DEFAULT_HOST, DoorManager.DEFAULT_PORT);
                        runDoorControlSession(scanner, door, DoorManager.DEFAULT_HOST, DoorManager.DEFAULT_PORT);
                        outerMessage = "-> Disconnected from server (localhost:5556).";
                    } catch (Exception e) {
                        outerMessage = "[CONNECTION ERROR] Failed to connect to localhost:5556: " + e.getMessage();
                    }
                    break;
                case "2":
                    System.out.print("Enter server host [localhost]: ");
                    String host = scanner.nextLine().trim();
                    if (host.isEmpty()) host = "localhost";
                    System.out.print("Enter server port [5556]: ");
                    String portStr = scanner.nextLine().trim();
                    int port = portStr.isEmpty() ? 5556 : Integer.parseInt(portStr);
                    try {
                        door.connect(host, port);
                        runDoorControlSession(scanner, door, host, port);
                        outerMessage = "-> Disconnected from " + host + ":" + port;
                    } catch (Exception e) {
                        outerMessage = "[CONNECTION ERROR] Could not connect to " + host + ":" + port + ": " + e.getMessage();
                    }
                    break;
                case "0":
                case "q":
                case "exit":
                    clearScreen();
                    System.out.println("Exiting Elevator Door Controller. Goodbye!");
                    appRunning = false;
                    break;
                default:
                    outerMessage = "Invalid choice. Please select 0, 1, or 2.";
            }
        }

        door.disconnect();
        scanner.close();
    }

    private static void runDoorControlSession(Scanner scanner, DoorManager door, String host, int port) {
        String lastMessage = "-> Successfully connected to DoorTwin server at " + host + ":" + port;
        boolean inSession = true;

        while (inSession && door.isConnected()) {
            clearScreen();
            System.out.println("==================================================");
            System.out.println("  CS460 Team #01: Elevator Door Controller CLI");
            System.out.println("  Bridge: CONNECTED (TCP " + host + ":" + port + ")");
            System.out.println("==================================================");
            if (lastMessage != null) {
                System.out.println(lastMessage);
            }
            printMenu(door);
            System.out.print("Select an option [0-5]: ");
            if (!scanner.hasNextLine()) break;
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
                            System.out.println("  Bridge: CONNECTED (TCP " + host + ":" + port + ")");
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
                            System.out.println("  Bridge: CONNECTED (TCP " + host + ":" + port + ")");
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
                    case "disconnect":
                        door.disconnect();
                        inSession = false;
                        break;
                    default:
                        lastMessage = "Invalid choice. Please enter a number between 0 and 5.";
                }
            } catch (Exception e) {
                lastMessage = "[CONNECTION ERROR] " + e.getMessage();
                door.disconnect();
                inSession = false;
            }
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
        System.out.println(" [0] Disconnect (Return to connection menu)");
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
