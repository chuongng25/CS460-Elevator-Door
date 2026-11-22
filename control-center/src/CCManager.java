import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// Manager: all the Control Center logic. It only talks to the four objects, never to the bus.
public class CCManager {

    public static final int CARS = 4;
    private static final int TICK_MS = 50;
    private static final int GROUND_FLOOR = 1;

    // ---- the API each of the four objects has to provide ----

    public enum Direction { UP, DOWN }

    public record HallRequest(int floor, Direction direction) { }

    // what the Manager can see of one elevator
    public record Status(int floor, boolean stopped, boolean obstructed, boolean overweight) { }

    public interface FireAlarmApi {
        boolean isFireDetected();
    }

    public interface CabinCallsApi {
        List<Integer> getNewRequests(int cabin);   // floors pressed in this elevator since the last call
        void resetButton(int cabin, int floor);
        void resetAll();
    }

    public interface FloorPanelsApi {
        List<HallRequest> getNewRequests();        // hall buttons pressed since the last call
        void resetFloor(int floor, Direction direction);
        void resetAll();
    }

    public interface ElevatorsApi {
        void service(int elevator, int floor);     // go to the floor and open the doors
        Status getStatus(int elevator);            // the latest the elevator has reported
    }

    // ---- the Manager ----

    // a stop on an elevator's list, and which buttons asked for it (they are reset on arrival)
    private static class Stop {
        final int floor;
        boolean cabin;
        final Set<Direction> hall = EnumSet.noneOf(Direction.class);

        Stop(int floor) {
            this.floor = floor;
        }
    }

    private static final int IDLE = 0, UP = 1, DOWN = 2;   // the way an elevator is travelling

    private final FireAlarmApi fireAlarm;
    private final CabinCallsApi cabinCalls;
    private final FloorPanelsApi floorPanels;
    private final ElevatorsApi elevators;

    private final List<List<Stop>> stops = new ArrayList<>();   // each elevator's list (index 1..CARS)
    private final int[] target = new int[CARS + 1];             // the stop last sent to each elevator, 0 = none
    private final int[] direction = new int[CARS + 1];
    private final List<HallRequest> waiting = new ArrayList<>(); // hall requests no elevator could take yet
    private boolean inFireMode;
    private volatile boolean running;

    public CCManager(FireAlarmApi fireAlarm, CabinCallsApi cabinCalls, FloorPanelsApi floorPanels, ElevatorsApi elevators) {
        this.fireAlarm = fireAlarm;
        this.cabinCalls = cabinCalls;
        this.floorPanels = floorPanels;
        this.elevators = elevators;
        for (int car = 0; car <= CARS; car++) {
            stops.add(new ArrayList<>());
        }
    }

