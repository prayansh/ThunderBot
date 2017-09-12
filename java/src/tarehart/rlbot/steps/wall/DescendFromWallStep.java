package tarehart.rlbot.steps.wall;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.Step;

import java.util.Optional;

import static tarehart.rlbot.planning.SteerUtil.SUPERSONIC_SPEED;

public class DescendFromWallStep implements Step {

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (!ArenaModel.isCarOnWall(input)) {
            return Optional.empty();
        }

        Vector3 groundBeneathMe = input.getMyPosition().copy();
        groundBeneathMe.z = 0;

        return Optional.of(SteerUtil.steerTowardWallPosition(input, groundBeneathMe));
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
        return "Descending wall.";
    }
}
