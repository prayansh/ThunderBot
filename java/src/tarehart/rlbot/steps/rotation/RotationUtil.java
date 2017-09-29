package tarehart.rlbot.steps.rotation;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.math.VectorUtil;

import java.time.Duration;

public class RotationUtil {

    public static Duration getStartingImpulse(double radians) {
        return Duration.ofMillis((long) (Math.abs(radians) * 200));
    }

    public static Duration getHaltingImpulse(double radians) {
        return Duration.ofMillis((long) (radians * 100));
    }

    public static double maxOrbitHeightAbovePlane(Vector3 axisOfRotation, Vector3 planeNormal) {
        double xVal = projectOntoPlane(axisOfRotation, planeNormal).magnitude();
        double angleAbovePlane = Math.acos(xVal);
        return Math.cos(angleAbovePlane);
    }

    private static Vector3 projectOntoPlane(Vector3 vec, Vector3 planeNormal) {
        Vector3 projection = vec.clone();
        projection.projectToPlane(planeNormal, 0);
        return projection;
    }

    public static double shortWay(double radians) {

        radians = radians % (Math.PI * 2);
        if (radians > Math.PI) {
            radians -= Math.PI * 2;
        }
        return radians;
    }

}
