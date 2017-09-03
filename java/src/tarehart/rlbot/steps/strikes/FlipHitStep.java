package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class FlipHitStep implements Step {

    private Plan plan;
    private boolean isComplete;
    private boolean startedStrike = true;

    public AgentOutput getOutput(AgentInput input) {

        if (plan != null) {
            if (plan.isComplete()) {
                if (startedStrike) {
                    isComplete = true;
                }
                plan = null;
            } else {
                return plan.getOutput(input);
            }
        }

        if (input.getMyPosition().z > 1) {
            isComplete = true;
        }

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(3));

        Optional<SpaceTime> currentIntercepts = SteerUtil.getInterceptOpportunityAssumingMaxAccel(input, ballPath, input.getMyBoost());
        if (currentIntercepts.isPresent()) {

            SpaceTime intercept = currentIntercepts.get();

            if (intercept.space.z > AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD) {
                BotLog.println("FlipHitStep failing because ball will be too high!.", input.team);
                isComplete = true;
                return new AgentOutput();
            }

            // Strike the ball such that it goes toward the enemy goal
            Vector3 fromGoal = (Vector3) intercept.space.subCopy(GoalUtil.getEnemyGoal(input.team).getNearestEntrance(intercept.space, 2));
            intercept.space.add(fromGoal.normaliseCopy());
            double distance = input.getMyPosition().distance(intercept.space);

            if (Duration.between(input.time, intercept.time).toMillis() < 500 || distance < 5) {
                plan = SetPieces.frontFlip();
                plan.begin();
                return plan.getOutput(input);
            } else {
                return getThereAsap(input, intercept);
            }
        } else {
            BotLog.println("JumpHitStep failing because there are no max speed intercepts", input.team);
            isComplete = true;
        }

        return new AgentOutput();
    }

    private AgentOutput getThereAsap(AgentInput input, SpaceTime groundPosition) {

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(input, groundPosition);
        if (sensibleFlip.isPresent()) {
            BotLog.println("Front flip to approach FlipHit", input.team);
            this.plan = sensibleFlip.get();
            this.plan.begin();
            return this.plan.getOutput(input);
        }

        return SteerUtil.steerTowardPosition(input, groundPosition.space);
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
        return "Preparing for FlipHit";
    }
}
