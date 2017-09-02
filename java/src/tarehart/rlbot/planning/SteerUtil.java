package tarehart.rlbot.planning;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.math.*;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.tuning.BotLog;
import tarehart.rlbot.tuning.Telemetry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SteerUtil {

    public static final int SUPERSONIC_SPEED = 46;
    public static final int MEDIUM_SPEED = 28;
    public static final double GOOD_ENOUGH_ANGLE = Math.PI / 40;
    private static final ArenaModel arenaModel = new ArenaModel();
    private static final int BOOST_NEEDED_FOR_ZERO_TO_MAX = 60;
    private static final double DISTANCE_NEEDED_FOR_ZERO_TO_MAX_WITH_BOOST = 60;
    private static final double DISTANCE_NEEDED_FOR_ZERO_TO_MAX_WITH_FLIPS = 150;
    private static final double FRONT_FLIP_SECONDS = 1.5;

    public static Optional<SpaceTime> getCatchOpportunity(AgentInput input, BallPath ballPath) {

        LocalDateTime searchStart = input.time;

        double potentialEnergy = (input.ballPosition.z - ArenaModel.BALL_RADIUS) * ArenaModel.GRAVITY;
        double verticalKineticEnergy = 0.5 * input.ballVelocity.z * input.ballVelocity.z;
        double groundBounceEnergy = potentialEnergy + verticalKineticEnergy;

        if (groundBounceEnergy < 50) {
            return Optional.empty();
        }

        for (int i = 0; i < 3; i++) {
            Optional<SpaceTimeVelocity> landingOption = ballPath.getLanding(searchStart);

            if (landingOption.isPresent()) {
                SpaceTime landing = landingOption.get().toSpaceTime();
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
        double travelSeconds = AccelerationModel.simulateTravelTime(input, spaceTime.space, input.getMyBoost());
        double secondsAllotted = Duration.between(input.time, spaceTime.time).toMillis() / 1000.0;
        return travelSeconds < secondsAllotted;
    }

    public static List<SpaceTime> getInterceptOpportunitiesAssumingMaxAccel(AgentInput input, BallPath ballPath, double boostBudget) {

        Optional<SpaceTime> ballPark = getInterceptOpportunities(input, ballPath, Math.max(input.getMyVelocity().magnitude(), MEDIUM_SPEED)).stream().findFirst();

        Vector3 target = input.ballPosition;
        if (ballPark.isPresent()) {
            target = ballPark.get().space;
        }

        double travelSeconds = AccelerationModel.simulateTravelTime(input, target, boostBudget);
        double flatDistance = VectorUtil.flatten(input.getMyPosition()).distance(VectorUtil.flatten(target));

        double predictedAverageSpeed = flatDistance / travelSeconds;
        return getInterceptOpportunities(input, ballPath, predictedAverageSpeed);
    }

    public static List<SpaceTime> getInterceptOpportunities(AgentInput input, BallPath ballPath, double predictedAverageSpeed) {

        List<SpaceTime> interceptOpportunities = new ArrayList<>();

        Vector3 myPosition = input.getMyPosition();
        double previousSpeedDiff = 0;
        for (SpaceTimeVelocity ballMoment: ballPath.getSlices()) {
            double speedNeeded = VectorUtil.flatDistance(myPosition, ballMoment.space) / TimeUtil.secondsBetween(input.time, ballMoment.time);
            double speedDiff = predictedAverageSpeed - speedNeeded;
            if (speedDiff == 0 || speedDiff * previousSpeedDiff < 0) {
                interceptOpportunities.add(ballMoment.toSpaceTime());
            }
            previousSpeedDiff = speedDiff;
        }

        return interceptOpportunities;
    }

    private static SpaceTime get3dIntercept(SpaceTime shadowTarget, BallPath ballPath) {
        LocalDateTime interceptTime = shadowTarget.time;
        Optional<SpaceTimeVelocity> motion = ballPath.getMotionAt(interceptTime);
        if (motion.isPresent()) {
            return motion.get().toSpaceTime();
        } else {
            return new SpaceTime(new Vector3(shadowTarget.space.x, shadowTarget.space.y, ArenaModel.BALL_RADIUS), interceptTime);
        }
    }

    private static List<SpaceTime> interceptWallBounce(AgentInput input, double predictedAverageSpeed, SpaceTimeVelocity rightAfterBounce) {
        Vector3 postBounceVelocity = rightAfterBounce.velocity;
        Duration timeTillBounce = Duration.between(input.time, rightAfterBounce.getTime());
        double timeTillBounceInSeconds = timeTillBounce.toMillis() / 1000.0;

        // Now back it up
        Vector3 backwards = (Vector3) postBounceVelocity.scaleCopy(-timeTillBounceInSeconds);
        Vector3 imaginaryStartingPoint = rightAfterBounce.getSpace().addCopy(backwards); // This is probably out of bounds
        return LeadTargetUtil.leadTarget(input.getMyPosition(), imaginaryStartingPoint, postBounceVelocity, predictedAverageSpeed, rightAfterBounce.getTime());
    }

    public static BallPath predictBallPath(AgentInput input, LocalDateTime startingAt, Duration duration) {
        Telemetry telemetry = Telemetry.forTeam(input.team);

        if (telemetry.getBallPath() == null) {
            telemetry.setBallPath(arenaModel.simulateBall(new SpaceTimeVelocity(input.ballPosition, startingAt, input.ballVelocity), duration));
            return telemetry.getBallPath();
        }

        if (telemetry.getBallPath().getEndpoint().getTime().isBefore(startingAt.plus(duration))) {
            arenaModel.extendSimulation(telemetry.getBallPath(), startingAt.plus(duration));
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

        double speed = input.getMyVelocity().magnitude();
        double difference = Math.abs(correctionAngle);
        double turnSharpness = difference * 6/Math.PI + speed * .007;

        double distance = position.subCopy(input.getMyPosition()).magnitude();
        boolean shouldBrake = distance < 25 && difference > Math.PI / 6 && speed > SUPERSONIC_SPEED * .6;
        boolean shouldSlide = shouldBrake || difference > Math.PI / 2;
        boolean isSupersonic = SUPERSONIC_SPEED - speed < .01;

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
            AgentOutput steer = SteerUtil.steerTowardPosition(input, position.getLocation());

            double correction = Math.abs(SteerUtil.getCorrectionAngleRad(input, position.getLocation()));
            if (correction > Math.PI / 4 && input.getMyVelocity().magnitude() > AccelerationModel.MEDIUM_SPEED) {
                steer.withBoost(false).withAcceleration(0).withDeceleration(.2);
            }
            return steer;
        } else {
            return SteerUtil.steerTowardPosition(input, position.getNearestHandle(input.getMyPosition()));
        }
    }

    public static double getDistanceFromMe(AgentInput input, Vector3 loc) {
        return loc.distance(input.getMyPosition());
    }

    public static Optional<Plan> getSensibleFlip(AgentInput input, SpaceTime target) {

        Duration timeTillIntercept = Duration.between(input.time, target.time);
        double distanceToIntercept = VectorUtil.flatDistance(target.space, input.getMyPosition());
        if (timeTillIntercept.toMillis() > AccelerationModel.FRONT_FLIP_SECONDS * 1000 * 1.5 && distanceToIntercept > 50) {

            double correctionAngleRad = SteerUtil.getCorrectionAngleRad(input, target.space);

            if (Math.abs(correctionAngleRad) < GOOD_ENOUGH_ANGLE
                    && input.getMyVelocity().magnitude() > SteerUtil.SUPERSONIC_SPEED / 4) {

                return Optional.of(SetPieces.frontFlip());
            }
        }

        return Optional.empty();
    }

    public static AgentOutput getThereOnTime(AgentInput input, SpaceTime groundPositionAndTime) {
        double flatDistance = VectorUtil.flatDistance(input.getMyPosition(), groundPositionAndTime.space);

        double secondsTillAppointment = Duration.between(input.time, groundPositionAndTime.time).toMillis() / 1000.0;
        double speed = input.getMyVelocity().magnitude();

        double pace = speed * secondsTillAppointment / flatDistance; // Ideally this should be 1

        if (flatDistance > 40) {
            // Go fast
            return SteerUtil.steerTowardPosition(input, groundPositionAndTime.space);
        } else if (flatDistance > 10 && pace < 1) {
            // Go fast
            return SteerUtil.steerTowardPosition(input, groundPositionAndTime.space);
        } else if (pace < 1) {
            // Go moderate
            return SteerUtil.steerTowardPosition(input, groundPositionAndTime.space).withBoost(false);
        } else {
            // We're going too fast!
            AgentOutput agentOutput = SteerUtil.steerTowardPosition(input, groundPositionAndTime.space);
            agentOutput.withAcceleration(0).withBoost(false).withDeceleration(Math.max(0, pace - 1.5)); // Hit the brakes, but keep steering!
            return agentOutput;
        }
    }

}
