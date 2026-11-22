import java.util.ArrayList;
import java.util.List;

// Tests the Manager by itself. The four objects below it are replaced by small fakes that
// record every command the Manager gives them, so no bus and no other classes are needed.
// Run:  java CCTest
public class CCTest {

    static final int CARS = CCManager.CARS;
    static final List<String> log = new ArrayList<>();
    static int failures = 0;

    // the fakes (they play the four objects)

    static class FakeFire implements CCManager.FireAlarmApi {
        boolean fire;

        public boolean isFireDetected() {
            return fire;
        }
    }

    static class FakeCabinCalls implements CCManager.CabinCallsApi {
        final List<List<Integer>> pending = new ArrayList<>();

        FakeCabinCalls() {
            for (int car = 0; car <= CARS; car++) {
                pending.add(new ArrayList<>());
            }
        }

        public List<Integer> getNewRequests(int cabin) {
            List<Integer> copy = new ArrayList<>(pending.get(cabin));
            pending.get(cabin).clear();
            return copy;
        }

        public void resetButton(int cabin, int floor) {
            log.add("resetButton " + cabin + " " + floor);
        }

        public void resetAll() {
            log.add("resetAllCabinButtons");
        }
    }

    static class FakeFloorPanels implements CCManager.FloorPanelsApi {
        final List<CCManager.HallRequest> pending = new ArrayList<>();

        public List<CCManager.HallRequest> getNewRequests() {
            List<CCManager.HallRequest> copy = new ArrayList<>(pending);
            pending.clear();
            return copy;
        }

        public void resetFloor(int floor, CCManager.Direction direction) {
            log.add("resetFloor " + floor + " " + direction);
        }

        public void resetAll() {
            log.add("resetAllFloorButtons");
        }
    }

    static class FakeElevators implements CCManager.ElevatorsApi {
        final CCManager.Status[] status = new CCManager.Status[CARS + 1];

        FakeElevators() {
            for (int car = 1; car <= CARS; car++) {
                status[car] = new CCManager.Status(1, false, false, false);
            }
        }

        public void service(int elevator, int floor) {
            log.add("service " + elevator + " " + floor);
        }

        public CCManager.Status getStatus(int elevator) {
            return status[elevator];
        }
    }

    static FakeFire fire = new FakeFire();
    static FakeCabinCalls cabin = new FakeCabinCalls();
    static FakeFloorPanels hall = new FakeFloorPanels();
    static FakeElevators elevators = new FakeElevators();
    static CCManager manager = new CCManager(fire, cabin, hall, elevators);

    // ---- helpers ----

    // a car reports: stopped or moving, and its floor
    static void car(int car, boolean stopped, int floor) {
        CCManager.Status old = elevators.status[car];
        elevators.status[car] = new CCManager.Status(floor, stopped, old.obstructed(), old.overweight());
    }

    static void sensors(int car, boolean obstructed, boolean overweight) {
        CCManager.Status old = elevators.status[car];
        elevators.status[car] = new CCManager.Status(old.floor(), old.stopped(), obstructed, overweight);
    }

    static void cabinButton(int car, int floor) {
        cabin.pending.get(car).add(floor);
    }

    static void hallButton(int floor, CCManager.Direction direction) {
        hall.pending.add(new CCManager.HallRequest(floor, direction));
    }

    // run the Manager's control loop a few times
    static void step() {
        for (int i = 0; i < 3; i++) {
            manager.step();
        }
    }

    static int mark() {
        return log.size();
    }

    static boolean has(int from, String command) {
        return log.subList(from, log.size()).contains(command);
    }

    static void check(String what, boolean ok) {
        System.out.println("   " + (ok ? "PASS" : "FAIL") + "  " + what);
        if (!ok) {
            failures++;
        }
    }

