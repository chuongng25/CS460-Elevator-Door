import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Bridge / Driver implementing DoorApi over a TCP Socket connection.
 * Provides headless client control and real-time telemetry from DoorTwin.
 */
public class DoorManager implements DoorApi {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5556;
    private static final int LEAF_WIDTH = 14;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean connected = false;

    private DoorTwin.State lastState = DoorTwin.State.CLOSED;
    private double lastPosition = 0.0;
    private boolean lastObstruction = false;

    public DoorManager() {
    }

    public DoorManager(String host, int port) throws IOException {
        connect(host, port);
    }

    /**
     * Establishes TCP connection with the DoorTwin socket server.
     */
    public synchronized void connect(String host, int port) throws IOException {
        if (connected) return;
        socket = new Socket(host, port);
        socket.setSoTimeout(3000);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
    }

    /**
     * Closes the TCP connection cleanly.
     */
    public synchronized void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        if (out != null) out.close();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    private synchronized String sendCommand(String cmd) {
        if (!connected || socket == null || socket.isClosed()) {
            throw new IllegalStateException("DoorManager is not connected to DoorTwin server.");
        }
        try {
            out.println(cmd);
            String response = in.readLine();
            if (response == null) {
                connected = false;
                throw new IOException("Server closed connection unexpectedly.");
            }
            return response;
        } catch (IOException e) {
            connected = false;
            throw new RuntimeException("Socket I/O error communicating with DoorTwin: " + e.getMessage(), e);
        }
    }

    @Override
    public void open() {
        sendCommand("OPEN");
    }

    @Override
    public void close() {
        sendCommand("CLOSE");
    }

    @Override
    public boolean isFullyOpen() {
        return Boolean.parseBoolean(sendCommand("IS_OPEN"));
    }

    @Override
    public boolean isFullyClosed() {
        return Boolean.parseBoolean(sendCommand("IS_CLOSED"));
    }

    @Override
    public boolean hasObstruction() {
        return Boolean.parseBoolean(sendCommand("HAS_OBSTRUCTION"));
    }

    public void setObstruction(boolean blocked) {
        sendCommand("SET_OBSTRUCTION " + blocked);
    }

    /**
     * Polls DoorTwin server for real-time telemetry (state, position, obstruction).
     */
    public synchronized void queryStatus() {
        String res = sendCommand("STATUS");
        // Expected response format: STATUS <STATE> <POSITION> <OBSTRUCTION>
        if (res != null && res.startsWith("STATUS ")) {
            String[] parts = res.split("\\s+");
            if (parts.length >= 4) {
                lastState = DoorTwin.State.valueOf(parts[1]);
                lastPosition = Double.parseDouble(parts[2]);
                lastObstruction = Boolean.parseBoolean(parts[3]);
            }
        }
    }

    public DoorTwin.State getState() {
        queryStatus();
        return lastState;
    }

    public double getPosition() {
        queryStatus();
        return lastPosition;
    }

    /**
     * Renders a 7-line realistic ASCII text visual of the center-opening elevator doorway
     * generated dynamically from telemetry received over TCP.
     */
    public synchronized String toAscii() {
        queryStatus();
        int closedW = (int) Math.round((1.0 - lastPosition) * LEAF_WIDTH);
        int openW = LEAF_WIDTH - closedW;

        String doorL = repeat('█', closedW) + repeat(' ', openW);
        String doorR = repeat(' ', openW) + repeat('█', closedW);

        char inChar = lastObstruction ? '·' : '░';
        String inL = repeat('█', closedW) + repeat(inChar, openW);
        String inR = repeat(inChar, openW) + repeat('█', closedW);

        String status = String.format("State: %-8s (%3.0f%%)", lastState, lastPosition * 100);
        String sensor = lastObstruction ? "⚠️ SENSOR: BLOCKED" : "SENSOR: CLEAR";

        return String.join("\n",
            "  ╔════════════════╤════════════════╗  [ ▲ 1F ] " + status,
            "  ║ " + doorL + " │ " + doorR + " ║",
            "  ║ " + inL   + " │ " + inR   + " ║",
            "  ║ " + inL   + " │ " + inR   + " ║",
            "  ║ " + inL   + " │ " + inR   + " ║",
            "  ║ " + doorL + " │ " + doorR + " ║",
            "  ╚════════════════╧════════════════╝  [ " + sensor + " ]"
        );
    }

    private static String repeat(char c, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
