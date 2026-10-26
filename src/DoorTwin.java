public class DoorTwin implements DoorApi {
    //For what phase of openess the door is in
    public enum State {
         CLOSED, OPENING, OPEN, CLOSING
         }

    //distance a door panel can travel to fully open/shut. UPDATE USE ONLY IF SPEED CHANGES AND UPDATE DDS
    public static final double LEAF_TRAVEL_FT = 2.0;
    //speed is 1 foot per second per panel. UPDATE USE ONLY IF SPEED CHANGES AND UPDATE DDS
    public static final double DOOR_SPEED_FPS = 1.0;
    //time it takes to open/close a door fully. will be used later for updating GUI
    public static final double DEFAULT_DURATION_SEC = 2.0;
    //Twin threads clock for changing positition
    private static final long TICK_MS = 50;
    //each of the 4 elevator cars and effectively doors ID. Needed for implementation with 4 cars
    //but for the demo we only use car1.
    private final int carId;
    //starts closed
    private State state = State.CLOSED;
    //fully closed position is 0 (1 is open)
    private double position = 0.0;

    //The two sensors we have to have. beambroken checks if
    //something is in the opening. the silloaded is for if someone is standing in
    //the doorway. so one covers above one below kinda ish.
    private boolean beamBroken = false;
    private boolean sillLoaded = false;
    //handles the thread that moves the door 20 timees a second.
    private Thread ticker;
    //checks if the twin thread is still going
    private volatile boolean running = true;
    //sets the carID and starting the ticker
    //which basically just starts the update and gets
    //the process going
    public DoorTwin(int carId) {
        this.carId = carId;
        startTicker();
    }
    //basically every 50MS refreshes update
    private void startTicker() {
        //the thread twin runs on
        ticker = new Thread(new Runnable() {
            public void run() {
                //makes sure everything stays in sync
                //by comparing to time now.
                long last = System.currentTimeMillis();
                //loop that moves the clock and calls update
                //whenever it does
                while (running) {
                    try {
                        //sleep the thread so movement doesnt crash something
                        Thread.sleep(TICK_MS);
                    } catch (InterruptedException e) {
                        break;
                    }
                    //whats compared to the last time to make sure stuff is synced
                    long now = System.currentTimeMillis();
                    //var describing that^^^
                    double chaneInTime = (now - last) / 1000.0;
                    last = now;
                    update(chaneInTime);
                }
            }
        }, "ThreadNumber" + carId);
        ticker.setDaemon(true);
        //calls the run method above
        ticker.start();
    }
    //update the door states 
    public synchronized void update(double chaneInTime) {
        if (state == State.OPENING) {
            //what percent of the 2 second trip just elapsed?
            position += chaneInTime / DEFAULT_DURATION_SEC;
            if (position >= 1.0) {
                position = 1.0;
                //flip the state
                state = State.OPEN;
            }
        } else if (state == State.CLOSING) {
            if (hasObstruction()) {
                //open if theres an obstruction
                open();
                return;
            }
            position -= chaneInTime / DEFAULT_DURATION_SEC;
            if (position <= 0.0) {
                position = 0.0;
                //flip the state
                state = State.CLOSED;
            }
        }
    }

    public synchronized void open() {
        if (state != State.OPEN) {
            state = State.OPENING;
        }
    }

    public synchronized void close() {
        if (!hasObstruction() && state != State.CLOSED) {
            state = State.CLOSING;
        }
    }

    public synchronized boolean isFullyOpen() {
        return position >= 1.0 && state == State.OPEN;
    }

    public synchronized boolean isFullyClosed() {
        return position <= 0.0 && state == State.CLOSED;
    }

    public synchronized boolean hasObstruction() {
        return beamBroken || sillLoaded;
    }

    public synchronized void setBeamBroken(boolean broken) {
        this.beamBroken = broken;
        if (broken && state == State.CLOSING) {
            open();
        }
    }

    public synchronized void setSillLoaded(boolean loaded) {
        this.sillLoaded = loaded;
        if (loaded && state == State.CLOSING) {
            open();
        }
    }

    public synchronized boolean isBeamBroken() {
        return beamBroken;
    }

    public synchronized boolean isSillLoaded() {
        return sillLoaded;
    }

    public synchronized double getPosition() {
        return position;
    }

    public synchronized State getState() {
        return state;
    }

    public int getCarId() {
        return carId;
    }

    public void stop() {
        running = false;
        if (ticker != null) {
            ticker.interrupt();
        }
    }
}
