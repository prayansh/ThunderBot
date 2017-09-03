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
import java.util.Optional;

public class JumpHitStep implements Step {

    private Plan plan;
    private boolean isComplete;
    private boolean startedStrike;
    private Vector3 originalIntercept;

    public JumpHitStep(Vector3 originalIntercept) {
        this.originalIntercept = originalIntercept;
    }

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

        Optional<SpaceTime> interceptOpportunity = SteerUtil.getInterceptOpportunityAssumingMaxAccel(input, ballPath, input.getMyBoost());
        if (interceptOpportunity.isPresent()) {

            SpaceTime intercept = interceptOpportunity.get();

            if (intercept.space.distance(originalIntercept) > 10 && Duration.between(input.time, intercept.time).toMillis() > 1000) {
                BotLog.println("JumpHitStep failing because we lost sight of the original plan.", input.team);
                isComplete = true;
                return new AgentOutput();
            }

            if (intercept.space.z > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD || intercept.space.z < AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD) {

                Optional<SpaceTime> volleyOpportunity = SteerUtil.getVolleyOpportunity(input, ballPath, input.getMyBoost(), AirTouchPlanner.NEEDS_AERIAL_THRESHOLD);
                if (volleyOpportunity.isPresent()) {

                    return SteerUtil.getThereOnTime(input, volleyOpportunity.get());
                }

                BotLog.println("JumpHitStep failing because ball will be too high and we can't find a volley.", input.team);
                isComplete = true;
                return new AgentOutput();
            }

            LaunchChecklist checklist = AirTouchPlanner.checkJumpHitReadiness(input, intercept);
            if (checklist.readyToLaunch()) {
                startedStrike = true;
                plan = SetPieces.performJumpHit(intercept.space.z);
                plan.begin();
                return plan.getOutput(input);
            } else if (!checklist.closeEnough) {
                BotLog.println("JumpHit failing because we are not close enough", input.team);
                isComplete = true;
            } else {
                return getThereAsap(input, interceptOpportunity.get());
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
