package lt.tomexas.serverselector.selector.ui;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * A plane facing the camera. UI coordinates are blocks on the plane at {@link UiMath#REFERENCE_DISTANCE},
 * x to the right and y up from its centre.
 */
public final class UiPlane {

    private final Location eye;
    private final Vector forward;
    private final Vector right;
    private final Vector up;

    public UiPlane(Location eye) {
        this.eye = eye.clone();
        this.forward = eye.getDirection().normalize();
        this.right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        this.up = right.clone().crossProduct(forward).normalize();
    }

    /** World position of (x, y), drawn at the given distance from the eye on the same line of sight. */
    public Location point(double x, double y, double depth) {
        double k = depth / UiMath.REFERENCE_DISTANCE;
        return eye.clone()
                .add(forward.clone().multiply(depth))
                .add(right.clone().multiply(x * k))
                .add(up.clone().multiply(y * k));
    }

    public float yaw() {
        return eye.getYaw();
    }
}
