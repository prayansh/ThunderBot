package tarehart.rlbot.math;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LeadTargetUtil {

    /**
     * Returns a list of locations where we can intercept the ball with our current speed.
     */
    public static List<Vector2> leadTarget(Vector3 playerPosition, Vector3 ballPosition, Vector3 ballVelocity, double carSpeed) {
        Vector2 basePosition = new Vector2(playerPosition.x, playerPosition.y);
        Vector2 targetPosition = new Vector2(ballPosition.x, ballPosition.y);
        Vector2 targetVelocity = new Vector2(ballVelocity.x, ballVelocity.y);

        Vector2 toTarget = (Vector2) targetPosition.subCopy(basePosition);

        double a = targetVelocity.dotProduct(targetVelocity) - carSpeed * carSpeed;
        double b = 2 * targetVelocity.dotProduct(toTarget);
        double c = toTarget.dotProduct(toTarget);

        double p = -b / (2 * a);
        double q = Math.sqrt(b * b - 4 * a * c) / (2 * a);

        double t1 = p - q;
        double t2 = p + q;

        double t;

        double tEarly = Math.min(t1, t2);
        double tLate = Math.max(t1, t2);

        List<Vector2> intercepts = new ArrayList<>();
        if (tEarly > 0) {
            intercepts.add((Vector2) targetPosition.addMultipleCopy(targetVelocity, tEarly));
        }

        if (tLate > 0) {
            intercepts.add((Vector2) targetPosition.addMultipleCopy(targetVelocity, tLate));
        }

        return intercepts;
    }

}
