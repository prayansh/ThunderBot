package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.planning.SteerUtil;

import java.util.Optional;

public class GoForKickoffStep implements Step {
    private boolean isComplete = false;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (VectorUtil.flatten(input.ballPosition).magnitudeSquared() > 0) {
            isComplete = true;
        }

        Vector3 target = new Vector3(0, 0, 0);
        return Optional.of(SteerUtil.steerTowardGroundPosition(input.getMyCarData(), target));
    }

    @Override
    public boolean isBlindlyComplete() {
        return isComplete;
    }

    @Override
    public void begin() {
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return "Going for kickoff!";
    }
}
