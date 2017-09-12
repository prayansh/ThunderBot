package tarehart.rlbot.planning;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.*;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.BallPhysics;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.tuning.Telemetry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.BiPredicate;

public class SteerUtil {

    public static final int SUPERSONIC_SPEED = 46;
    public static final int MEDIUM_SPEED = 28;
    public static final double GOOD_ENOUGH_ANGLE = Math.PI / 40;
    private static final ArenaModel arenaModel = new ArenaModel();
    private static final int BOOST_NEEDED_FOR_ZERO_TO_MAX = 60;
    private static final double DISTANCE_NEEDED_FOR_ZERO_TO_MAX_WITH_BOOST = 60;
    private static final double DISTANCE_NEEDED_FOR_ZERO_TO_MAX_WITH_FLIPS = 150;
    private static final double FRONT_FLIP_SECONDS = 1.5;

    public static Optional<SpaceTime> getCatchOpportunity(AgentInput input, BallPath ballPath, double boostBudget) {

        LocalDateTime searchStart = input.time;

        double groundBounceEnergy = BallPhysics.getGroundBounceEnergy(input);

        if (groundBounceEnergy < 50) {
            return Optional.empty();
        }

        for (int i = 0; i < 3; i++) {
            Optional<SpaceTimeVelocity> landingOption = ballPath.getLanding(searchStart);

            if (landingOption.isPresent()) {
                SpaceTime landing = landingOption.get().toSpaceTime();
                if (canGetUnder(input, landing, boostBudget)) {
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

    public static Optional<SpaceTime> getVolleyOpportunity(AgentInput input, BallPath ballPath, double boostBudget, double height) {

        LocalDateTime searchStart = input.time;

        Optional<SpaceTimeVelocity> landingOption = ballPath.getPlaneBreak(searchStart, new Plane(new Vector3(0, 0, height), new Vector3(0, 0, 1)), true);

        if (landingOption.isPresent()) {
            SpaceTime landing = landingOption.get().toSpaceTime();
            if (canGetUnder(input, landing, boostBudget)) {
                return Optional.of(landing);
            }
        }

        return Optional.empty();
    }

    private static boolean canGetUnder(AgentInput input, SpaceTime spaceTime, double boostBudget) {
        DistancePlot plot = AccelerationModel.simulateAcceleration(input, Duration.ofSeconds(4), boostBudget, input.getMyPosition().distance(spaceTime.space));
        Optional<Double> travelSeconds = AccelerationModel.getTravelSeconds(input, plot, spaceTime.space);
        double secondsAllotted = Duration.between(input.time, spaceTime.time).toMillis() / 1000.0;
        return travelSeconds.filter(travel -> travel < secondsAllotted).isPresent();
    }

    public static Optional<SpaceTime> getInterceptOpportunityAssumingMaxAccel(AgentInput input, BallPath ballPath, double boostBudget) {
        DistancePlot plot = AccelerationModel.simulateAcceleration(input, Duration.ofSeconds(4), boostBudget);

        return getInterceptOpportunity(input, ballPath, plot);
    }

    public static Optional<SpaceTime> getInterceptOpportunity(AgentInput input, BallPath ballPath, DistancePlot acceleration) {
        return getFilteredInterceptOpportunity(input, ballPath, acceleration, new Vector3(), (a, b) -> true);
    }

    public static Optional<SpaceTime> getFilteredInterceptOpportunity(
            AgentInput input, BallPath ballPath, DistancePlot acceleration, Vector3 interceptModifier, BiPredicate<AgentInput, SpaceTime> predicate) {

        Vector3 myPosition = input.getMyPosition();

        for (SpaceTimeVelocity ballMoment: ballPath.getSlices()) {
            Optional<DistanceTimeSpeed> motionAt = acceleration.getMotionAt(ballMoment.getTime());
            if (motionAt.isPresent()) {
                DistanceTimeSpeed dts = motionAt.get();
                Vector3 intercept = ballMoment.space.addCopy(interceptModifier);
                SpaceTime interceptSpaceTime = new SpaceTime(intercept, ballMoment.getTime());
                double ballDistance = VectorUtil.flatDistance(myPosition, intercept);
                if (dts.distance > ballDistance) {
                    Optional<Double> travelSeconds = AccelerationModel.getTravelSeconds(input, acceleration, intercept);
                    if (travelSeconds.isPresent() && travelSeconds.get() <= TimeUtil.secondsBetween(input.time, interceptSpaceTime.time)) {
                        if (predicate.test(input, interceptSpaceTime)) {
                            return Optional.of(interceptSpaceTime);
                        }
                    }
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<SpaceTime> getInterceptOpportunity(AgentInput input, BallPath ballPath, double speed) {

        Vector3 myPosition = input.getMyPosition();

        for (SpaceTimeVelocity ballMoment: ballPath.getSlices()) {
            double distanceSoFar = TimeUtil.secondsBetween(input.time, ballMoment.getTime()) * speed;
            if (distanceSoFar > VectorUtil.flatDistance(myPosition, ballMoment.space)) {
                return Optional.of(ballMoment.toSpaceTime());
            }
        }

        return Optional.empty();
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

    public static double getCorrectionAngleRad(AgentInput input, Vector3 target) {
        return getCorrectionAngleRad(input, VectorUtil.flatten(target));
    }

    public static double getCorrectionAngleRad(AgentInput input, Vector2 target) {
        return getCorrectionAngleRad(VectorUtil.flatten(input.getMyRotation().noseVector), (Vector2) target.subCopy(VectorUtil.flatten(input.getMyPosition())));
    }

    public static double getCorrectionAngleRad(Vector2 current, Vector2 ideal) {

        float currentRad = (float) Math.atan2(current.y, current.x);
        float idealRad = (float) Math.atan2(ideal.y, ideal.x);

        if (Math.abs(currentRad - idealRad) > Math.PI) {
            if (currentRad < 0) {
                currentRad += Math.PI * 2;
            }
            if (idealRad < 0) {
                idealRad += Math.PI * 2;
            }
        }

        return idealRad - currentRad;
    }

    public static AgentOutput steerTowardGroundPosition(AgentInput input, Vector2 position) {
        double correctionAngle = getCorrectionAngleRad(input, position);
        Vector2 myPositionFlat = VectorUtil.flatten(input.getMyPosition());
        double distance = position.distance(myPositionFlat);
        double speed = input.getMyVelocity().magnitude();
        return getSteeringOutput(correctionAngle, distance, speed);
    }

    public static AgentOutput steerTowardWallPosition(AgentInput input, Vector3 position) {
        Vector3 toPosition = (Vector3) position.subCopy(input.getMyPosition());
        double correctionAngle = VectorUtil.getCorrectionAngle(input.getMyRotation().noseVector, toPosition, input.getMyRotation().roofVector);
        double speed = input.getMyVelocity().magnitude();
        double distance = position.distance(input.getMyPosition());
        return getSteeringOutput(correctionAngle, distance, speed);
    }

    private static AgentOutput getSteeringOutput(double correctionAngle, double distance, double speed) {
        double difference = Math.abs(correctionAngle);
        double turnSharpness = difference * 6/Math.PI + difference * speed * .1;

        boolean shouldBrake = distance < 25 && difference > Math.PI / 6 && speed > SUPERSONIC_SPEED * .6;
        boolean shouldSlide = shouldBrake || difference > Math.PI / 2;
        boolean isSupersonic = SUPERSONIC_SPEED - speed < .01;

        boolean shouldBoost = !shouldBrake && turnSharpness < .5 && !isSupersonic;

        return new AgentOutput()
                .withAcceleration(shouldBrake ? 0 : 1)
                .withDeceleration(shouldBrake ? 1 : 0)
                .withSteer((float) (-Math.signum(correctionAngle) * turnSharpness))
                .withSlide(shouldSlide)
                .withBoost(shouldBoost);
    }

    public static AgentOutput steerTowardGroundPosition(AgentInput input, Vector3 position) {
        return steerTowardGroundPosition(input, VectorUtil.flatten(position));
    }

    public static AgentOutput arcTowardPosition(AgentInput input, SplineHandle position) {
        if (position.isWithinHandleRange(input.getMyPosition())) {
            AgentOutput steer = SteerUtil.steerTowardGroundPosition(input, position.getLocation());

            double correction = Math.abs(SteerUtil.getCorrectionAngleRad(input, position.getLocation()));
            if (correction > Math.PI / 4 && input.getMyVelocity().magnitude() > AccelerationModel.MEDIUM_SPEED) {
                steer.withBoost(false).withAcceleration(0).withDeceleration(.2);
            }
            return steer;
        } else {
            return SteerUtil.steerTowardGroundPosition(input, position.getNearestHandle(input.getMyPosition()));
        }
    }

    public static double getDistanceFromMe(AgentInput input, Vector3 loc) {
        return loc.distance(input.getMyPosition());
    }

    public static Optional<Plan> getSensibleFlip(AgentInput input, Vector3 target) {
        return getSensibleFlip(input, VectorUtil.flatten(target));
    }

    public static Optional<Plan> getSensibleFlip(AgentInput input, Vector2 target) {

        double distanceCovered = AccelerationModel.getFrontFlipDistance(input.getMyVelocity().magnitude());

        double distanceToIntercept = target.distance(VectorUtil.flatten(input.getMyPosition()));
        if (distanceToIntercept > distanceCovered + 15) {

            Vector2 facing = VectorUtil.flatten(input.getMyRotation().noseVector);
            double facingCorrection = SteerUtil.getCorrectionAngleRad(facing, target);
            double slideAngle = SteerUtil.getCorrectionAngleRad(facing, VectorUtil.flatten(input.getMyVelocity()));

            if (Math.abs(facingCorrection) < GOOD_ENOUGH_ANGLE && Math.abs(slideAngle) < GOOD_ENOUGH_ANGLE
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
            return SteerUtil.steerTowardGroundPosition(input, groundPositionAndTime.space);
        } else if (flatDistance > 10 && pace < 1) {
            // Go fast
            return SteerUtil.steerTowardGroundPosition(input, groundPositionAndTime.space);
        } else if (pace < 1) {
            // Go moderate
            return SteerUtil.steerTowardGroundPosition(input, groundPositionAndTime.space).withBoost(false);
        } else {
            // We're going too fast!
            AgentOutput agentOutput = SteerUtil.steerTowardGroundPosition(input, groundPositionAndTime.space);
            agentOutput.withAcceleration(0).withBoost(false).withDeceleration(Math.max(0, pace - 1.5)); // Hit the brakes, but keep steering!
            return agentOutput;
        }
    }

    private static double getTurnRadius(double speed) {
        return Math.abs(speed) * .8;
    }

    public static Optional<Vector2> getWaypointForCircleTurn(AgentInput input, DistancePlot distancePlot, Vector2 targetPosition, Vector2 targetFacing) {
        Vector2 flatPosition = VectorUtil.flatten(input.getMyPosition());
        double distance = flatPosition.distance(targetPosition);
        double currentSpeed = input.getMyVelocity().magnitude();
        double expectedSpeed = AccelerationModel.SUPERSONIC_SPEED;

        Optional<DistanceTimeSpeed> motion = distancePlot.getMotionAt(distance);
        if (motion.isPresent()) {
            expectedSpeed = motion.get().speed;
        }
        return circleWaypoint(input, targetPosition, targetFacing, currentSpeed, expectedSpeed);
    }

    private static Optional<Vector2> circleWaypoint(AgentInput input, Vector2 targetPosition, Vector2 targetFacing, double currentSpeed, double expectedSpeed) {

        Vector2 flatPosition = VectorUtil.flatten(input.getMyPosition());
        Vector2 toTarget = (Vector2) targetPosition.subCopy(flatPosition);
        Vector2 currentFacing = VectorUtil.flatten(input.getMyRotation().noseVector);
        double approachCorrection = getCorrectionAngleRad(currentFacing, toTarget);
        if (Math.abs(approachCorrection) > Math.PI / 2) {
            return Optional.empty();
        }

        double turnRadius = getTurnRadius(expectedSpeed);
        Vector2 radiusVector = (Vector2) VectorUtil.orthogonal(targetFacing).scaleCopy(turnRadius);
        if (radiusVector.dotProduct(toTarget) > 0) {
            radiusVector.scale(-1); // Make sure the radius vector points from the target position to the center of the turn circle.
        }

        Vector2 center = (Vector2) targetPosition.addCopy(radiusVector);
        double distanceFromCenter = flatPosition.distance(center);

        if (distanceFromCenter < turnRadius * 1.1) {

            if (distanceFromCenter < turnRadius * .8) {
                if (currentSpeed < expectedSpeed) {
                    return circleWaypoint(input, targetPosition, targetFacing, currentSpeed, currentSpeed);
                }
                return Optional.empty();
            }

            return Optional.of(targetPosition);
        }

        Vector2 centerToTangent = (Vector2) VectorUtil.orthogonal(toTarget).normaliseCopy().scaleCopy(turnRadius);
        if (centerToTangent.dotProduct(targetFacing) > 0) {
            centerToTangent.scale(-1); // Make sure we choose the tangent point behind the target car.
        }
        Vector2 tangentPoint = (Vector2) center.addCopy(centerToTangent);
        return Optional.of(tangentPoint);
    }
}
