package tarehart.rlbot.steps;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
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

public class DribbleStep implements Step {

    public static final double DRIBBLE_DISTANCE = 20;

    public Optional<AgentOutput> getOutput(AgentInput input) {
        if (!canDribble(input, true)) {
            return Optional.empty();
        }


        Vector2 myPositonFlat = VectorUtil.flatten(input.getMyPosition());
        Vector2 ballPositionFlat = VectorUtil.flatten(input.ballPosition);
        Vector2 ballVelocityFlat = VectorUtil.flatten(input.ballVelocity);
        Vector2 toBallFlat = (Vector2) ballPositionFlat.subCopy(myPositonFlat);
        double flatDistance = toBallFlat.magnitude();


        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(1));
        SpaceTimeVelocity ballFuture = ballPath.getMotionAt(input.time.plus(TimeUtil.toDuration(.2))).get();
        Vector2 futureBallPosition = VectorUtil.flatten(ballFuture.getSpace());

        Vector2 scoreLocation = VectorUtil.flatten(GoalUtil.getEnemyGoal(input.team).getNearestEntrance(input.ballPosition, 3));

        Vector2 ballToGoal = (Vector2) scoreLocation.subCopy(futureBallPosition);

        double velocityCorrectionAngle = SteerUtil.getCorrectionAngleRad(ballVelocityFlat, ballToGoal);
        double angleTweak = Math.min(Math.PI / 6, Math.max(-Math.PI / 6, velocityCorrectionAngle * 2));

        Vector2 pushDirection = (Vector2) VectorUtil.rotateVector(ballToGoal, angleTweak).normaliseCopy();

        double approachDistance = VectorUtil.project(toBallFlat, new Vector2(pushDirection.y, -pushDirection.x)).magnitude() * 1.6 + .8;
        approachDistance = Math.min(approachDistance, 4);
        Vector2 pressurePoint = (Vector2) futureBallPosition.subCopy(pushDirection.normaliseCopy().scaleCopy(approachDistance));
        Vector2 carToPressurePoint = (Vector2) pressurePoint.subCopy(myPositonFlat);

        LocalDateTime hurryUp = input.time.plus(TimeUtil.toDuration(.2));

        boolean hasLineOfSight = pushDirection.normaliseCopy().dotProduct(carToPressurePoint.normaliseCopy()) > -.2 || input.ballPosition.z > 2;
        if (!hasLineOfSight) {
            // Steer toward a farther-back waypoint.
            Vector2 fallBack = new Vector2(pushDirection.y, -pushDirection.x);
            if (fallBack.dotProduct(ballToGoal) > 0) {
                fallBack.scale(-1);
            }
            fallBack.normalise();
            fallBack.scale(5);

            return Optional.of(SteerUtil.getThereOnTime(input, new SpaceTime(new Vector3(fallBack.x, fallBack.y, 0), hurryUp)));
        }

        AgentOutput dribble = SteerUtil.getThereOnTime(input, new SpaceTime(new Vector3(pressurePoint.x, pressurePoint.y, 0), hurryUp));
        if (carToPressurePoint.normaliseCopy().dotProduct(ballToGoal.normaliseCopy()) > .80 && flatDistance > 3 && flatDistance < 5 && input.ballPosition.z < 2 && approachDistance < 2) {
            dribble.withAcceleration(1).withBoost();
        }
        return Optional.of(dribble);
    }

    public static boolean canDribble(AgentInput input, boolean log) {
        if (input.getMyPosition().distance(input.ballPosition) > DRIBBLE_DISTANCE) {
            // It got away from us
            if (log) {
                BotLog.println("Too far to dribble", input.team);
            }
            return false;
        }

        if (input.ballPosition.subCopy(input.getMyPosition()).dotProduct(GoalUtil.getEnemyGoal(input.team).navigationSpline.getLocation().subCopy(input.ballPosition)) < -.6) {
            // Wrong side of ball
            if (log) {
                BotLog.println("Wrong side of ball for dribble", input.team);
            }
            return false;
        }

        if (VectorUtil.flatDistance(input.getMyVelocity(), input.ballVelocity) > 50) {
            if (log) {
                BotLog.println("Velocity too different to dribble.", input.team);
            }
            return false;
        }

        if (input.ballPosition.z > 10) {
            if (log) {
                BotLog.println("Ball too high to dribble", input.team);
            }
            return false;
        }

        if (input.getMyPosition().z > 5) {
            if (log) {
                BotLog.println("Car too high to dribble", input.team);
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
        return "Dribbling";
    }
}
