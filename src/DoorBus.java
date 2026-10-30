/**
 * Team 01's side of the T03 Software Bus message format. This file contains no
 * networking: the door only builds and reads 8-byte messages, and whatever
 * connects the door to the bus (the "transport") carries them.
 *
 * Every message is 8 bytes: [topic][subtopic][6-byte big-endian body].
 * The bus knows nothing about doors; the subtopic is the elevator number
 * (1..255, 0 = all elevators).
 *
 * Door topics (DOOR_STATE is the agreed T03 topic; the others are Team 01's own
 * numbers and still need to be added to the shared T03 topic table):
 *   7  DOOR_STATE        DoorManager -> bus   body 0 closed, 1 opening, 2 open, 3 closing
 *   8  DOOR_COMMAND      bus -> DoorManager   body 1 = open, 0 = close
 *   9  DOOR_OBSTRUCTION  DoorManager -> bus   body 1 = blocked, 0 = clear
 *
 * What the transport must deliver to DoorManager.onMessage() for its car number:
 *   topic 8 (DOOR_COMMAND)
 */
public final class DoorBus {

    public static final int TOPIC_DOOR_STATE = 7;
    public static final int TOPIC_DOOR_COMMAND = 8;
    public static final int TOPIC_DOOR_OBSTRUCTION = 9;

    public static final int SIZE = 8;
    private static final long MAX_VALUE = (1L << 48) - 1;

    private DoorBus() {
    }

    /**
     * Where the door's outgoing 8-byte messages go. The bus layer supplies this:
     * send() should publish the message on the Software Bus.
     */
    @FunctionalInterface
    public interface Link {
        void send(byte[] message);
    }

    /**
     * Builds the 8-byte wire form of a message. The value fills the 6-byte body.
     */
    public static byte[] encode(int topic, int subtopic, long value) {
        if (topic < 0 || topic > 255 || subtopic < 0 || subtopic > 255 || value < 0 || value > MAX_VALUE) {
            throw new IllegalArgumentException("message field out of range: " + topic + "/" + subtopic + "/" + value);
        }
        byte[] wire = new byte[SIZE];
        wire[0] = (byte) topic;
        wire[1] = (byte) subtopic;
        for (int i = SIZE - 1; i >= 2; i--) {
            wire[i] = (byte) (value & 0xFF);
            value >>>= 8;
        }
        return wire;
    }

    private static void check(byte[] message) {
        if (message == null || message.length != SIZE) {
            throw new IllegalArgumentException("message must be " + SIZE + " bytes");
        }
    }

    public static int topic(byte[] message) {
        check(message);
        return message[0] & 0xFF;
    }

    public static int subtopic(byte[] message) {
        check(message);
        return message[1] & 0xFF;
    }

    /**
     * The 6-byte body read as a big-endian unsigned number.
     */
    public static long value(byte[] message) {
        check(message);
        long v = 0;
        for (int i = 2; i < SIZE; i++) {
            v = (v << 8) | (message[i] & 0xFF);
        }
        return v;
    }
}
