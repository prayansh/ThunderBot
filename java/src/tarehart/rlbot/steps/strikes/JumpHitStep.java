package tarehart.rlbot.steps.strikes;

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

public class JumpHitStep implements Step {

    private Plan plan;
    private boolean isComplete;
    private boolean startedStrike;

    public AgentOutput getOutput(AgentInput input) {

        if (input.getMyPosition().z > 1) {
            isComplete = true;
        }

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

        double currentSpeed = input.getMyVelocity().magnitude();
        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(3));

        List<SpaceTime> currentIntercepts = SteerUtil.getInterceptOpportunitiesAssumingMaxAccel(input, ballPath, input.getMyBoost());
        if (currentIntercepts.size() > 0) {

            SpaceTime intercept = currentIntercepts.get(0);

            if (intercept.space.z > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD) {
                BotLog.println("JumpHitStep failing because ball will be too high!.", input.team);
                isComplete = true;
                return new AgentOutput();
            }

            LaunchChecklist checklist = AirTouchPlanner.checkJumpHitReadiness(input, currentIntercepts.get(0));
            if (checklist.readyToLaunch()) {
                plan = SetPieces.performJumpHit(intercept.space.z);
                plan.begin();
                return plan.getOutput(input);
            } else if (!checklist.notTooClose) {
                double nextSpeed = currentSpeed * .9;

                List<SpaceTime> slowerIntercepts = SteerUtil.getInterceptOpportunities(input, ballPath, nextSpeed);
                if (slowerIntercepts.size() > 0) {

                    return SteerUtil.getThereOnTime(input, slowerIntercepts.get(0));
                }
            } else if (!checklist.closeEnough) {
                BotLog.println("JumpHit failing because we are not close enough", input.team);
                isComplete = true;
            }
            else {
                return getThereAsap(input, currentIntercepts.get(0));
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
            BotLog.println("Front flip for JumpHit", input.team);
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
        return "Preparing for JumpHit";
    }
}
