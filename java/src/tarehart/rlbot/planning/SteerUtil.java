package tarehart.rlbot.planning;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.SplineHandle;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.tuning.Telemetry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class SteerUtil {

    public static final int MAX_SPEED = 42;
    public static final double GOOD_ENOUGH_ANGLE = Math.PI / 40;
    private static final ArenaModel arenaModel = new ArenaModel();


    public static SpaceTime predictBallInterceptFlat(AgentInput input) {

        double ballDistance = input.ballPosition.distance(input.getMyPosition());
        int maxSpeedWeight = (int) (ballDistance / 20);
        double currentSpeed = input.getMyVelocity().magnitude();

        double predictedAverageSpeed = (maxSpeedWeight * MAX_SPEED + currentSpeed) / (maxSpeedWeight + 1);

        Optional<Vector2> aimSpot = leadTarget(input.getMyPosition(), input.ballPosition, input.ballVelocity, predictedAverageSpeed);

        if (!aimSpot.isPresent() || !ArenaModel.isInBoundsBall(aimSpot.get())) {

            System.out.println("Simulating wall bounce!");
            BallPath ballPath = predictBallPath(input, input.time, Duration.ofSeconds(3));

            Optional<SpaceTimeVelocity> stvOption = ballPath.getMotionAfterBounce(1);
            if (stvOption.isPresent()) {
                SpaceTimeVelocity stv = stvOption.get();
                Vector3 postBounceVelocity = stv.velocity;
                Duration timeTillBounce = Duration.between(input.time, stv.getTime());
                double timeTillBounceInSeconds = timeTillBounce.toMillis() * 1000.0;

                // Now back it up 3 seconds
                Vector3 backwards = (Vector3) postBounceVelocity.scaleCopy(-timeTillBounceInSeconds);
                Vector3 imaginaryStartingPoint = stv.getSpace().addCopy(backwards); // This is probably out of bounds
                aimSpot = leadTarget(input.getMyPosition(), imaginaryStartingPoint, postBounceVelocity, predictedAverageSpeed);
                if (!aimSpot.isPresent() || !ArenaModel.isInBoundsBall(aimSpot.get())) {
                    // Ugh. Our bounce calculat
                    return stv.spaceTime;
                }
            } else {
                // Double ugh. We have no clue, so just chase the ball for now.
                aimSpot = Optional.of(new Vector2(input.ballPosition.x, input.ballPosition.y));
            }

        }

        double etaSeconds = aimSpot.get().magnitude() / predictedAverageSpeed;
        long etaMillis = (long) (etaSeconds * 1000);

        return new SpaceTime(new Vector3(aimSpot.get().x, aimSpot.get().y, 0), input.time.plus(Duration.ofMillis(etaMillis)));
    }

    public static SpaceTime predictBallIntercept(AgentInput input, SpaceTime flatIntercept) {

        double predictedBallHeight = predictBallHeight(input, flatIntercept.time);

        return new SpaceTime(new Vector3(flatIntercept.space.x, flatIntercept.space.y, predictedBallHeight), flatIntercept.time);
    }

    private static double predictBallHeight(AgentInput input, LocalDateTime moment) {
        System.out.println("Simulating height for potential aerial!");
        return predictBallPosition(input, moment).z;
    }

    private static BallPath predictBallPath(AgentInput input, LocalDateTime startingAt, Duration duration) {
        Telemetry telemetry = Telemetry.forTeam(input.team);

        if (telemetry.getBallPath() == null) {
            telemetry.setBallPath(arenaModel.simulateBall(input.ballPosition, input.ballVelocity, startingAt, duration));
            return telemetry.getBallPath();
        }

        if (telemetry.getBallPath().getEndpoint().time.isBefore(startingAt.plus(duration))) {
            arenaModel.extendSimulation(telemetry.getBallPath(), startingAt, duration);
        }
        return telemetry.getBallPath();
    }

    private static Vector3 predictBallPosition(AgentInput input, LocalDateTime targetTime) {
        BallPath ballPath = predictBallPath(input, input.time, Duration.between(input.time, targetTime));
        return ballPath.getMotionAt(targetTime).get().getSpace();
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
