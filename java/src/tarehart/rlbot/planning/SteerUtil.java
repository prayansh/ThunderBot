package tarehart.rlbot.planning;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.math.*;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.tuning.BotLog;
import tarehart.rlbot.tuning.Telemetry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
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

    public static Optional<SpaceTime> getCatchOpportunity(AgentInput input, BallPath ballPath, double boostBudget) {

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

        Optional<SpaceTimeVelocity> landingOption = ballPath.getPlaneBreak(searchStart, new Vector3(0, 0, height), new Vector3(0, 0, 1));

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

        Vector3 myPosition = input.getMyPosition();

        for (SpaceTimeVelocity ballMoment: ballPath.getSlices()) {
            Optional<DistanceTimeSpeed> motionAt = acceleration.getMotionAt(ballMoment.getTime());
            if (motionAt.isPresent()) {
                DistanceTimeSpeed dts = motionAt.get();
                if (dts.distance > VectorUtil.flatDistance(myPosition, ballMoment.space)) {
                    return Optional.of(ballMoment.toSpaceTime());
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

    public static double getCorrectionAngleRad(AgentInput input, Vector3 position) {
        return getCorrectionAngleRad(VectorUtil.flatten(input.getMyRotation().noseVector), VectorUtil.flatten((Vector3) position.subCopy(input.getMyPosition())));
    }

    public static double getCorrectionAngleRad(Vector2 current, Vector2 ideal) {

        float currentRad = (float) Math.atan2(current.x, current.y);
        float idealRad = (float) Math.atan2(ideal.x, ideal.y);

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

    public static AgentOutput steerTowardPosition(AgentInput input, Vector3 position) {

        double correctionAngle = getCorrectionAngleRad(input, position);

        double speed = input.getMyVelocity().magnitude();
        double difference = Math.abs(correctionAngle);
        double turnSharpness = difference * 6/Math.PI + difference * speed * .1;

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

    public static Optional<Plan> getSensibleFlip(AgentInput input, Vector3 target) {

        double distanceToIntercept = VectorUtil.flatDistance(target, input.getMyPosition());
        double speed = input.getMyVelocity().magnitude();

        double seconds = distanceToIntercept / speed; // This will underestimate; that's alright because it will cause us to flip less when we're oriented wrong.
        return getSensibleFlip(input, new SpaceTime(target, input.time.plus(TimeUtil.toDuration(seconds))));
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
