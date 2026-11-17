//just use DoorBus with these values for the new stuff like buttons and different modes
public final class CCBus {

    public static final int HALL_BUTTON = 20;
    public static final int CABIN_BUTTON = 21;
    public static final int FIRE = 22;
    public static final int CAR_POSITION = 23;
    public static final int SERVICE = 24;
    public static final int GOTO = 25;
    public static final int CLEAR = 26;
    public static final int RESET = 27;
    public static final int OVERWEIGHT = 28;

    public static final int IDLE = 0;
    public static final int UP = 1;
    public static final int DOWN = 2;
    //reset hall buttons
    public static final int BOTH = 3;
    //for resetting the buttons (applies to all buttons)
    public static final int ALL = 0xFFFF;

    private CCBus() {
    }
}
