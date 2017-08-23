package tarehart.rlbot.planning;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SplineHandle;
import tarehart.rlbot.physics.ArenaModel;

import javax.vecmath.Vector3f;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class SteerUtil {

    public static final int MAX_SPEED = 52;
    public static final double GOOD_ENOUGH_ANGLE = Math.PI / 40;
    private static final ArenaModel arenaModel = new ArenaModel();


    public static SpaceTime predictBallInterceptFlat(AgentInput input) {

        double ballDistance = input.ballPosition.distance(input.getMyPosition());
        int maxSpeedWeight = (int) (ballDistance / 20);
        double currentSpeed = input.getMyVelocity().magnitude();

        double predictedAverageSpeed = (maxSpeedWeight * MAX_SPEED + currentSpeed) / (maxSpeedWeight + 1);

        Optional<Vector2> aimSpot = leadTarget(input.getMyPosition(), input.ballPosition, input.ballVelocity, predictedAverageSpeed);

        if (!aimSpot.isPresent() || !isInBounds(aimSpot.get())) {

            System.out.println("Simulating wall bounce!");
            Vector3 position = predictBall(input, 3);
            aimSpot = Optional.of(new Vector2(position.x, position.y));
        }

        double etaSeconds = aimSpot.get().magnitude() / predictedAverageSpeed;
        long etaMillis = (long) (etaSeconds * 1000);

        return new SpaceTime(new Vector3(aimSpot.get().x, aimSpot.get().y, 0), LocalDateTime.now().plus(Duration.ofMillis(etaMillis)));
    }

    private static Vector3f toV3f(Vector3 v) {
        return new Vector3f((float) v.x, (float) v.y, (float) v.z);
    }

    private static boolean isInBounds(Vector2 location) {
        return Math.abs(location.x) < 74 && Math.abs(location.y) < 100;
    }

    public static SpaceTime predictBallIntercept(AgentInput input, SpaceTime flatIntercept) {

        long etaMillis = Duration.between(LocalDateTime.now(), flatIntercept.time).toMillis();

        double predictedBallHeight = predictBallHeight(input, etaMillis / 1000.0);

        return new SpaceTime(new Vector3(flatIntercept.space.x, flatIntercept.space.y, predictedBallHeight), flatIntercept.time);
    }

    private static double predictBallHeight(AgentInput input, double etaSeconds) {
        System.out.println("Simulating height for potential aerial!");
        return predictBall(input, (float) etaSeconds).z;
    }

    private static Vector3 predictBall(AgentInput input, float seconds) {
        Vector3f position = arenaModel.simulateBall(toV3f(input.ballPosition), toV3f(input.ballVelocity), seconds);
        return new Vector3(position.x, position.y, position.z);
    }

    public static double getCorrectionAngleRad(AgentInput input, Vector3 position) {
        Vector3 myPosition = input.getMyPosition();
        CarRotation myRotation = input.getMyRotation();

        float playerDirectionRad = (float) Math.atan2(myRotation.noseX, myRotation.noseY);

        float relativeAngleToTargetRad = (float) Math.atan2(position.x - myPosition.x, position.y - myPosition.y);

        if (Math.abs(playerDirectionRad - relativeAngleToTargetRad) > Math.PI) {
            if (playerDirectionRad < 0) {
                playerDirectionRad += Math.PI * 2;
            }
            if (relativeAngleToTargetRad < 0) {
                relativeAngleToTargetRad += Math.PI * 2;
            }
        }

        return relativeAngleToTargetRad - playerDirectionRad;
    }

    public static AgentOutput steerTowardPosition(AgentInput input, Vector3 position) {

        double correctionAngle = getCorrectionAngleRad(input, position);

        float turnSharpness = 1;
        double difference = Math.abs(correctionAngle);
        if (difference < Math.PI / 6) {
            turnSharpness = 0.5f;

            if (difference < GOOD_ENOUGH_ANGLE) {
                turnSharpness = 0;
            }
        }

        boolean shouldBoost = difference < Math.PI / 6 && input.getMyVelocity().magnitude() < MAX_SPEED * .98;

        return new AgentOutput()
                .withAcceleration(1)
                .withSteer((float) (-Math.signum(correctionAngle) * turnSharpness))
                .withSlide(difference > Math.PI / 2)
                .withBoost(shouldBoost);
    }

    public static AgentOutput arcTowardPosition(AgentInput input, SplineHandle position) {
        if (position.isWithinHandleRange(input.getMyPosition())) {
            return SteerUtil.steerTowardPosition(input, position.getLocation());
        } else {
            return SteerUtil.steerTowardPosition(input, position.getNearestHandle(input.getMyPosition()));
        }
    }

    public static double getDistanceFromMe(AgentInput input, Vector3 loc) {
        return loc.distance(input.getMyPosition());
    }

    private static Optional<Vector2> leadTarget(Vector3 playerPosition, Vector3 ballPosition, Vector3 ballVelocity, double carSpeed) {
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

        if (t < 0) {
            return Optional.empty();
        }

        Vector2 aimSpot = (Vector2) targetPosition.addMultipleCopy(targetVelocity, t);

        return Optional.of(aimSpot);
    }

}
