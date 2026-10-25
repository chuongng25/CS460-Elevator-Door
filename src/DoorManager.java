/**
 * Bridge / Driver implementing DoorApi over a TCP Socket connection.
 */
public class DoorManager implements DoorApi {

    public void connect(String host, int port) {
        throw new UnsupportedOperationException("DoorManager.connect() not implemented yet");
    }

    public void disconnect() {
        throw new UnsupportedOperationException("DoorManager.disconnect() not implemented yet");
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException("DoorManager.open() not implemented yet");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("DoorManager.close() not implemented yet");
    }

    @Override
    public boolean isFullyOpen() {
        throw new UnsupportedOperationException("DoorManager.isFullyOpen() not implemented yet");
    }

    @Override
    public boolean isFullyClosed() {
        throw new UnsupportedOperationException("DoorManager.isFullyClosed() not implemented yet");
    }

    @Override
    public boolean hasObstruction() {
        throw new UnsupportedOperationException("DoorManager.hasObstruction() not implemented yet");
    }
}
