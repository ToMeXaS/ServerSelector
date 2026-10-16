package lt.tomexas.serverselector.server;

/** A server's availability, as found by pinging it. */
public enum ServerStatus {
    ONLINE(true),
    OFFLINE(false),
    // Not pinged yet
    UNKNOWN(false);

    private final boolean joinable;

    ServerStatus(boolean joinable) {
        this.joinable = joinable;
    }

    public boolean joinable() {
        return joinable;
    }
}