    public static void main(String[] args) {
        System.out.println("Setup: cars 1,2,3,4 are stopped at floors 1, 8, 1, 1");
        car(1, true, 1);
        car(2, true, 8);
        car(3, true, 1);
        car(4, true, 1);

        System.out.println("\nTEST 1: hall button, floor 7 down -> car 2 (free and closest, on floor 8)");
        int m = mark();
        hallButton(7, CCManager.Direction.DOWN);
        step();
        check("service car 2 -> floor 7", has(m, "service 2 7"));

        System.out.println("\nTEST 2: cabin button, car 1 floor 4 -> goes straight onto car 1");
        m = mark();
        cabinButton(1, 4);
        step();
        check("service car 1 -> floor 4", has(m, "service 1 4"));

        System.out.println("\nTEST 3: car 1 stops at floor 4 -> its button is cleared, next cabin button (floor 6) is sent");
        m = mark();
        car(1, true, 4);
        step();
        check("resetButton car 1 floor 4", has(m, "resetButton 1 4"));
        cabinButton(1, 6);
        step();
        check("service car 1 -> floor 6", has(m, "service 1 6"));

        System.out.println("\nTEST 4: car 1 passes floor 6 without stopping -> not an arrival, floor 8 has to wait");
        car(1, false, 6);
        cabinButton(1, 8);
        step();
        check("no service to floor 8 yet", !has(m, "service 1 8"));
        car(1, true, 6);
        step();
        check("car 1 stops at 6, service car 1 -> floor 8", has(m, "service 1 8"));

        System.out.println("\nTEST 5: car 2 stops at floor 7 -> the floor 7 down button is cleared");
        m = mark();
        car(2, true, 7);
        step();
        check("resetFloor 7 DOWN", has(m, "resetFloor 7 DOWN"));

        System.out.println("\nTEST 6: hall button, floor 9 up -> free cars are 2 (floor 7), 3 and 4 (floor 1), so car 2");
        m = mark();
        hallButton(9, CCManager.Direction.UP);
        step();
        check("service car 2 -> floor 9", has(m, "service 2 9"));

        System.out.println("\nTEST 7: car 4 has stops 9, 3, 5 -> after floor 9 it goes to 5 (closest), not 3 (first pressed)");
        m = mark();
        cabinButton(4, 9);
        step();
        check("service car 4 -> floor 9", has(m, "service 4 9"));
        cabinButton(4, 3);
        cabinButton(4, 5);
        step();
        car(4, true, 9);
        step();
        check("service car 4 -> floor 5", has(m, "service 4 5"));
        check("floor 3 not sent first", !has(m, "service 4 3"));
        car(4, true, 5);
        step();
        check("then service car 4 -> floor 3", has(m, "service 4 3"));

        System.out.println("\nTEST 8: fire on -> clear every button, all cars to floor 1");
        m = mark();
        fire.fire = true;
        step();
        check("all cabin buttons reset", has(m, "resetAllCabinButtons"));
        check("all floor buttons reset", has(m, "resetAllFloorButtons"));
        for (int c = 1; c <= CARS; c++) {
            check("service car " + c + " -> floor 1", has(m, "service " + c + " 1"));
        }

        System.out.println("\nTEST 9: during fire, a cabin button is ignored");
        m = mark();
        cabinButton(3, 5);
        step();
        check("no service sent", !has(m, "service 3 5"));

        System.out.println("\nTEST 10: fire reset -> normal requests work again");
        fire.fire = false;
        step();
        check("the button pressed during the fire is not sent afterwards", !has(m, "service 3 5"));
        m = mark();
        cabinButton(4, 3);
        step();
        check("service car 4 -> floor 3", has(m, "service 4 3"));
        car(4, true, 3);
        step();

        System.out.println("\nSetup: cars 1,2,3,4 are stopped at floors 3, 5, 8, 10, all sensors clear");
        int[] floors = {0, 3, 5, 8, 10};
        for (int c = 1; c <= CARS; c++) {
            car(c, true, floors[c]);
            sensors(c, false, false);
        }
        step();

        System.out.println("\nTEST 11: cabin button for the floor the car is already stopped at -> ignored, button cleared");
        m = mark();
        cabinButton(1, 3);
        step();
        check("resetButton car 1 floor 3", has(m, "resetButton 1 3"));
        check("no service sent", !has(m, "service 1 3"));

        System.out.println("\nTEST 12: car 3 is obstructed -> hall button floor 8 goes to the next closest (car 4)");
        m = mark();
        sensors(3, true, false);
        hallButton(8, CCManager.Direction.UP);
        step();
        check("service car 4 -> floor 8", has(m, "service 4 8"));

        System.out.println("\nTEST 13: a cabin button in the obstructed car is kept, and sent once the car is clear");
        m = mark();
        cabinButton(3, 2);
        step();
        check("no service while obstructed", !has(m, "service 3 2"));
        sensors(3, false, false);
        step();
        check("service car 3 -> floor 2 after it is clear", has(m, "service 3 2"));

        System.out.println("\nTEST 14: no elevator available -> the hall button waits, then goes to the first one that is");
        m = mark();
        sensors(1, false, true);
        sensors(2, true, false);
        sensors(3, true, false);
        sensors(4, true, false);
        hallButton(6, CCManager.Direction.DOWN);
        step();
        check("no service for floor 6 yet", !has(m, "service 1 6") && !has(m, "service 2 6")
                && !has(m, "service 3 6") && !has(m, "service 4 6"));
        sensors(2, false, false);
        step();
        check("car 2 is clear, service car 2 -> floor 6", has(m, "service 2 6"));

        System.out.println("\nTEST 15: sweep rule: car 1 is going up with stops 3 and 8 waiting -> 8 first (not the nearer 3), then 3");
        sensors(1, false, false);
        step();
        m = mark();
        cabinButton(1, 5);
        step();
        check("service car 1 -> floor 5", has(m, "service 1 5"));
        car(1, false, 4);   // car 1 has left floor 3
        cabinButton(1, 3);
        cabinButton(1, 8);
        step();
        car(1, true, 5);
        step();
        check("service car 1 -> floor 8", has(m, "service 1 8"));
        check("floor 3 not sent first", !has(m, "service 1 3"));
        car(1, true, 8);
        step();
        check("then service car 1 -> floor 3", has(m, "service 1 3"));

        System.out.println();
        System.out.println(failures == 0 ? "ALL TESTS PASSED" : failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
