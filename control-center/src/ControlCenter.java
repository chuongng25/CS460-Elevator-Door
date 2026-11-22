// Main: creates the Manager and starts it. No logic here.
public class ControlCenter {

    // Builds the Manager from the four objects and starts its control loop.
    public static CCManager start(CCManager.FireAlarmApi fireAlarm, CCManager.CabinCallsApi cabinCalls,
                                  CCManager.FloorPanelsApi floorPanels, CCManager.ElevatorsApi elevators) {
        CCManager manager = new CCManager(fireAlarm, cabinCalls, floorPanels, elevators);
        manager.start();
        return manager;
    }

    public static void main(String[] args) throws Exception {
        // TODO: create the four objects here once they exist (each one connects to the Software Bus),
        // then pass them to start(...):
        //     CCManager manager = start(fireAlarm, cabinCalls, floorPanels, elevators);
        //     Thread.currentThread().join();
        System.out.println("Control Center: the four objects are not built yet, nothing to start.");
    }
}
