package tarehart.rlbot.planning;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarData;
import tarehart.rlbot.math.*;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.BallPhysics;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.tuning.BallTelemetry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.BiPredicate;

public class SteerUtil {

    public static final int SUPERSONIC_SPEED = 46;
    public static final double GOOD_ENOUGH_ANGLE = Math.PI / 40;
    private static final ArenaModel arenaModel = new ArenaModel();

    public static Optional<SpaceTime> getCatchOpportunity(CarData carData, BallPath ballPath, double boostBudget) {

        LocalDateTime searchStart = carData.time;

        double groundBounceEnergy = BallPhysics.getGroundBounceEnergy(ballPath.getStartPoint());

        if (groundBounceEnergy < 50) {
            return Optional.empty();
        }

        for (int i = 0; i < 3; i++) {
            Optional<SpaceTimeVelocity> landingOption = ballPath.getLanding(searchStart);

            if (landingOption.isPresent()) {
                SpaceTime landing = landingOption.get().toSpaceTime();
                if (canGetUnder(carData, landing, boostBudget)) {
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

    public static Optional<SpaceTime> getVolleyOpportunity(CarData carData, BallPath ballPath, double boostBudget, double height) {

        LocalDateTime searchStart = carData.time;

        Optional<SpaceTimeVelocity> landingOption = ballPath.getPlaneBreak(searchStart, new Plane(new Vector3(0, 0, height), new Vector3(0, 0, 1)), true);

        if (landingOption.isPresent()) {
            SpaceTime landing = landingOption.get().toSpaceTime();
            if (canGetUnder(carData, landing, boostBudget)) {
                return Optional.of(landing);
            }
        }

        return Optional.empty();
    }

    private static boolean canGetUnder(CarData carData, SpaceTime spaceTime, double boostBudget) {
        DistancePlot plot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4), boostBudget, carData.position.distance(spaceTime.space));
        Optional<Double> travelSeconds = AccelerationModel.getTravelSeconds(carData, plot, spaceTime.space);
        double secondsAllotted = Duration.between(carData.time, spaceTime.time).toMillis() / 1000.0;
        return travelSeconds.filter(travel -> travel < secondsAllotted).isPresent();
    }

    public static Optional<SpaceTime> getInterceptOpportunityAssumingMaxAccel(CarData carData, BallPath ballPath, double boostBudget) {
        DistancePlot plot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4), boostBudget);

        return getInterceptOpportunity(carData, ballPath, plot);
    }

    public static Optional<SpaceTime> getInterceptOpportunity(CarData carData, BallPath ballPath, DistancePlot acceleration) {
        return getFilteredInterceptOpportunity(carData, ballPath, acceleration, new Vector3(), (a, b) -> true);
    }

