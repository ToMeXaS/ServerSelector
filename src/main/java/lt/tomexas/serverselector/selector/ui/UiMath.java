package lt.tomexas.serverselector.selector.ui;

/** Pure helpers for turning head rotation into a cursor position on the UI plane. */
public final class UiMath {

    /** Distance from the camera to the plane all UI coordinates are given on, in blocks. */
    public static final double REFERENCE_DISTANCE = 2.0;
    /** One font pixel of a text display at scale 1, in blocks. */
    public static final double PIXEL = 1.0 / 40.0;
    // tan() runs off to infinity near 90 degrees
    private static final double MAX_ANGLE = 80.0;

    private UiMath() {
    }

    /** Wraps an angle difference into [-180, 180). */
    public static double wrapDegrees(double degrees) {
        double wrapped = (degrees + 180.0) % 360.0;
        if (wrapped < 0) wrapped += 360.0;
        return wrapped - 180.0;
    }

    /** Where a line of sight turned by the given angle hits the reference plane. */
    public static double project(double degrees) {
        return REFERENCE_DISTANCE * Math.tan(Math.toRadians(clamp(degrees, -MAX_ANGLE, MAX_ANGLE)));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
