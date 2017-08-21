package tarehart.rlbot.steps;

import com.sun.javafx.geom.Vec2f;
import com.sun.javafx.geom.Vec3f;
import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.planning.SteerUtil;

import java.time.Duration;
import java.time.LocalDateTime;

public class ChaseBallStep implements Step {
    public static final int MAX_SPEED = 52;
    private static final double BALL_RADIUS = 1.8555;
    private static final double GRAVITY = 1;
    private static final double DRAG = 1;

    private Duration duration;
    private LocalDateTime scheduledEndTime;

    public ChaseBallStep(Duration duration) {
        this.duration = duration;
    }

    public AgentOutput getOutput(AgentInput input) {

        double carSpeed = Math.max(input.getMyVelocity().magnitude(), 30);
        carSpeed *= 1.2;
        carSpeed = Math.min(carSpeed, MAX_SPEED);
        Vector2 aimSpot = leadTarget(input.getMyPosition(), input.ballPosition, input.ballVelocity, carSpeed);
        Vector3 aimSpot3 = new Vector3(aimSpot.x, aimSpot.y, 0);

        double eta = aimSpot.magnitude() / carSpeed;
        double predictedBallHeight = predictBallHeight(input, eta);

        //System.out.println("Predicted height: " + predictedBallHeight);

//        if (aimSpot.distance(new Vector2(input.getMyPosition().x, input.getMyPosition().y)) < 1) {
//            System.out.println("Actual height: " + input.ballPosition.z);
//        }

        return SteerUtil.steerTowardPosition(input, aimSpot3);
    }

    private double predictBallHeight(AgentInput input, double eta) {
        // Simulate ball bouncing.
        double pos = input.ballPosition.z;
        double vel = input.ballVelocity.z;

        for (int i = 0; i < (int) eta; i++) {
            pos += vel;
            vel -= GRAVITY;
            vel *= DRAG;
            if (pos - BALL_RADIUS < 0) {
                pos = BALL_RADIUS;
                vel = Math.abs(vel);
            }
        }

        return pos;
    }


    // https://gamedev.stackexchange.com/questions/35859/algorithm-to-shoot-at-a-target-in-a-3d-game?rq=1
    public Vector2 leadTarget(Vector3 playerPosition, Vector3 ballPosition, Vector3 ballVelocity, double carSpeed) {
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
        if (t1 > t2 && t2 > 0) {
            t = t2;
        } else {
            t = t1;
        }

        Vector2 aimSpot = (Vector2) targetPosition.addMultipleCopy(targetVelocity, t);

        return  aimSpot;
    }


    @Override
    public boolean isComplete() {
        return scheduledEndTime != null && LocalDateTime.now().isAfter(scheduledEndTime);
    }

    @Override
    public void begin() {
        scheduledEndTime = LocalDateTime.now().plus(duration);
    }
}
