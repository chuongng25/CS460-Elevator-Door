/**
 * Digital Twin: Hardware simulation and physical door logic.
 * Models a center-opening sliding door with real-time kinematic updates,
 * realistic 7-line ASCII doorway visualization, and automatic safety reversal.
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

    public void stop() {
        running = false;
        if (ticker != null) {
            ticker.interrupt();
        }
    }
}
