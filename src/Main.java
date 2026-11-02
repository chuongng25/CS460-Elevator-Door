import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

/**
 * Test harness. Runs the door side (DoorTwin + DoorManager + DoorDash) and walks through the
 * tests, with everything going through the T03 Software Bus:
 *
 *   1. Obstruction: the door reports it, it is published on the bus, read back, and printed.
 *   2. Close: a command is published on the bus, the door side reads it and closes the door.
 *   3. Open
 *   4. Close is ignored while the doorway is blocked
 *   5. Blocked while closing: the door reopens
 *   6. Open while closing: the door reverses
 *   7. Auto-close after the door has been open for a while
 *
 *   java Main [host] [port]     (defaults: localhost 6000; the Software Bus must be running)
 */
public class Main {

    static final int CAR = 1;
    static final double HOLD_SEC = 3.0;   // door stays open this long before closing itself
    static int failed = 0;
    static final String[] STATE = {"CLOSED", "OPENING", "OPEN", "CLOSING"};

    static volatile int state = -1;       // last DOOR_STATE read from the bus
    static volatile int blocked = -1;     // last DOOR_OBSTRUCTION read from the bus
    static volatile byte[] lastMsg;       // last raw message read from the bus
    static OutputStream out;

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 6000;

        // door side: the door (simulated), the brain, and the bus connection
        DoorTwin twin = new DoorTwin(CAR);
        DoorApi door = new LoggingDoor(twin);
        DoorManager manager = new DoorManager(door, CAR, HOLD_SEC);
        manager.start();
        DoorDash dash = new DoorDash(manager, host, port);
        dash.start();

        // test side: its own connection to the bus
        Socket socket = new Socket(host, port);
        out = socket.getOutputStream();
        DataInputStream in = new DataInputStream(socket.getInputStream());
        send(DoorBus.encode(0x02, DoorBus.TOPIC_DOOR_STATE, CAR));          // subscribe
        send(DoorBus.encode(0x02, DoorBus.TOPIC_DOOR_OBSTRUCTION, CAR));
        Thread reader = new Thread(() -> {
            try {
                while (true) {
                    for (int topic : new int[]{DoorBus.TOPIC_DOOR_STATE, DoorBus.TOPIC_DOOR_OBSTRUCTION}) {
                        send(DoorBus.encode(0x05, topic, CAR));              // ask the bus for news
                        byte[] msg = new byte[DoorBus.SIZE];
                        in.readFully(msg);
                        if (msg[0] == 0x06) continue;                        // nothing new
                        lastMsg = msg;
                        int v = (int) DoorBus.value(msg);
                        if (topic == DoorBus.TOPIC_DOOR_STATE) state = v; else blocked = v;
                    }
                    Thread.sleep(20);
                }
            } catch (Exception e) {
                // socket closed: we are done
            }
        });
        reader.setDaemon(true);
        reader.start();

        // get the door open first so there is something to close
        if (!waitFor(() -> state >= 0)) {
            System.out.println("No status from the door side - is the bus running?");
            System.exit(1);
        }
        System.out.println("Setup: opening the door");
        send(DoorBus.encode(DoorBus.TOPIC_DOOR_COMMAND, CAR, 1));
        waitFor(() -> state == 2);
        System.out.println("   door is OPEN\n");

        //test 1: obstruction
        System.out.println("TEST 1: obstruction");
        twin.setBeamBroken(true);
        System.out.println("   1. door side: hasObstruction() = " + door.hasObstruction());
        System.out.println("   2. door side published DOOR_OBSTRUCTION on the bus");
        waitFor(() -> blocked == 1);
        System.out.println("   3. read from the bus: " + Arrays.toString(lastMsg) + "  (DOOR_OBSTRUCTION = " + blocked + ")");
        System.out.println("   4. terminal: OBSTRUCTION DETECTED\n");
        twin.setBeamBroken(false);
        waitFor(() -> blocked == 0);

