package lt.tomexas.serverselector.selector.screen;

/** A layer of the selector UI that reacts to the cursor. Coordinates are on the UI reference plane. */
public interface Screen {

    /** Runs every tick for animations. */
    default void tick() {
    }

    /** Updates hover state; returns true when the cursor is over something clickable. */
    boolean hover(double x, double y);

    void click(double x, double y);

    void close();
}
