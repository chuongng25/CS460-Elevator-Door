import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * Small demo. Runs the door side (DoorTwin + DoorManager + DoorDash) and plays "the other team":
 * commands go out over the T03 Software Bus and the door's status comes back the same way.
 *
 *   java Main [host] [port]     (defaults: localhost 6000; the Software Bus must be running)
 */
public class Main {

    static final int CAR = 1;
    static final String[] STATE = {"CLOSED", "OPENING", "OPEN", "CLOSING"};

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 6000;

        // Door side
        DoorTwin door = new DoorTwin(CAR);
        DoorManager manager = new DoorManager(door, CAR);
        manager.start();
        new DoorDash(manager, host, port).start();

        // The "other team": its own connection to the bus
        Socket socket = new Socket(host, port);
        OutputStream out = socket.getOutputStream();
        DataInputStream in = new DataInputStream(socket.getInputStream());
        out.write(DoorBus.encode(0x02, DoorBus.TOPIC_DOOR_STATE, CAR));         // subscribe
        out.write(DoorBus.encode(0x02, DoorBus.TOPIC_DOOR_OBSTRUCTION, CAR));

        Thread listener = new Thread(() -> {
            try {
                int lastState = -1, lastBlocked = -1;
                while (true) {
                    for (int topic : new int[]{DoorBus.TOPIC_DOOR_STATE, DoorBus.TOPIC_DOOR_OBSTRUCTION}) {
                        synchronized (out) { out.write(DoorBus.encode(0x05, topic, CAR)); }   // ask for news
                        byte[] msg = new byte[DoorBus.SIZE];
                        in.readFully(msg);
                        if (msg[0] == 0x06) continue;                                   // nothing new
                        int v = (int) DoorBus.value(msg);
                        if (topic == DoorBus.TOPIC_DOOR_STATE && v != lastState) {
                            System.out.println("  door: " + STATE[lastState = v]);
                        } else if (topic == DoorBus.TOPIC_DOOR_OBSTRUCTION && v != lastBlocked) {
                            System.out.println("  sensor: " + ((lastBlocked = v) == 1 ? "BLOCKED" : "clear"));
                        }
                    }
                    Thread.sleep(50);
                }
            } catch (Exception e) {
                // socket closed: we are exiting
            }
        });
        listener.setDaemon(true);
        listener.start();

        System.out.println("[1] open  [2] close  [3] block door  [4] unblock door  [0] exit");
        Scanner keys = new Scanner(System.in);
        while (keys.hasNextLine()) {
            String choice = keys.nextLine().trim();
            if (choice.equals("0")) break;
            if (choice.equals("3")) door.setBeamBroken(true);
            else if (choice.equals("4")) door.setBeamBroken(false);
            else if (choice.equals("1") || choice.equals("2")) {
                synchronized (out) { out.write(DoorBus.encode(DoorBus.TOPIC_DOOR_COMMAND, CAR, choice.equals("1") ? 1 : 0)); }
            }
        }
        socket.close();
        manager.stop();
        door.stop();
    }
}
