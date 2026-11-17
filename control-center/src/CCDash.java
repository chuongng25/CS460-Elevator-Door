import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

//DoorDash for the CC
public class CCDash {

    static final int NUM_CARS = 2;

    static OutputStream out;
    static DataInputStream in;

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 6000;
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }
        Socket socket = new Socket(host, port);
        out = socket.getOutputStream();
        in = new DataInputStream(socket.getInputStream());
        //command center has to subscribe to everything
        send(DoorBus.encode(DoorBus.SUBSCRIBE, CCBus.HALL_BUTTON, 0));
        send(DoorBus.encode(DoorBus.SUBSCRIBE, CCBus.FIRE, 0));
        for (int car = 1; car <= NUM_CARS; car++) {
            send(DoorBus.encode(DoorBus.SUBSCRIBE, CCBus.CABIN_BUTTON, car));
        }
        System.out.println("Command Center listening on " + host + ":" + port);
        // the bus never pushes, so keep asking for new messages
        while (true) {
            check(CCBus.HALL_BUTTON, 0);
            check(CCBus.FIRE, 0);
            for (int car = 1; car <= NUM_CARS; car++) {
                check(CCBus.CABIN_BUTTON, car);
            }
            Thread.sleep(20);
        }
    }

    static void check(int topic, int subtopic) throws Exception {
        send(DoorBus.encode(DoorBus.GET, topic, subtopic));
        byte[] msg = new byte[DoorBus.SIZE];
        in.readFully(msg);
        if (msg[0] == DoorBus.NO_MESSAGE) {
            return;
        }
        print(msg);
    }
    //just for testing with sim
    static void print(byte[] msg) {
        int topic = DoorBus.topic(msg);
        int sub = DoorBus.subtopic(msg);
        long value = DoorBus.value(msg);
        if (topic == CCBus.HALL_BUTTON) {
            long floor = value / 256;
            String dir;
            if (value % 256 == CCBus.UP) {
                dir = "UP";
            } else {
                dir = "DOWN";
            }
            System.out.println("hall button: floor " + floor + " " + dir + "   " + Arrays.toString(msg));
        } else if (topic == CCBus.CABIN_BUTTON) {
            System.out.println("cabin button: car " + sub + " floor " + value + "   " + Arrays.toString(msg));
        } else if (topic == CCBus.FIRE) {
            String state;
            if (value == 1) {
                state = "ON";
            } else {
                state = "OFF";
            }
            System.out.println("fire: " + state + "   " + Arrays.toString(msg));
        }
    }

    static void send(byte[] msg) throws Exception {
        out.write(msg);
        out.flush();
    }
}
