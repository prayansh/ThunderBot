package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.SplineHandle;
import tarehart.rlbot.math.VectorUtil;
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

        SplineHandle enemyGoal = GoalUtil.getEnemyGoal(input.team);

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(2));
        SpaceTimeVelocity futureMotion = ballPath.getMotionAt(input.time.plusSeconds(2)).get();

        Vector3 goalToBall = (Vector3) futureMotion.getSpace().subCopy(enemyGoal.getLocation());
        Vector3 target = futureMotion.getSpace().clone();
        Vector3 goalToBallNormal = (Vector3) goalToBall.normaliseCopy();
        target.add(goalToBallNormal.scaleCopy(30));

        target.x = clamp(target.x, -ArenaModel.SIDE_WALL + SIDE_WALL_BUFFER, ArenaModel.SIDE_WALL - SIDE_WALL_BUFFER);
        target.y = clamp(target.y, -ArenaModel.BACK_WALL + BACK_WALL_BUFFER, ArenaModel.BACK_WALL - BACK_WALL_BUFFER);

        Vector3 carToTarget = (Vector3) target.subCopy(input.getMyPosition());
        Vector3 carToTargetNormal = (Vector3) carToTarget.normaliseCopy();
        if (carToTarget.magnitude() < 20 || carToTarget.magnitude() < 50 && input.getMyVelocity().dotProduct(carToTargetNormal) > 30) {
            isComplete = true;
        }

        if (target.distance(input.getMyPosition()) > 40 && Math.abs(SteerUtil.getCorrectionAngleRad(input, target)) < Math.PI / 20
                && input.getMyVelocity().magnitude() > SteerUtil.MAX_SPEED / 4) {

            BotLog.println("Front flipping into offense!", input.team);
            plan = SetPieces.frontFlip();
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
