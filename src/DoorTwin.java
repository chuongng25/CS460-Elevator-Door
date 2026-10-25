import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Digital Twin: Hardware simulation and physical door logic.
 * Models a center-opening sliding door with real-time kinematic updates,
 * realistic 7-line ASCII doorway visualization, automatic safety reversal,
 * and an embedded line-delimited TCP socket server with live command logging.
 */
public class DoorTwin implements DoorApi {

    public enum State { CLOSED, OPENING, OPEN, CLOSING }

    public static final int DEFAULT_PORT = 5556;
    public static final double DEFAULT_DURATION_SEC = 2.0;
    private static final int LEAF_WIDTH = 14;

    private final double durationSec;
    private State state = State.CLOSED;
    private double position = 0.0; // 0.0 = fully closed, 1.0 = fully open
    private boolean obstruction = false;

    private Thread ticker;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean serverRunning = false;
    private final List<Socket> clientSockets = new CopyOnWriteArrayList<>();

    /**
     * Default constructor: initializes door and starts background real-time ticker (20 Hz).
     */
    public DoorTwin() {
        this(DEFAULT_DURATION_SEC, true);
    }

    /**
     * Flexible constructor for real-time execution or headless discrete testing.
     */
    public DoorTwin(double durationSec, boolean startTicker) {
        this.durationSec = durationSec;
        if (startTicker) {
            startTicker();
        }
    }

    private void startTicker() {
        ticker = new Thread(() -> {
            long last = System.currentTimeMillis();
            while (running) {
                try {
                    Thread.sleep(50); // 20 Hz tick
                } catch (InterruptedException e) {
                    break;
                }
                long now = System.currentTimeMillis();
                double dt = (now - last) / 1000.0;
                last = now;
                update(dt);
            }
        }, "DoorPhysicsTicker");
        ticker.setDaemon(true);
        ticker.start();
    }

    /**
     * Starts the embedded TCP socket server on the specified port.
     */
    public synchronized void startServer(int port) throws IOException {
        if (serverRunning) return;
        serverSocket = new ServerSocket(port);
        serverRunning = true;
        serverThread = new Thread(this::listenClients, "DoorTwin-Server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void listenClients() {
        while (serverRunning && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                clientSockets.add(client);
                Thread clientThread = new Thread(() -> handleClient(client), "DoorTwin-Client-" + client.getPort());
                clientThread.setDaemon(true);
                clientThread.start();
            } catch (IOException e) {
                if (!serverRunning) break;
            }
        }
    }

