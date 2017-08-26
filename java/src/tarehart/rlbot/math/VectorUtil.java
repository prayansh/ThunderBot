package tarehart.rlbot.math;

import mikera.vectorz.Vector3;

public class VectorUtil {

    public static Vector3 project(Vector3 vector, Vector3 onto) {
        double scale = vector.dotProduct(onto) / onto.magnitudeSquared();
        return (Vector3) onto.scaleCopy(scale);
    }

}