        //test 2: close the door
        System.out.println("TEST 2: close the door");
        byte[] close = DoorBus.encode(DoorBus.TOPIC_DOOR_COMMAND, CAR, 0);
        System.out.println("   1. published the command on the bus: " + Arrays.toString(close) + "  (DOOR_COMMAND = close)");
        send(close);
        System.out.println("   2. door side read the command from the bus");
        waitFor(() -> state == 3);                                           // CLOSING seen on the bus
        waitFor(() -> state == 0);
        System.out.println("   4. read from the bus: DOOR_STATE = " + STATE[state] + "\n");

        //test 3: open
        System.out.println("TEST 3: open the door");
        command(true);
        check("door opens", waitFor(() -> state == 2));
        System.out.println();

        //test 4: close is ignored while blocked
        System.out.println("TEST 4: close is ignored while the doorway is blocked");
        twin.setBeamBroken(true);
        waitFor(() -> blocked == 1);
        command(false);
        Thread.sleep(1000);
        check("door stays OPEN", state == 2);
        twin.setBeamBroken(false);
        waitFor(() -> blocked == 0);
        System.out.println();

        //test 5: blocked while closing -> reopen
        System.out.println("TEST 5: blocked while closing, the door reopens");
        command(false);
        check("door starts closing", waitFor(() -> state == 3));
        twin.setBeamBroken(true);
        check("door reverses", waitFor(() -> state == 1 || state == 2));
        twin.setBeamBroken(false);
        check("door ends fully open", waitFor(() -> state == 2));
        System.out.println();

        //test 6: open while closing -> reverse
        System.out.println("TEST 6: open while closing, the door reverses");
        command(false);
        check("door starts closing", waitFor(() -> state == 3));
        command(true);
        check("door reverses", waitFor(() -> state == 1 || state == 2));
        check("door ends fully open", waitFor(() -> state == 2));
        System.out.println();

        //test 7: auto-close
        System.out.println("TEST 7: auto-close (waiting about " + (int) HOLD_SEC + " seconds, no command sent)");
        check("door closes by itself", waitFor(() -> state == 0));
        System.out.println();

        System.out.println(failed == 0 ? "ALL TESTS PASSED" : failed + " CHECK(S) FAILED");
        socket.close();
        dash.stop();
        manager.stop();
        twin.stop();
        System.exit(failed == 0 ? 0 : 1);
    }

    static void command(boolean open) throws java.io.IOException {
        byte[] msg = DoorBus.encode(DoorBus.TOPIC_DOOR_COMMAND, CAR, open ? 1 : 0);
        System.out.println("   published the command on the bus: " + Arrays.toString(msg) + "  (" + (open ? "open" : "close") + ")");
        send(msg);
    }

    static void check(String what, boolean ok) {
        System.out.println("   " + (ok ? "PASS  " : "FAIL  ") + what);
        if (!ok) failed++;
    }

    /** Waits up to 10 seconds. */
    static boolean waitFor(BooleanSupplier cond) throws InterruptedException {
        long end = System.currentTimeMillis() + 10000;
        while (!cond.getAsBoolean() && System.currentTimeMillis() < end) Thread.sleep(20);
        return cond.getAsBoolean();
    }

    static synchronized void send(byte[] msg) throws java.io.IOException {
        out.write(msg);
        out.flush();
    }

    /** Wraps the door and prints when the manager moves it. Everything else passes straight through. */
    static class LoggingDoor implements DoorApi {
        private final DoorApi door;
        LoggingDoor(DoorApi door) { this.door = door; }
        public void open()  { System.out.println("   -> door side calls door.open()");  door.open(); }
        public void close() { System.out.println("   3. door side calls door.close()"); door.close(); }
        public boolean isFullyOpen()    { return door.isFullyOpen(); }
        public boolean isFullyClosed()  { return door.isFullyClosed(); }
        public boolean hasObstruction() { return door.hasObstruction(); }
    }
}
