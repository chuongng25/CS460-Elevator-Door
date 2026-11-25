import Utilities.Message;
import Utilities.Topics
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.OptionalInt;

public class CabinCalls
{
	public static final int NUM_CARS = 4;
	public static final int NUM_FLOORS = 10;
	public static final int MIN_FLOOR = 1;
	public static final int MAX_FLOOR = NUM_FLOORS;
	public static final int TOPIC_CABIN_CALL = 0X00; //todo need to define since its not in message catalog
	private static final int TOPIC_CLEAR_BUTTONS = 0x00 //todo this too
	private static int payloadFloor=0;
	private CCBus bus; //todo needs to become T03 software bus
	private final ArrayDeque<Integer>[] pendingCalls;
	private final boolean[][] lit;

	/*
	From T02 Passener Facing Devices Elevator Panel
	 */
	private final ElevatorPanelGUI[] gui;
	private final boolean[] keyInserted;
	private final boolean[] buzzerOn;

	public CabinCalls(CCBus bus)
	{
		this.bus = bus;
		this.pendingCalls = new ArrayDeque[NUM_CARS +1];
		this.lit = new boolean[NUM_CARS+1][NUM_FLOORS+1];
		this.gui = new ElevatorPanelGUI[NUM_CARS+1];
		this.keyInserted = new boolean[NUM_CARS +1];
		this.buzzerOn = new boolean[NUM_CARS+1];


		for(int i = 1; i <= NUM_CARS; i++)
		{
			final int cabin = i;
			gui[i] = new ElevatorPanelGUI(cabin,
							floor -> pressFloorButton(cabin,floor),
							inserted-> setKeyInserted(cabin, inserted),
							on -> setBuzzerOn(cabin, on));
			pendingCalls[i] = new ArrayDeque<>();
		}

		//todo subscribe to bus
	}

	/**
	 * Reads the next unread destination floor selected by the given cabin.
	 * Oldest requests will be read first.
	 * OptionalInt- may have null or empty value if no floor is selected.
	 * @param cabin - cabin requesting the service
	 * @return - the floor requested, or empty if none.
	 */
	public synchronized OptionalInt getNewRequest(int cabin)
	{
		checkCabin(cabin);
		Integer floor = pendingCalls[cabin].poll();
		return floor == null ? OptionalInt.empty() : OptionalInt.of(floor);
	}

	/**
	 * Tunrs of the specific cabin button when the cabin arrives at that floor
	 * and clears the stored values.  then it publishes a CLEAR_BUTTONS message to
	 * the bus.
	 * @param cabin - the cabin servicing the request
	 * @param floor - the floor button to be reset
	 */
	public synchronized void reset(int cabin, int floor)
	{
		checkCabin(cabin);
		checkFloor(floor);
		lit[cabin][floor] = false;

//		gui[cabin].setFloorButtonStatus(floor,false);
		pendingCalls[cabin].remove(Integer.valueOf(floor));
		publishClear(cabin,floor);
	}

	/**
	 * Turn off every button in every cabin (used for firemode)
	 * will send a CLEAR_BUTTON message for each one (40 total)
	 */
	public synchronized void resetAll()
	{
		for (int i = 1; i <= NUM_CARS; i++)
		{
			pendingCalls[i].clear();
			Arrays.fill(lit[i], false);
			for (int j = 1; j <= NUM_FLOORS; j++)
			{
				gui[j].setFloorButtonStatus(j, false);
				publishClear(i,j);
			}
		}
	}

	public synchronized void addMessage(Message m) //todo message comes from T03 bus, need ot integrate
	{
		int cabin = m.subtopic;
		int floor = m.bodyByte(payloadFloor);

		if(!verifyCabin(cabin) || !verifyFloor(floor))
		{
			return; //bad message
		}

		if(lit[cabin][floor])
		{
			return; //request already exists, no need to add agian
		}

		lit[cabin][floor] = true; //button remains lit
		pendingCalls[cabin].add(floor); //floor is added to queue of requests to service.
	}

