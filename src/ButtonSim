import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

//Fake buttons: type commands, they get published on the bus.
public class ButtonSim {
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
        OutputStream out = socket.getOutputStream();
        Scanner in = new Scanner(System.in);
        System.out.println("Commands: hall <floor> up|down   cabin <car> <floor>   fire on|off   quit");
        while (in.hasNextLine()) {
            String[] w = in.nextLine().trim().split("\\s+");
            byte[] msg;
            if (w[0].equals("hall")) {
                int floor = Integer.parseInt(w[1]);
                int dir;
                if (w[2].equals("up")) {
                    dir = CCBus.UP;
                } else {
                    dir = CCBus.DOWN;
                }
                msg = DoorBus.encode(CCBus.HALL_BUTTON, 0, floor * 256 + dir);
            } else if (w[0].equals("cabin")) {
                int car = Integer.parseInt(w[1]);
                int floor = Integer.parseInt(w[2]);
                msg = DoorBus.encode(CCBus.CABIN_BUTTON, car, floor);
            } else if (w[0].equals("fire")) {
                int on;
                if (w[1].equals("on")) {
                    on = 1;
                } else {
                    on = 0;
                }
                msg = DoorBus.encode(CCBus.FIRE, 0, on);
            } else if (w[0].equals("quit")) {
                socket.close();
                return;
            } else {
                continue;
            }
            out.write(msg);
            out.flush();
            System.out.println("published " + Arrays.toString(msg));
        }
    }
}
