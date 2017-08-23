package tarehart.rlbot.math;

import mikera.vectorz.Vector3;

public class SplineHandle {

    // Controls how close you get to a spline handle before you stop going for the handle and start going for
    // the target in the center.
    private static final double RANGE_MULTIPLIER = 1.5;

    private Vector3 location;
    private Vector3 handle1;
    private Vector3 handle2;
    private double handleRange;

    public SplineHandle(Vector3 location, Vector3 relativeHandle1, Vector3 relativeHandle2) {
        this.location = location;
        this.handle1 = location.addCopy(relativeHandle1);
        this.handle2 = location.addCopy(relativeHandle2);

        handleRange = Math.max(relativeHandle1.magnitude(), relativeHandle2.magnitude()) * RANGE_MULTIPLIER;
    }

    public Vector3 getNearestHandle(Vector3 position) {
        double d1 = position.distanceSquared(handle1);
        double d2 = position.distanceSquared(handle2);
        return d1 < d2 ? handle1 : handle2;
    }

    public Vector3 getFarthestHandle(Vector3 position) {
        double d1 = position.distanceSquared(handle1);
        double d2 = position.distanceSquared(handle2);
        return d1 > d2 ? handle1 : handle2;
    }

    public Vector3 getLocation() {
        return location;
    }

    public boolean isWithinHandleRange(Vector3 position) {
        return position.distance(location) < handleRange;
    }
}
