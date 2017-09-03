package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;

public class LandMindlesslyStep implements Step {

    private boolean isComplete = false;

    public AgentOutput getOutput(AgentInput input) {

        if (input.getMyPosition().z < .40f || LandGracefullyStep.isOnWall(input)) {
            isComplete = true;
        }
        return new AgentOutput().withAcceleration(1);
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
        return "Waiting to land";
    }
}