    private void handleClient(Socket socket) {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        System.out.println("[Twin:Server] Client connected: " + clientAddr);
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            String lastQueryCmd = null;
            String lastQueryRes = null;

            while (serverRunning && (line = in.readLine()) != null) {
                String trimmed = line.trim();
                String response = processProtocolCommand(trimmed);
                out.println(response);

                if ("STATUS".equalsIgnoreCase(trimmed)) {
                    continue; // Suppress continuous 100ms animation polling
                }

                boolean isQuery = trimmed.startsWith("IS_") || trimmed.startsWith("HAS_");
                String tag = trimmed.equalsIgnoreCase("OPEN") ? "[Twin:Server]" : "[Twin]";
                if (isQuery) {
                    // Log query only on transition to prevent spam during wait loops
                    if (!trimmed.equalsIgnoreCase(lastQueryCmd) || !response.equalsIgnoreCase(lastQueryRes)) {
                        System.out.printf("%s %s -> %s%n", tag, trimmed, response);
                        lastQueryCmd = trimmed;
                        lastQueryRes = response;
                    }
                } else {
                    // Always log active command triggers
                    System.out.printf("%s %s -> %s%n", tag, trimmed, response);
                    lastQueryCmd = null;
                }
            }
        } catch (IOException ignored) {
        } finally {
            System.out.println("[Twin:Server] Client disconnected: " + clientAddr);
            clientSockets.remove(socket);
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Processes line-delimited ASCII wire protocol commands.
     */
    public synchronized String processProtocolCommand(String line) {
        if (line == null || line.isEmpty()) return "ERR_EMPTY";
        String[] parts = line.split("\\s+");
        String cmd = parts[0].toUpperCase();

        switch (cmd) {
            case "OPEN":
                open();
                return "OK";
            case "CLOSE":
                close();
                return obstruction ? "ERR_OBSTRUCTED" : "OK";
            case "IS_OPEN":
                return Boolean.toString(isFullyOpen());
            case "IS_CLOSED":
                return Boolean.toString(isFullyClosed());
            case "HAS_OBSTRUCTION":
                return Boolean.toString(hasObstruction());
            case "SET_OBSTRUCTION":
                if (parts.length >= 2) {
                    setObstruction(Boolean.parseBoolean(parts[1]));
                    return "OK";
                }
                return "ERR_MISSING_PARAM";
            case "STATUS":
                return String.format(java.util.Locale.US, "STATUS %s %.4f %b", state.name(), position, obstruction);
            default:
                return "ERR_UNKNOWN_COMMAND: " + cmd;
        }
    }

    /**
     * Advances door kinematics by time step dt seconds.
     */
    public synchronized void update(double dt) {
        if (state == State.OPENING) {
            position = Math.min(1.0, position + dt / durationSec);
            if (position >= 1.0) {
                position = 1.0;
                state = State.OPEN;
            }
        } else if (state == State.CLOSING) {
            if (obstruction) {
                open(); // Safety reversal: instantly reverse if blocked
                return;
            }
            position = Math.max(0.0, position - dt / durationSec);
            if (position <= 0.0) {
                position = 0.0;
                state = State.CLOSED;
            }
        }
    }

    @Override
    public synchronized void open() {
        if (state != State.OPEN) {
            state = State.OPENING;
        }
    }

    @Override
    public synchronized void close() {
        if (!obstruction && state != State.CLOSED) {
            state = State.CLOSING;
        }
    }

    @Override
    public synchronized boolean isFullyOpen() {
        return position >= 1.0 && state == State.OPEN;
    }

    @Override
    public synchronized boolean isFullyClosed() {
        return position <= 0.0 && state == State.CLOSED;
    }

    @Override
    public synchronized boolean hasObstruction() {
        return obstruction;
    }

    public synchronized void setObstruction(boolean blocked) {
        this.obstruction = blocked;
        if (blocked && state == State.CLOSING) {
            open(); // Instant safety reversal
        }
    }

    public synchronized double getPosition() {
        return position;
    }

    public synchronized State getState() {
        return state;
    }

    /**
     * Renders a 7-line realistic ASCII text visual of the center-opening elevator doorway.
     */
    public synchronized String toAscii() {
        int closedW = (int) Math.round((1.0 - position) * LEAF_WIDTH);
        int openW = LEAF_WIDTH - closedW;

        String doorL = repeat('█', closedW) + repeat(' ', openW);
        String doorR = repeat(' ', openW) + repeat('█', closedW);

        char inChar = obstruction ? '·' : '░';
        String inL = repeat('█', closedW) + repeat(inChar, openW);
        String inR = repeat(inChar, openW) + repeat('█', closedW);

        String status = String.format("State: %-8s (%3.0f%%)", state, position * 100);
        String sensor = obstruction ? "⚠️ SENSOR: BLOCKED" : "SENSOR: CLEAR";

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

    public synchronized void stop() {
        running = false;
        serverRunning = false;
        if (ticker != null) {
            ticker.interrupt();
        }
        for (Socket s : clientSockets) {
            try {
                s.close();
            } catch (IOException ignored) {}
        }
        clientSockets.clear();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        DoorTwin twin = new DoorTwin();
        twin.startServer(port);
        System.out.println("==================================================");
        System.out.println("  CS460 Team #01: DoorTwin Simulation Server");
        System.out.println("  Port: " + port + " (TCP) | Physics Loop: 20 Hz");
        System.out.println("==================================================");
        System.out.println("[Twin:Server] Listening for controller connections...\n");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Twin:Server] Shutting down server...");
            twin.stop();
        }));

        Thread.currentThread().join();
    }
}
