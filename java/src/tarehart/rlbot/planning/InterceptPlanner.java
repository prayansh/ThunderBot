package tarehart.rlbot.planning;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.steps.AsapAerialStep;
import tarehart.rlbot.steps.strikes.FlipHitStep;
import tarehart.rlbot.steps.strikes.JumpHitStep;

import java.time.Duration;
import java.util.Optional;

public class InterceptPlanner {

    public static Optional<Plan> planFullSpeedIntercept(AgentInput input, BallPath ballPath) {
        Optional<SpaceTime> interceptOpportunity = SteerUtil.getInterceptOpportunityAssumingMaxAccel(input, ballPath, AirTouchPlanner.getBoostBudget(input));

        if (interceptOpportunity.isPresent()) {

            SpaceTime intercept = interceptOpportunity.get();

            if (intercept.space.z > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD) {

                AerialChecklist checklist = AirTouchPlanner.checkAerialReadiness(input, intercept);

                if(checklist.notTooClose && checklist.closeEnough && checklist.hasBoost) {
                    return Optional.of(new Plan().withStep(new AsapAerialStep()));
                }
                return Optional.empty();
            }

            if (intercept.space.z > AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD) {
                return Optional.of(new Plan().withStep(new JumpHitStep(intercept.space)));
            }

            return Optional.of(new Plan().withStep(new FlipHitStep(intercept.space)));
        }

        return Optional.empty();
    }

}
