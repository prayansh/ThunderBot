package tarehart.rlbot.planning;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.SpaceTime;

import java.util.Optional;

public class InterceptPlanner {

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
