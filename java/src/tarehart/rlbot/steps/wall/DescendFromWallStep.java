package tarehart.rlbot.steps.wall;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarData;
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

        CarData car = input.getMyCarData();
        if (!ArenaModel.isCarOnWall(car)) {
            return Optional.empty();
        }

        Vector3 groundBeneathMe = car.position.copy();
        groundBeneathMe.z = 0;

        return Optional.of(SteerUtil.steerTowardWallPosition(car, groundBeneathMe));
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
