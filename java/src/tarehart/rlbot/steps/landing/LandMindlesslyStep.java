package tarehart.rlbot.steps.landing;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.steps.Step;

import java.util.Optional;

public class LandMindlesslyStep implements Step {

    private boolean isComplete = false;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (input.getMyPosition().z < .40f || LandGracefullyStep.isOnWall(input)) {
            isComplete = true;
        }
        return Optional.of(new AgentOutput().withAcceleration(1));
    }

    @Override
    public boolean isBlindlyComplete() {
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
