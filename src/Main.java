public class Main {

    //wait until the door stops moving. a full open/close is 2 seconds,
    //so 2.5 seconds is enough for every test
    private static final long SETTLE_MS = 2500;

    public static void main(String[] args) throws InterruptedException {
        DoorTwin twin = new DoorTwin(1);
        DoorApi door = twin;
        System.out.println("NO GUI TEST (car " + twin.getCarId() + ")");
        System.out.println();
        //doors are closed so using the isFullyClosed method
        if (door.isFullyClosed()) {
            System.out.println("PASS  starts fully closed");
        } else {
            System.out.println("FAIL  doesnt starts fully closed");
        }
        //door fully open so using the doorFullyOpen() method
        door.open();
        Thread.sleep(SETTLE_MS);
        if (door.isFullyOpen()) {
            System.out.println("PASS  opens fully");
        } else {
            System.out.println("FAIL  doesnt opens fully");
        }
        //top sensor is tripped so something obstructed the door

        //start with the door closed
        door.close();
        //wait until the doors about a quarter shut to test
        Thread.sleep(500);
        //the beam is now broken, so set it to true
        twin.setBeamBroken(true);

        Thread.sleep(SETTLE_MS);
        if (door.isFullyOpen()) {
            System.out.println("PASS  opened when obstructed");
        } else {
            System.out.println("FAIL  did not open when obstructed");
        }
        twin.setBeamBroken(false);

        //same as test 3 basically
        door.close();
        Thread.sleep(500);
        twin.setSillLoaded(true);
        //let the door either open or close
        Thread.sleep(SETTLE_MS);
        //passes if its open cuz it was obstructed
        if (door.isFullyOpen()) {
            System.out.println("PASS  opened when obstructed");
            //fails if closed
        } else {
            System.out.println("FAIL  did not open when obstructed");
        }
        twin.setSillLoaded(false);

        //check if it closes
        door.close();
        Thread.sleep(SETTLE_MS);
        if (door.isFullyClosed()) {
            System.out.println("PASS  closes fully once clear");
        } else {
            System.out.println("FAIL  doesnt closes fully once clear");
        }

        twin.stop();
    }
}
