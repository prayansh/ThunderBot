package tarehart.rlbot.math;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;

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

}