    public static Optional<SpaceTime> getFilteredInterceptOpportunity(
            CarData carData, BallPath ballPath, DistancePlot acceleration, Vector3 interceptModifier, BiPredicate<CarData, SpaceTime> predicate) {

        Vector3 myPosition = carData.position;

        for (SpaceTimeVelocity ballMoment: ballPath.getSlices()) {
            Optional<DistanceTimeSpeed> motionAt = acceleration.getMotionAt(ballMoment.getTime());
            if (motionAt.isPresent()) {
                DistanceTimeSpeed dts = motionAt.get();
                Vector3 intercept = ballMoment.space.addCopy(interceptModifier);
                SpaceTime interceptSpaceTime = new SpaceTime(intercept, ballMoment.getTime());
                double ballDistance = VectorUtil.flatDistance(myPosition, intercept);
                if (dts.distance > ballDistance) {
                    Optional<Double> travelSeconds = AccelerationModel.getTravelSeconds(carData, acceleration, intercept);
                    if (travelSeconds.isPresent() && travelSeconds.get() <= TimeUtil.secondsBetween(carData.time, interceptSpaceTime.time)) {
                        if (predicate.test(carData, interceptSpaceTime)) {
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

    public static Optional<SpaceTime> getInterceptOpportunity(CarData car, BallPath ballPath, double speed) {

        for (SpaceTimeVelocity ballMoment: ballPath.getSlices()) {
            double distanceSoFar = TimeUtil.secondsBetween(car.time, ballMoment.getTime()) * speed;
            if (distanceSoFar > VectorUtil.flatDistance(car.position, ballMoment.space)) {
                return Optional.of(ballMoment.toSpaceTime());
            }
        }

        return Optional.empty();
    }

    public static BallPath predictBallPath(AgentInput input, LocalDateTime startingAt, Duration duration) {

        Optional<BallPath> pathOption = BallTelemetry.getPath();

        if (pathOption.isPresent()) {
            BallPath ballPath = pathOption.get();
            if (ballPath.getEndpoint().getTime().isBefore(startingAt.plus(duration))) {
                arenaModel.extendSimulation(ballPath, startingAt.plus(duration));
            }
            return ballPath;
        } else {
            BallPath ballPath = arenaModel.simulateBall(new SpaceTimeVelocity(input.ballPosition, startingAt, input.ballVelocity), duration);
            BallTelemetry.setPath(ballPath);
            return ballPath;
        }
    }

    public static double getCorrectionAngleRad(CarData carData, Vector3 target) {
        return getCorrectionAngleRad(carData, VectorUtil.flatten(target));
    }

    public static double getCorrectionAngleRad(CarData carData, Vector2 target) {
        return getCorrectionAngleRad(VectorUtil.flatten(carData.rotation.noseVector), (Vector2) target.subCopy(VectorUtil.flatten(carData.position)));
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

    public static AgentOutput steerTowardGroundPosition(CarData carData, Vector2 position) {
        double correctionAngle = getCorrectionAngleRad(carData, position);
        Vector2 myPositionFlat = VectorUtil.flatten(carData.position);
        double distance = position.distance(myPositionFlat);
        double speed = carData.velocity.magnitude();
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic);
    }

    public static AgentOutput steerTowardWallPosition(CarData carData, Vector3 position) {
        Vector3 toPosition = (Vector3) position.subCopy(carData.position);
        double correctionAngle = VectorUtil.getCorrectionAngle(carData.rotation.noseVector, toPosition, carData.rotation.roofVector);
        double speed = carData.velocity.magnitude();
        double distance = position.distance(carData.position);
        return getSteeringOutput(correctionAngle, distance, speed, carData.isSupersonic);
    }

    private static AgentOutput getSteeringOutput(double correctionAngle, double distance, double speed, boolean isSupersonic) {
        double difference = Math.abs(correctionAngle);
        double turnSharpness = difference * 6/Math.PI + difference * speed * .1;

        boolean shouldBrake = distance < 25 && difference > Math.PI / 6 && speed > 25;
        boolean shouldSlide = shouldBrake || difference > Math.PI / 2;
        boolean shouldBoost = !shouldBrake && turnSharpness < .5 && !isSupersonic;

        return new AgentOutput()
                .withAcceleration(shouldBrake ? 0 : 1)
                .withDeceleration(shouldBrake ? 1 : 0)
                .withSteer((float) (-Math.signum(correctionAngle) * turnSharpness))
                .withSlide(shouldSlide)
                .withBoost(shouldBoost);
    }

    public static AgentOutput steerTowardGroundPosition(CarData carData, Vector3 position) {
        return steerTowardGroundPosition(carData, VectorUtil.flatten(position));
    }

    public static AgentOutput arcTowardPosition(CarData car, SplineHandle position) {
        if (position.isWithinHandleRange(car.position)) {
            AgentOutput steer = SteerUtil.steerTowardGroundPosition(car, position.getLocation());

            double correction = Math.abs(SteerUtil.getCorrectionAngleRad(car, position.getLocation()));
            if (correction > Math.PI / 4 && car.velocity.magnitude() > AccelerationModel.MEDIUM_SPEED) {
                steer.withBoost(false).withAcceleration(0).withDeceleration(.2);
            }
            return steer;
        } else {
            return SteerUtil.steerTowardGroundPosition(car, position.getNearestHandle(car.position));
        }
    }

    public static double getDistanceFromCar(CarData car, Vector3 loc) {
        return loc.distance(car.position);
    }

    public static Optional<Plan> getSensibleFlip(CarData car, Vector3 target) {
        return getSensibleFlip(car, VectorUtil.flatten(target));
    }

    public static Optional<Plan> getSensibleFlip(CarData car, Vector2 target) {

        double distanceCovered = AccelerationModel.getFrontFlipDistance(car.velocity.magnitude());

        double distanceToIntercept = target.distance(VectorUtil.flatten(car.position));
        if (distanceToIntercept > distanceCovered + 15) {

            Vector2 facing = VectorUtil.flatten(car.rotation.noseVector);
            double facingCorrection = SteerUtil.getCorrectionAngleRad(facing, target);
            double slideAngle = SteerUtil.getCorrectionAngleRad(facing, VectorUtil.flatten(car.velocity));

            if (Math.abs(facingCorrection) < GOOD_ENOUGH_ANGLE && Math.abs(slideAngle) < GOOD_ENOUGH_ANGLE
                    && car.velocity.magnitude() > SteerUtil.SUPERSONIC_SPEED / 4) {

                return Optional.of(SetPieces.frontFlip());
            }
        }

        return Optional.empty();
    }

    public static AgentOutput getThereOnTime(CarData input, SpaceTime groundPositionAndTime) {
        double flatDistance = VectorUtil.flatDistance(input.position, groundPositionAndTime.space);

        double secondsTillAppointment = Duration.between(input.time, groundPositionAndTime.time).toMillis() / 1000.0;
        double speed = input.velocity.magnitude();

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

    public static double getFacingCorrectionSeconds(Vector2 approach, Vector2 targetFacing, double expectedSpeed) {

        double correction = getCorrectionAngleRad(approach, targetFacing);
        return getTurnRadius(expectedSpeed) * Math.abs(correction) / expectedSpeed;
    }

    public static Optional<Vector2> getWaypointForCircleTurn(
            CarData car, DistancePlot distancePlot, Vector2 targetPosition, Vector2 targetFacing) {

        Vector2 flatPosition = VectorUtil.flatten(car.position);
        double distance = flatPosition.distance(targetPosition);
        double currentSpeed = car.velocity.magnitude();
        double expectedSpeed = AccelerationModel.SUPERSONIC_SPEED;

        Optional<DistanceTimeSpeed> motion = distancePlot.getMotionAt(distance);
        if (motion.isPresent()) {
            expectedSpeed = motion.get().speed;
        }
        return circleWaypoint(car, targetPosition, targetFacing, currentSpeed, expectedSpeed);
    }

    private static Optional<Vector2> circleWaypoint(CarData car, Vector2 targetPosition, Vector2 targetFacing, double currentSpeed, double expectedSpeed) {

        Vector2 flatPosition = VectorUtil.flatten(car.position);
        Vector2 toTarget = (Vector2) targetPosition.subCopy(flatPosition);
        Vector2 currentFacing = VectorUtil.flatten(car.rotation.noseVector);
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
                    return circleWaypoint(car, targetPosition, targetFacing, currentSpeed, currentSpeed);
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
