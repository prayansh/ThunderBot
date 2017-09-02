package tarehart.rlbot.math;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LeadTargetUtil {

    /**
     * Returns a list of locations where we can intercept the ball's shadow on the ground with our current speed.
     */
    public static List<SpaceTime> leadTarget(Vector3 playerPosition, Vector3 ballPosition, Vector3 ballVelocity, double carSpeed, LocalDateTime startTime) {
        Vector2 basePosition = VectorUtil.flatten(playerPosition);
        Vector2 targetPosition = VectorUtil.flatten(ballPosition);
        Vector2 targetVelocity = VectorUtil.flatten(ballVelocity);
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

        List<SpaceTime> intercepts = new ArrayList<>();
        if (tEarly > 0) {
            Vector2 space = (Vector2) targetPosition.addMultipleCopy(targetVelocity, tEarly);
            intercepts.add(new SpaceTime(new Vector3(space.x, space.y, 0), startTime.plus(Duration.ofMillis((long) (tEarly * 1000)))));
        }

        if (tLate > 0) {
            Vector2 space = (Vector2) targetPosition.addMultipleCopy(targetVelocity, tLate);
            intercepts.add(new SpaceTime(new Vector3(space.x, space.y, 0), startTime.plus(Duration.ofMillis((long) (tLate * 1000)))));
        }

        return intercepts;
    }

}
