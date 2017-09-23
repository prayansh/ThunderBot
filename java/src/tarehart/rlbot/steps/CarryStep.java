package tarehart.rlbot.steps;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class CarryStep implements Step {

    private static final double MAX_X_DIFF = 1.3;
    private static final double MAX_Y = 1.5;
    private static final double MIN_Y = -0.9;


    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (!canCarry(input, true)) {
            return Optional.empty();
        }

        Vector2 ballVelocityFlat = VectorUtil.flatten(input.ballVelocity);
        double leadSeconds = .2;

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(2));

        Optional<SpaceTimeVelocity> motionAfterWallBounce = ballPath.getMotionAfterWallBounce(1);
        if (motionAfterWallBounce.isPresent() && Duration.between(input.time, motionAfterWallBounce.get().getTime()).toMillis() < 1000) {
            return Optional.empty(); // The dribble step is not in the business of wall reads.
        }

        Vector2 futureBallPosition;
        SpaceTimeVelocity ballFuture = ballPath.getMotionAt(input.time.plus(TimeUtil.toDuration(leadSeconds))).get();
        futureBallPosition = VectorUtil.flatten(ballFuture.getSpace());



        Vector2 scoreLocation = VectorUtil.flatten(GoalUtil.getEnemyGoal(input.team).getNearestEntrance(input.ballPosition, 3));

        Vector2 ballToGoal = (Vector2) scoreLocation.subCopy(futureBallPosition);
        Vector2 pushDirection;
        Vector2 pressurePoint;
        double approachDistance = 1;
        // TODO: vary the approachDistance based on whether the ball is forward / off to the side.

        double velocityCorrectionAngle = SteerUtil.getCorrectionAngleRad(ballVelocityFlat, ballToGoal);
        double angleTweak = Math.min(Math.PI / 6, Math.max(-Math.PI / 6, velocityCorrectionAngle * 2));
        pushDirection = (Vector2) VectorUtil.rotateVector(ballToGoal, angleTweak).normaliseCopy();
        pressurePoint = (Vector2) futureBallPosition.subCopy(pushDirection.scaleCopy(approachDistance));


        LocalDateTime hurryUp = input.time.plus(TimeUtil.toDuration(leadSeconds));

        AgentOutput dribble = SteerUtil.getThereOnTime(input.getMyCarData(), new SpaceTime(new Vector3(pressurePoint.x, pressurePoint.y, 0), hurryUp));
        return Optional.of(dribble);
    }

    private static Vector3 positionInCarCoordinates(CarData car, Vector3 worldPosition) {
        // We will assume that the car is flat on the ground.

        // We will treat (0, 1) as the car's natural orientation.
        double carYaw = SteerUtil.getCorrectionAngleRad(new Vector2(0, 1), VectorUtil.flatten(car.rotation.noseVector));

        Vector2 carToPosition = VectorUtil.flatten((Vector3) worldPosition.subCopy(car.position));

        Vector2 carToPositionRotated = VectorUtil.rotateVector(carToPosition, -carYaw);

        double zDiff = worldPosition.z - car.position.z;
        return new Vector3(carToPositionRotated.x, carToPositionRotated.y, zDiff);
    }

    public static boolean canCarry(AgentInput input, boolean log) {

        CarData car = input.getMyCarData();
        Vector3 ballInCarCoordinates = positionInCarCoordinates(car, input.ballPosition);

        double xMag = Math.abs(ballInCarCoordinates.x);
        if (xMag > MAX_X_DIFF) {
            if (log) {
                BotLog.println("Fell off the side", input.team);
            }
            return false;
        }

        if (ballInCarCoordinates.y > MAX_Y) {
            if (log) {
                BotLog.println("Fell off the front", input.team);
            }
            return false;
        }

        if (ballInCarCoordinates.y < MIN_Y) {
            if (log) {
                BotLog.println("Fell off the back", input.team);
            }
            return false;
        }

        if (ballInCarCoordinates.z > 3) {
            if (log) {
                BotLog.println("Ball too high to carry", input.team);
            }
            return false;
        }

        if (ballInCarCoordinates.z < 1) {
            if (log) {
                BotLog.println("Ball too low to carry", input.team);
            }
            return false;
        }

        if (VectorUtil.flatDistance(car.velocity, input.ballVelocity) > 10) {
            if (log) {
                BotLog.println("Velocity too different to carry.", input.team);
            }
            return false;
        }


        return true;
    }

    @Override
    public boolean isBlindlyComplete() {
        return false;
    }

    @Override
    public void begin() {
    }

    @Override
    public String getSituation() {
        return "Carrying";
    }
}
