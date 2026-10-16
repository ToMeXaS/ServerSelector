package lt.tomexas.serverselector.selector.ui;

/**
 * Turns head rotation into a cursor position on the UI plane. When the cursor hits an edge, the reference
 * angle slides along with the head so turning further is ignored and turning back moves the cursor at once.
 */
public final class CursorTracker {

    private final double boundsX;
    private final double boundsY;
    private double anchorYaw;
    private double anchorPitch;
    private double x;
    private double y;

    public CursorTracker(double boundsX, double boundsY, double anchorYaw, double anchorPitch) {
        this.boundsX = boundsX;
        this.boundsY = boundsY;
        this.anchorYaw = anchorYaw;
        this.anchorPitch = anchorPitch;
    }

    /** Updates the cursor from the head's current yaw and pitch (pitch is positive looking down). */
    public void update(double yaw, double pitch) {
        double edgeX = Math.toDegrees(Math.atan(boundsX / UiMath.REFERENCE_DISTANCE));
        double angleX = UiMath.wrapDegrees(yaw - anchorYaw);
        if (angleX > edgeX) {
            // Slide the anchor with the head so turning back moves the cursor right away
            anchorYaw = UiMath.wrapDegrees(yaw - edgeX);
            angleX = edgeX;
        } else if (angleX < -edgeX) {
            anchorYaw = UiMath.wrapDegrees(yaw + edgeX);
            angleX = -edgeX;
        }

        double edgeY = Math.toDegrees(Math.atan(boundsY / UiMath.REFERENCE_DISTANCE));
        // Looking up is positive y, and pitch is positive looking down
        double angleY = anchorPitch - pitch;
        if (angleY > edgeY) {
            anchorPitch = pitch + edgeY;
            angleY = edgeY;
        } else if (angleY < -edgeY) {
            anchorPitch = pitch - edgeY;
            angleY = -edgeY;
        }

        // Guard tiny float overshoot so the edges land exactly on the bounds
        x = UiMath.clamp(UiMath.project(angleX), -boundsX, boundsX);
        y = UiMath.clamp(UiMath.project(angleY), -boundsY, boundsY);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }
}
