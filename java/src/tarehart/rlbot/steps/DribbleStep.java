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
import tarehart.rlbot.physics.BallPhysics;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class DribbleStep implements Step {

    public static final double DRIBBLE_DISTANCE = 20;

    private Plan plan;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        CarData car = input.getMyCarData();

        if (!canDribble(input, true)) {
            return Optional.empty();
        }

        Vector2 myPositonFlat = VectorUtil.flatten(car.position);
        Vector2 myDirectionFlat = VectorUtil.flatten(car.rotation.noseVector);
        Vector2 ballPositionFlat = VectorUtil.flatten(input.ballPosition);
        Vector2 ballVelocityFlat = VectorUtil.flatten(input.ballVelocity);
        Vector2 toBallFlat = (Vector2) ballPositionFlat.subCopy(myPositonFlat);
        double flatDistance = toBallFlat.magnitude();

        double ballSpeed = ballVelocityFlat.magnitude();
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
        double approachDistance = 0;

        if (ballSpeed > 10) {
            double velocityCorrectionAngle = SteerUtil.getCorrectionAngleRad(ballVelocityFlat, ballToGoal);
            double angleTweak = Math.min(Math.PI / 6, Math.max(-Math.PI / 6, velocityCorrectionAngle * 2));
            pushDirection = (Vector2) VectorUtil.rotateVector(ballToGoal, angleTweak).normaliseCopy();
            approachDistance = VectorUtil.project(toBallFlat, new Vector2(pushDirection.y, -pushDirection.x)).magnitude() * 1.6 + .8;
            approachDistance = Math.min(approachDistance, 4);
            pressurePoint = (Vector2) futureBallPosition.subCopy(pushDirection.normaliseCopy().scaleCopy(approachDistance));
        } else {
            pushDirection = (Vector2) ballToGoal.normaliseCopy();
            pressurePoint = (Vector2) futureBallPosition.subCopy(pushDirection);
        }


        Vector2 carToPressurePoint = (Vector2) pressurePoint.subCopy(myPositonFlat);
        Vector2 carToBall = (Vector2) futureBallPosition.subCopy(myPositonFlat);

        LocalDateTime hurryUp = input.time.plus(TimeUtil.toDuration(leadSeconds));

        boolean hasLineOfSight = pushDirection.normaliseCopy().dotProduct(carToBall.normaliseCopy()) > -.2 || input.ballPosition.z > 2;
        if (!hasLineOfSight) {
            // Steer toward a farther-back waypoint.
            Vector2 fallBack = new Vector2(pushDirection.y, -pushDirection.x);
            if (fallBack.dotProduct(ballToGoal) > 0) {
                fallBack.scale(-1);
            }
            fallBack.normalise();
            fallBack.scale(5);

            return Optional.of(SteerUtil.getThereOnTime(car, new SpaceTime(new Vector3(fallBack.x, fallBack.y, 0), hurryUp)));
        }

        AgentOutput dribble = SteerUtil.getThereOnTime(car, new SpaceTime(new Vector3(pressurePoint.x, pressurePoint.y, 0), hurryUp));
        if (carToPressurePoint.normaliseCopy().dotProduct(ballToGoal.normaliseCopy()) > .80 &&
                flatDistance > 3 && flatDistance < 5 && input.ballPosition.z < 2 && approachDistance < 2
                && SteerUtil.getCorrectionAngleRad(myDirectionFlat, carToPressurePoint) < Math.PI / 12) {
            if (car.boost > 0) {
                dribble.withAcceleration(1).withBoost();
            } else {
                plan = SetPieces.frontFlip();
                plan.begin();
                return plan.getOutput(input);
            }
        }
        return Optional.of(dribble);
    }

    public static boolean canDribble(AgentInput input, boolean log) {

        CarData car = input.getMyCarData();
        Vector3 ballToMe = (Vector3) car.position.subCopy(input.ballPosition);

        if (ballToMe.magnitude() > DRIBBLE_DISTANCE) {
            // It got away from us
            if (log) {
                BotLog.println("Too far to dribble", input.team);
            }
            return false;
        }

        if (input.ballPosition.subCopy(car.position).normaliseCopy().dotProduct(
                GoalUtil.getOwnGoal(input.team).navigationSpline.getLocation().subCopy(input.ballPosition).normaliseCopy()) > .9) {
            // Wrong side of ball
            if (log) {
                BotLog.println("Wrong side of ball for dribble", input.team);
            }
            return false;
        }

        if (VectorUtil.flatDistance(car.velocity, input.ballVelocity) > 30) {
            if (log) {
                BotLog.println("Velocity too different to dribble.", input.team);
            }
            return false;
        }

//        if (ballToMe.dotProduct(input.ballVelocity) > 0) {
//            if (log) {
//                BotLog.println("Ball is rolling toward us, can't dribble.", input.team);
//            }
//            return false;
//        }

        if (BallPhysics.getGroundBounceEnergy(new SpaceTimeVelocity(input.ballPosition, input.time, input.ballVelocity)) > 50) {
            if (log) {
                BotLog.println("Ball bouncing too hard to dribble", input.team);
            }
            return false;
        }

        if (car.position.z > 5) {
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