    public void start() {
        running = true;
        Thread t = new Thread(() -> {
            while (running) {
                step();
                try {
                    Thread.sleep(TICK_MS);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "cc-manager");
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
    }

    // one pass of the control loop
    public synchronized void step() {
        boolean fire = fireAlarm.isFireDetected();   // Fire Mode lasts as long as the alarm does
        if (fire != inFireMode) {
            fireMode(fire);
        }
        if (inFireMode) {                            // button presses during a fire are thrown away
            for (int car = 1; car <= CARS; car++) {
                cabinCalls.getNewRequests(car);
            }
            floorPanels.getNewRequests();
            return;
        }
        for (int car = 1; car <= CARS; car++) {
            for (int floor : cabinCalls.getNewRequests(car)) {
                handleCabinRequest(car, floor);
            }
        }
        for (HallRequest r : floorPanels.getNewRequests()) {
            handleHallRequest(r);
        }
        for (HallRequest r : new ArrayList<>(waiting)) {   // retry the ones that had no elevator
            int car = selectElevator(r);
            if (car != 0) {
                waiting.remove(r);
                findOrAddStop(car, r.floor()).hall.add(r.direction());
            }
        }
        for (int car = 1; car <= CARS; car++) {
            handleElevatorEvent(car);
        }
        for (int car = 1; car <= CARS; car++) {
            sendElevator(car);
        }
    }

    // a button pressed inside an elevator: the passenger already chose, so it goes on that list
    public synchronized void handleCabinRequest(int car, int floor) {
        Status status = elevators.getStatus(car);
        if (status.stopped() && status.floor() == floor) {
            cabinCalls.resetButton(car, floor);   // already there
        } else {
            findOrAddStop(car, floor).cabin = true;
        }
    }

    // a hall button: pick an elevator, or keep it waiting if none can take it
    public synchronized void handleHallRequest(HallRequest request) {
        boolean known = waiting.contains(request);
        for (int car = 1; car <= CARS; car++) {
            Stop s = findStop(car, request.floor());
            known |= s != null && s.hall.contains(request.direction());
        }
        if (known) {
            return;
        }
        int car = selectElevator(request);
        if (car == 0) {
            waiting.add(request);
        } else {
            findOrAddStop(car, request.floor()).hall.add(request.direction());
        }
    }

    // if the elevator has stopped at the floor we sent it to, finish that stop and reset its buttons
    public synchronized void handleElevatorEvent(int car) {
        Status status = elevators.getStatus(car);
        if (target[car] == 0 || !status.stopped() || status.floor() != target[car]) {
            return;
        }
        Stop arrived = findStop(car, target[car]);
        stops.get(car).remove(arrived);
        target[car] = 0;
        if (arrived.cabin) {
            cabinCalls.resetButton(car, arrived.floor);
        }
        for (Direction d : arrived.hall) {
            floorPanels.resetFloor(arrived.floor, d);
        }
    }

    // the elevator for a hall request: a free one closest to the floor, else the closest one;
    // a tie goes to the lower number. 0 = none is available (obstructed or overweight).
    public synchronized int selectElevator(HallRequest request) {
        int best = 0;
        boolean bestFree = false;
        int bestDistance = 0;
        for (int car = 1; car <= CARS; car++) {
            Status status = elevators.getStatus(car);
            if (status.obstructed() || status.overweight()) {
                continue;
            }
            boolean free = stops.get(car).isEmpty() && target[car] == 0;
            int distance = Math.abs(status.floor() - request.floor());
            if (best == 0 || (free && !bestFree) || (free == bestFree && distance < bestDistance)) {
                best = car;
                bestFree = free;
                bestDistance = distance;
            }
        }
        return best;
    }

    // if the elevator is available and not on its way to a stop, send it the next stop by the sweep rule
    public synchronized void sendElevator(int car) {
        Status status = elevators.getStatus(car);
        if (target[car] != 0 || stops.get(car).isEmpty() || status.obstructed() || status.overweight()) {
            return;
        }
        // Sweep rule: keep going the same way to the nearest stop; when there is none, turn round.
        // An elevator with no direction yet takes the nearest stop.
        int floor = status.floor();
        Stop next = nearestStop(car, floor, direction[car]);
        if (next == null) {
            next = nearestStop(car, floor, direction[car] == UP ? DOWN : UP);
        }
        if (next.floor != floor) {
            direction[car] = next.floor > floor ? UP : DOWN;
        }
        target[car] = next.floor;
        elevators.service(car, next.floor);
    }

    // on: stop everything, clear every button, send all elevators to floor 1 with the doors open.
    // off: the alarm is over, go back to normal (the lists were emptied when it started)
    public synchronized void fireMode(boolean on) {
        inFireMode = on;
        waiting.clear();
        for (int car = 1; car <= CARS; car++) {
            stops.get(car).clear();
            target[car] = on ? GROUND_FLOOR : 0;
            direction[car] = IDLE;
        }
        if (on) {
            cabinCalls.resetAll();
            floorPanels.resetAll();
            for (int car = 1; car <= CARS; car++) {
                elevators.service(car, GROUND_FLOOR);
            }
        }
    }

    // the stop closest to the floor on the given side (the floor itself counts); IDLE = either side
    private Stop nearestStop(int car, int floor, int side) {
        Stop best = null;
        for (Stop s : stops.get(car)) {
            boolean onSide = side == IDLE || (side == UP ? s.floor >= floor : s.floor <= floor);
            int d = Math.abs(s.floor - floor);
            if (onSide && (best == null || d < Math.abs(best.floor - floor)
                    || (d == Math.abs(best.floor - floor) && s.floor < best.floor))) {
                best = s;
            }
        }
        return best;
    }

    private Stop findStop(int car, int floor) {
        for (Stop s : stops.get(car)) {
            if (s.floor == floor) {
                return s;
            }
        }
        return null;
    }

    private Stop findOrAddStop(int car, int floor) {
        Stop s = findStop(car, floor);
        if (s == null) {
            s = new Stop(floor);
            stops.get(car).add(s);
        }
        return s;
    }
}
