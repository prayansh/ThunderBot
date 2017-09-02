package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.SteerUtil;

public class GoForKickoffStep implements Step {
    private boolean isComplete = false;

    public AgentOutput getOutput(AgentInput input) {

        if (VectorUtil.flatten(input.ballPosition).magnitudeSquared() > 0) {
            isComplete = true;
        }

        Vector3 target = new Vector3(0, 0, 0);
        return SteerUtil.steerTowardPosition(input, target);
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
        return "Going for kickoff!";
    }
}