	private boolean verifyCabin(int cabin)
	{
		if(cabin < 1 || cabin > NUM_CARS)
		{
			return false;
		}
		return true;
	}

	private boolean verifyFloor(int floor)
	{
		if(floor<1 || floor > NUM_FLOORS)
		{
			return false;
		}
		return true;
	}

	/**
	 * T02 Passenger Facing Devices- Elevator Panel Code
	 */

	public synchronized boolean btnPressed(int cabin, int floor)
	{
		checkCabin(cabin);
		checkFloor(floor);
		return lit[cabin][floor];
	}

	public synchronized void setKeyInserted(int cabin, boolean inserted)
	{
		checkCabin(cabin);
		keyInserted[cabin] = inserted;
		gui[cabin].setKeyInserted(inserted);
	}

	public void ding(int cabin)
	{
		checkCabin(cabin);
		SoundEffects.playDing();
		gui[cabin].flashBuzzerImage();
	}

	public synchronized void playBuzz(int cabin)
	{
		checkCabin(cabin);
		if (buzzerOn[cabin]) return;

		buzzerOn[cabin] = true;
		SoundEffects.startBuzzer();
		gui[cabin].setBuzzerActive(true);
	}

	public synchronized void stopBuzz(int cabin)
	{
		checkCabin(cabin);
		if (!buzzerOn[cabin]) return;

		buzzerOn[cabin] = false;
		SoundEffects.stopBuzzer();
		gui[cabin].setBuzzerActive(false);
	}

	public synchronized boolean isBuzzerOn(int cabin)
	{
		checkCabin(cabin);
		return buzzerOn[cabin];
	}

	public void updateFloorDisplay(int cabin, int newFloor)
	{
		checkCabin(cabin);
		checkFloor(newFloor);
		gui[cabin].updateFloorDisplay(newFloor);
	}

	public void updateDirDisplay(int cabin, Direction newDirection)
	{
		checkCabin(cabin);
		gui[cabin].setDirection(Objects.requireNonNull(newDirection, "Direction cannot be null."));
	}

	private synchronized void pressFloorButton(int cabin, int floor)
	{
		checkFloor(floor);
		registerPress(cabin, floor);
		// The bus may echo this back to onMessage; the lit check there ignores the repeat.
		bus.publish(new Message(TOPIC_CABIN_CALL, cabin, (byte) floor));
	}

	private void setBuzzerOn(int cabin, boolean on)
	{
		if (on) {
			playBuzz(cabin);
		} else {
			stopBuzz(cabin);
		}
	}

	private void registerPress(int cabin, int floor)
	{
		if (lit[cabin][floor]) {
			return; // button already lit, repeat press adds nothing
		}
		lit[cabin][floor] = true;
		gui[cabin].setFloorButtonStatus(floor, true);
		pendingCalls[cabin].add(floor);
	}

	// Bus callback: runs on the bus's thread, hence synchronized.
	private synchronized void onMessage(Message m)
	{
		int cabin = m.subtopic();
		int floor = m.bodyByte(PAYLOAD_FLOOR);
		if (cabin < 1 || cabin > NUM_CARS || floor < 1 || floor > NUM_FLOORS) {
			return; // malformed message: ignore
		}
		registerPress(cabin, floor);
	}

	private void publishClear(int cabin, int floor)
	{
		bus.publish(new Message(TOPIC_CLEAR_BUTTONS, cabin, (byte) floor));
	}
	// check that the cabin number is valid
	private static void checkCabin(int cabin)
	{
		if (cabin < 1 || cabin > NUM_CARS) {
			throw new IllegalArgumentException("Cabin must be 1-" + NUM_CARS + ": " + cabin);
		}
	}

	// check that the floor number is valid (from ElevatorPanel.checkFloor)
	private static void checkFloor(int floor)
	{
		if (floor < MIN_FLOOR || floor > MAX_FLOOR) {
			throw new IllegalArgumentException(
							"Floor number must be between " + MIN_FLOOR + " and " + MAX_FLOOR + ".");
		}
	}

}
