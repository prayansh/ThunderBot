package tarehart.rlbot.planning;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.math.LeadTargetUtil;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.SplineHandle;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.tuning.Telemetry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class SteerUtil {

    public static final int MAX_SPEED = 46;
    public static final int MAX_SPEED_SANS_BOOST = 28;
    public static final double GOOD_ENOUGH_ANGLE = Math.PI / 40;
    private static final ArenaModel arenaModel = new ArenaModel();
    private static final int BOOST_NEEDED_FOR_ZERO_TO_MAX = 60;
    private static final double DISTANCE_NEEDED_FOR_ZERO_TO_MAX_WITH_BOOST = 60;
    private static final double DISTANCE_NEEDED_FOR_ZERO_TO_MAX_SANS_BOOST = 60;
    public static final int PREDICTION_SECONDS = 3;


    private static double distanceNeededForMaxSpeed(double currentSpeed, double boostRemaining) {
        // If we have 60 boost, we can use it to get from 0 to max speed in approx 60 meters.

        double speedNeeded = MAX_SPEED - currentSpeed;

        double proportionGrantedByBoost = (boostRemaining / BOOST_NEEDED_FOR_ZERO_TO_MAX) / (speedNeeded / MAX_SPEED);
        proportionGrantedByBoost = Math.min(proportionGrantedByBoost, 1);

        double distanceFactor = proportionGrantedByBoost * DISTANCE_NEEDED_FOR_ZERO_TO_MAX_WITH_BOOST +
                (1 - proportionGrantedByBoost) * DISTANCE_NEEDED_FOR_ZERO_TO_MAX_SANS_BOOST;

        return distanceFactor * speedNeeded / MAX_SPEED;
    }

    public static double predictAverageSpeed(AgentInput input, double targetDistance) {
        double speed = input.getMyVelocity().magnitude();
        double distanceTillMaxSpeed = distanceNeededForMaxSpeed(speed, input.getMyBoost());

        if (distanceTillMaxSpeed < targetDistance) {
            double averageSpeedFirstSegment = (speed + MAX_SPEED) / 2;
            double accelerationPortion = distanceTillMaxSpeed / targetDistance;
            return averageSpeedFirstSegment * accelerationPortion + MAX_SPEED * (1 - accelerationPortion);
        } else {
            double speedAtTargetDistance = speed + (MAX_SPEED - speed) * targetDistance / distanceTillMaxSpeed;
            return (speed + speedAtTargetDistance) / 2;
        }
    }

    public static SpaceTime predictBallInterceptFlat(AgentInput input) {

        double ballDistance = input.ballPosition.distance(input.getMyPosition());
        double predictedAverageSpeed = predictAverageSpeed(input, ballDistance);

        List<Vector2> aimSpots = LeadTargetUtil.leadTarget(input.getMyPosition(), input.ballPosition, input.ballVelocity, predictedAverageSpeed);
        final Vector2 aimSpot;

        // TODO: invalidate aim spots if the ball is too high to be reached!

        if (aimSpots.isEmpty() || !ArenaModel.isInBoundsBall(aimSpots.get(0))) {

            BallPath ballPath = predictBallPath(input, input.time, Duration.ofSeconds(PREDICTION_SECONDS));

            Optional<SpaceTimeVelocity> stvOption = ballPath.getMotionAfterWallBounce(1);
            if (stvOption.isPresent()) {
                SpaceTimeVelocity stv = stvOption.get();
                Vector3 postBounceVelocity = stv.velocity;
                Duration timeTillBounce = Duration.between(input.time, stv.getTime());
                double timeTillBounceInSeconds = timeTillBounce.toMillis() / 1000.0;

                // Now back it up
                Vector3 backwards = (Vector3) postBounceVelocity.scaleCopy(-timeTillBounceInSeconds);
                Vector3 imaginaryStartingPoint = stv.getSpace().addCopy(backwards); // This is probably out of bounds
                aimSpots = LeadTargetUtil.leadTarget(input.getMyPosition(), imaginaryStartingPoint, postBounceVelocity, predictedAverageSpeed);
                while (!aimSpots.isEmpty() && !ArenaModel.isInBoundsBall(aimSpots.get(0))) {
                    aimSpots.remove(0);
                }
                if (aimSpots.isEmpty()) {
                    // We're probably chasing the ball. Aim toward the end of our prediction.
                    Vector3 pathEnd = ballPath.getMotionAt(input.time.plusSeconds(PREDICTION_SECONDS)).get().getSpace();
                    aimSpot = new Vector2(pathEnd.x, pathEnd.y);
                } else {
                    System.out.println("Successful wall read!");
                    aimSpot = aimSpots.get(0);
                }
            } else {
                // This might happen if the ball is going faster than our expected speed. Just chase the ball.
                aimSpot = new Vector2(input.ballPosition.x, input.ballPosition.y);
            }
        } else {
            aimSpot = aimSpots.get(0);
        }

        double etaSeconds = aimSpot.magnitude() / predictedAverageSpeed;
        long etaMillis = (long) (etaSeconds * 1000);

        return new SpaceTime(new Vector3(aimSpot.x, aimSpot.y, 0), input.time.plus(Duration.ofMillis(etaMillis)));
    }

    public static SpaceTime predictBallIntercept3d(AgentInput input, SpaceTime flatIntercept) {

        double predictedBallHeight = predictBallHeight(input, flatIntercept.time);

        return new SpaceTime(new Vector3(flatIntercept.space.x, flatIntercept.space.y, predictedBallHeight), flatIntercept.time);
    }

    private static double predictBallHeight(AgentInput input, LocalDateTime moment) {
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

        float playerDirectionRad = (float) Math.atan2(myRotation.noseVector.x, myRotation.noseVector.y);

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

        double distance = position.subCopy(input.getMyPosition()).magnitude();
        double speed = input.getMyVelocity().magnitude();
        boolean shouldSlide = difference > Math.PI / 2;
        boolean shouldBrake = distance < 25 && difference > Math.PI / 6 && speed > MAX_SPEED * .6;
        boolean isSupersonic = MAX_SPEED - speed < .01;

        boolean shouldBoost = !shouldBrake && difference < Math.PI / 6 && !isSupersonic;

        return new AgentOutput()
                .withAcceleration(shouldBrake ? 0 : 1)
                .withDeceleration(shouldBrake ? 1 : 0)
                .withSteer((float) (-Math.signum(correctionAngle) * turnSharpness))
                .withSlide(shouldSlide)
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

}
