package tarehart.rlbot.planning;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.steps.strikes.AsapAerialStep;
import tarehart.rlbot.steps.strikes.FlipHitStep;
import tarehart.rlbot.steps.strikes.JumpHitStep;

import java.util.Optional;
import java.util.Set;

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

    public static Optional<Plan> planImmediateLaunch(AgentInput input, SpaceTime intercept) {


        if (intercept.space.z > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD) {
            AerialChecklist checklist = AirTouchPlanner.checkAerialReadiness(input, intercept);
            if (checklist.readyToLaunch()) {
                return Optional.of(SetPieces.performAerial());
            }
            return Optional.empty();
        }

        if (intercept.space.z > AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD && AirTouchPlanner.isJumpHitAccessible(input, intercept)) {
            LaunchChecklist checklist = AirTouchPlanner.checkJumpHitReadiness(input, intercept);
            if (checklist.readyToLaunch()) {
                return Optional.of(SetPieces.performJumpHit(intercept.space.z));
            }
            return Optional.empty();
        }

        if (intercept.space.z > AirTouchPlanner.NEEDS_FRONT_FLIP_THRESHOLD && AirTouchPlanner.isFlipHitAccessible(input, intercept)) {
            LaunchChecklist checklist = AirTouchPlanner.checkFlipHitReadiness(input, intercept);
            if (checklist.readyToLaunch()) {
                return Optional.of(SetPieces.frontFlip());
            }
            return Optional.empty();
        }

        return Optional.empty();
    }
}
