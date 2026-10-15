package lt.tomexas.serverselector.Queue;

/** A snapshot of a player's ajQueue state. */
public record QueueState(boolean available, boolean inQueue, String server, int position, int total) {

    public static final QueueState NOT_QUEUED = new QueueState(true, false, null, 0, 0);
    public static final QueueState UNAVAILABLE = new QueueState(false, false, null, 0, 0);

    public static QueueState queued(String server, int position, int total) {
        return new QueueState(true, true, server, position, total);
    }

    /** How far along the queue the player is: 1.0 at the front, approaching 0 at the back. */
    public double progress() {
        if (total <= 0 || position <= 0 || position > total) return 0.0;
        return (double) (total - position + 1) / total;
    }
}
