package tarehart.rlbot.math;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;

import java.util.Optional;

public class VectorUtil {

    public static Vector3 project(Vector3 vector, Vector3 onto) {
        double scale = vector.dotProduct(onto) / onto.magnitudeSquared();
        return (Vector3) onto.scaleCopy(scale);
    }

    public static Vector2 project(Vector2 vector, Vector2 onto) {
        double scale = vector.dotProduct(onto) / onto.magnitudeSquared();
        return (Vector2) onto.scaleCopy(scale);
    }

    public static Vector2 flatten(Vector3 vector3) {
        return new Vector2(vector3.x, vector3.y);
    }

    public static double flatDistance(Vector3 a, Vector3 b) {
        return flatten(a).distance(flatten(b));
    }

    public static Optional<Vector3> getPlaneIntersection(Vector3 planePosition, Vector3 planeNormal, Vector3 segmentPosition, Vector3 segmentVector) {
        // get d value
        double d = planeNormal.dotProduct(planePosition);

        if (planeNormal.dotProduct(segmentVector) == 0) {
            return Optional.empty(); // No intersection, the line is parallel to the plane
        }

        // Compute the X value for the directed line ray intersecting the plane
        double x = (d - planeNormal.dotProduct(segmentPosition)) / planeNormal.dotProduct(segmentVector);

        // output contact point
        Vector3 intersection = segmentPosition.addCopy(segmentVector.scaleCopy(x));

        if (intersection.distance(segmentPosition) > segmentVector.magnitude()) {
            return Optional.empty();
        }

        return Optional.of(intersection);
    }

    public static Vector2 rotateVector(Vector2 vec, double radians) {
        return new Vector2(
                vec.x * Math.cos(radians) - vec.y * Math.sin(radians),
                vec.x * Math.sin(radians) + vec.y * Math.cos(radians));
    }

}
