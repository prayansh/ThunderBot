package tarehart.rlbot.steps.landing;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.wall.DescendFromWallStep;

import java.util.Optional;

public class LandMindlesslyStep implements Step {

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (input.getMyPosition().z < .40f || ArenaModel.isCarNearWall(input) && input.getMyPosition().z < 5) {
            return Optional.empty();
        }

        if (ArenaModel.isCarOnWall(input)) {
            Vector3 groundBeneathMe = input.getMyPosition().copy();
            groundBeneathMe.z = 0;
            return Optional.of(SteerUtil.steerTowardWallPosition(input, groundBeneathMe));
        }

        return Optional.of(new AgentOutput().withAcceleration(1));
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
        return "Waiting to land";
    }
}
