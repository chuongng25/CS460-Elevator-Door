import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class Fire {
    private boolean FIRE_ACTIVE = false;
    private final OutputStream out;
    private final DataInputStream in;


    public Fire(String host, int port) throws IOException{
        Socket socket = new Socket(host, port);
        out = socket.getOutputStream();
        in = new DataInputStream(socket.getInputStream());
        send(DoorBus.encode(DoorBus.SUBSCRIBE, CCBus.FIRE, 0));
    }
    public void poll() throws IOException{
        send(DoorBus.encode(DoorBus.GET, CCBus.FIRE, 0));
        byte[] msg = new byte[DoorBus.SIZE];
        in.readFully(msg);
        if (msg[0] == DoorBus.NO_MESSAGE){
            return;
        }
        FIRE_ACTIVE = DoorBus.value(msg) == 1;
    }

    public boolean isFireDetected(){
        return FIRE_ACTIVE;
    }
    private void send(byte[] msg) throws IOException{
        out.write(msg);
        out.flush();
    }
}
