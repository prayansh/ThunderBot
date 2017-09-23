package tarehart.rlbot.steps.wall;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BallTelemetry;

import java.util.Optional;

public class MountWallStep implements Step {

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        if (ArenaModel.isCarOnWall(car)) {
            // Successfully made it onto the wall
            return Optional.empty();
        }

        Optional<BallPath> ballPath = BallTelemetry.getPath();
        if (!ballPath.isPresent() || !WallTouchStep.hasWallTouchOpportunity(input, ballPath.get())) {
            // Failed to mount the wall in time.
            return Optional.empty();
        }

        Vector3 ballPositionExaggerated = (Vector3) input.ballPosition.scaleCopy(1.1); // This assumes the ball is close to the wall

        return Optional.of(SteerUtil.steerTowardGroundPosition(car, ballPositionExaggerated));
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
        return "Mounting the wall";
    }
}
