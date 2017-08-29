package tarehart.rlbot.planning;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.math.*;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.tuning.Telemetry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private static final double AERIAL_RISE_RATE = 9;
    public static final double NEEDS_AERIAL_THRESHOLD = 4;



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

    public static Optional<SpaceTime> getCatchOpportunity(AgentInput input, BallPath ballPath) {

        LocalDateTime searchStart = input.time;

        double potentialEnergy = (input.ballPosition.z - ArenaModel.BALL_RADIUS) * ArenaModel.GRAVITY;
        double verticalKineticEnergy = 0.5 * input.ballVelocity.z * input.ballVelocity.z;
        double groundBounceEnergy = potentialEnergy + verticalKineticEnergy;

        if (groundBounceEnergy < 30) {
            return Optional.empty();
        }

        for (int i = 0; i < 3; i++) {
            Optional<SpaceTimeVelocity> landingOption = ballPath.getLanding(searchStart);

            if (landingOption.isPresent()) {
                SpaceTime landing = landingOption.get().spaceTime;
                if (canGetUnder(input, landing)) {
                    return Optional.of(landing);
                } else {
                    searchStart = landing.time.plusSeconds(1);
                }
            } else {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private static boolean canGetUnder(AgentInput input, SpaceTime spaceTime) {
        double distance = spaceTime.space.subCopy(input.getMyPosition()).magnitude();
        double predictedAverageSpeed = predictAverageSpeed(input, distance);
        double secondsAllotted = Duration.between(input.time, spaceTime.time).toMillis() / 1000.0;
        return predictedAverageSpeed * secondsAllotted > distance;
    }

    public static List<SpaceTime> getInterceptOpportunities(AgentInput input, BallPath ballPath) {

        List<SpaceTime> interceptOpportunities = new ArrayList<>();

        double ballDistance = input.ballPosition.distance(input.getMyPosition());
        double predictedAverageSpeed = predictAverageSpeed(input, ballDistance); // Using ballDistance here is an extremely rough estimate

        List<Vector2> aimSpots = LeadTargetUtil.leadTarget(input.getMyPosition(), input.ballPosition, input.ballVelocity, predictedAverageSpeed);

        if (aimSpots.isEmpty() || !ArenaModel.isInBoundsBall(aimSpots.get(0))) {

            Optional<SpaceTimeVelocity> stvOption = ballPath.getMotionAfterWallBounce(1);
            if (stvOption.isPresent()) {
                SpaceTimeVelocity stv = stvOption.get();
                aimSpots = interceptWallBounce(input, predictedAverageSpeed, stv);
            }
        }

        for (Vector2 spot: aimSpots) {
            if (ArenaModel.isInBoundsBall(spot)) {
                SpaceTime arrival = getArrival(input, spot, predictedAverageSpeed, ballPath);
                interceptOpportunities.add(arrival);
            }
        }

        return interceptOpportunities;
    }

    private static SpaceTime getArrival(AgentInput input, Vector2 target, double predictedAverageSpeed, BallPath ballPath) {
        Vector3 position = input.getMyPosition();
        double distance = target.subCopy(new Vector2(position.x, position.y)).magnitude();
        Duration travelTime = Duration.ofMillis((long) (1000 * distance / predictedAverageSpeed));
        LocalDateTime interceptTime = input.time.plus(travelTime);
        Optional<SpaceTimeVelocity> motion = ballPath.getMotionAt(interceptTime);
        if (motion.isPresent()) {
            return motion.get().spaceTime;
        } else {
            return new SpaceTime(new Vector3(target.x, target.y, ArenaModel.BALL_RADIUS), interceptTime);
        }
    }

    private static List<Vector2> interceptWallBounce(AgentInput input, double predictedAverageSpeed, SpaceTimeVelocity rightAfterBounce) {
        Vector3 postBounceVelocity = rightAfterBounce.velocity;
        Duration timeTillBounce = Duration.between(input.time, rightAfterBounce.getTime());
        double timeTillBounceInSeconds = timeTillBounce.toMillis() / 1000.0;

        // Now back it up
        Vector3 backwards = (Vector3) postBounceVelocity.scaleCopy(-timeTillBounceInSeconds);
        Vector3 imaginaryStartingPoint = rightAfterBounce.getSpace().addCopy(backwards); // This is probably out of bounds
        return LeadTargetUtil.leadTarget(input.getMyPosition(), imaginaryStartingPoint, postBounceVelocity, predictedAverageSpeed);
    }

    public static BallPath predictBallPath(AgentInput input, LocalDateTime startingAt, Duration duration) {
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
        boolean shouldBrake = distance < 25 && difference > Math.PI / 6 && speed > MAX_SPEED * .6;
        boolean shouldSlide = shouldBrake || difference > Math.PI / 2;
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

    private static boolean isVerticallyAccessible(AgentInput input, SpaceTime intercept) {
        Duration timeTillIntercept = Duration.between(input.time, intercept.time);

        if (intercept.space.z < NEEDS_AERIAL_THRESHOLD) {
            // We can probably just get it by jumping
            return true;
        }

        if (input.getMyBoost() > 30) {
            Duration tMinus = getAerialLaunchCountdown(intercept, timeTillIntercept);
            return tMinus.toMillis() >= -100;
        }
        return false;
    }

    public static Duration getAerialLaunchCountdown(SpaceTime intercept, Duration timeTillIntercept) {
        Duration expectedAerialTime = Duration.ofMillis((long) (1000 * intercept.space.z / AERIAL_RISE_RATE));
        return timeTillIntercept.minus(expectedAerialTime);
    }

    public static Optional<SpaceTime> getPreferredIntercept(AgentInput input, List<SpaceTime> interceptOpportunities) {
        return interceptOpportunities.stream().filter(intercept -> isVerticallyAccessible(input, intercept)).findFirst();
    }

}
