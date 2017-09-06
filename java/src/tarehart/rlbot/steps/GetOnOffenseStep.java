package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.*;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.Optional;

public class GetOnOffenseStep implements Step {
    public static final int BACK_WALL_BUFFER = 0;
    public static final int SIDE_WALL_BUFFER = 0;
    private boolean isComplete = false;

    private Plan plan;

    public AgentOutput getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        SplineHandle enemyGoal = GoalUtil.getEnemyGoal(input.team).navigationSpline;

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(2));

        Vector3 target = input.ballPosition.copy();
        if (input.ballVelocity.y * (enemyGoal.getLocation().y - input.ballPosition.y) < 0) {
            // if ball is rolling away from the enemy goal
            SpaceTimeVelocity futureMotion = ballPath.getMotionAt(input.time.plusSeconds(2)).get();
            target = futureMotion.getSpace().clone();
        }

        if (Math.abs(target.x) < 20) {
            Vector3 goalToBall = (Vector3) target.subCopy(enemyGoal.getLocation());
            Vector3 goalToBallNormal = (Vector3) goalToBall.normaliseCopy();
            target.add(goalToBallNormal.scaleCopy(10));
        }

        target.x = clamp(target.x, -ArenaModel.SIDE_WALL + SIDE_WALL_BUFFER, ArenaModel.SIDE_WALL - SIDE_WALL_BUFFER);
        target.y = clamp(target.y, -ArenaModel.BACK_WALL + BACK_WALL_BUFFER, ArenaModel.BACK_WALL - BACK_WALL_BUFFER);

        Vector3 carToTarget = (Vector3) target.subCopy(input.getMyPosition());
        double flatDistance = VectorUtil.flatDistance(target, input.getMyPosition());
        Vector3 carToTargetNormal = (Vector3) carToTarget.normaliseCopy();
        if (flatDistance < 20 || flatDistance < 50 && input.getMyVelocity().dotProduct(carToTargetNormal) > 30) {
            isComplete = true;
        }

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(input, target);
        if (sensibleFlip.isPresent()) {
            BotLog.println("Front flipping into offense!", input.team);
            plan = sensibleFlip.get();
            plan.begin();
            return plan.getOutput(input);
        } else {
            return SteerUtil.steerTowardPosition(input, target).withBoost(false);
        }
    }

    private double clamp(double value, double min, double max) {
        if (value > max) return max;
        if (value < min) return min;
        return value;
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void begin() {
    }

    @Override
    public String getSituation() {
        return "Getting on offense";
    }
}
