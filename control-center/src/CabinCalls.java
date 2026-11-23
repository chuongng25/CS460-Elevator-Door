import java.util.ArrayDeque;

public class CabinCalls
{
	private static final int NUM_CARS = 4;
	private static final int NUM_FLOORS = 10;
	private static final int CABIN_CALL_TOPIC = 0X00; //todo need to define since its not in message catalog
	private static final int CLEAR_CALLS_TOPIC = 0x00 //todo this too
	private static int payloadFloor=0;
	private CCBus bus;
	private final ArrayDeque<Integer>[] pendingCalls;
	private final boolean[][] buttonLit;

	public CabinCalls(CCBus bus)
	{
		this.bus = bus;
		this.pendingCalls = new ArrayDeque[NUM_CARS +1];
		this.buttonLit = new boolean[NUM_CARS+1][NUM_FLOORS+1];

		for(int i = 1; i <= NUM_CARS; i++)
		{
			pendingCalls[i] = new ArrayDeque<>();
		}

		//todo subscribe to bus
	}

	public synchronized int getNewRequest(int cabin)
	{

	}

	public synchronized void reset(int cabin, int floor, boolean all)
	{

	}

	public synchronized void addMessage(Message m) //todo message comes from T03 bus, need ot integrate
	{
		int cabin = m.subtopic;
		int floor = m.bodyByte(payloadFloor);

		if(!verifyCabin(cabin) || !verifyFloor(floor))
		{
			return; //bad message
		}

		if(buttonLit[cabin][floor])
		{
			return; //request already exists, no need to add agian
		}

		buttonLit[cabin][floor] = true; //button remains lit
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


}
