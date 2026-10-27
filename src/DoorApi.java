public interface DoorApi {
    void open();

    void close();

    boolean isFullyOpen();

    boolean isFullyClosed();

    boolean hasObstruction();
}