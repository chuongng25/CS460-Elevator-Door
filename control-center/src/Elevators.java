import java.io.IOException;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Elevators {
    private final OutputStream out;
    private final DataInputStream in;
    private final int[] floor = new int[5];
    private final boolean[] obstructed = new boolean[5];
    private final boolean[] overweight = new boolean[5];
        
    
    public Elevators(String host, int port) throws IOException{
        Socket socket = new Socket(host, port);
        out = socket.getOutputStream();
        in = new DataInputStream(socket.getInputStream());
        for (int i = 1; i < 5; i++){
            send(DoorBus.encode(DoorBus.SUBSCRIBE, CCBus.CAR_POSITION, i));
            send(DoorBus.encode(DoorBus.SUBSCRIBE, DoorBus.TOPIC_DOOR_OBSTRUCTION, i));
            send(DoorBus.encode(DoorBus.SUBSCRIBE, CCBus.OVERWEIGHT, i));
        }
    }
    public void poll() throws IOException{
        for (int i = 1; i < 5; i ++){
            byte[] msg = get(CCBus.CAR_POSITION, i);
            if (msg != null){
                floor[i] = (int) DoorBus.value(msg);
            }
            msg = get(DoorBus.TOPIC_DOOR_OBSTRUCTION, i);
            if (msg != null) {
            obstructed[i] = DoorBus.value(msg) == 1;
            }
            msg = get(CCBus.OVERWEIGHT, i);
            if (msg != null) {
            overweight[i] = DoorBus.value(msg) == 1;
            }
        }
    }
    // NOTE: maybe not correct for what we agrered on for all these.
    public void Service(int car, int floor) throws IOException{
        send(DoorBus.encode(CCBus.SERVICE, car, floor));
    }
    public void Goto(int car, int floor) throws IOException{
        send(DoorBus.encode(CCBus.GOTO, car, floor));

    }
    public void clear(int car) throws IOException{
        send(DoorBus.encode(CCBus.CLEAR, car, 0));

    }
    public void clearAll(){
        for (int i = 1; i < 5; i++){
            clear(i);    
        }
    }
    public byte[] get(int topic, int subtopic) throws IOException{
        send(DoorBus.encode(DoorBus.GET, topic, subtopic));
        byte[] msg = new byte[DoorBus.SIZE];
        in.readFully(msg);
        if (msg[0] == DoorBus.NO_MESSAGE){
            return null;
        }
        return msg;
    }
    public int getFloor(int car){
        return floor[car];
    }
    public boolean isAvailable(int car){
        return (!obstructed[car] && !overweight[car]);
    }
    private void send(byte[] msg) throws IOException{
    out.write(msg);
    out.flush();
    }

}
