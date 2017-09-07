package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;

import java.util.Optional;

public class TapStep implements Step {
    private AgentOutput output;
    private int numFrames;
    private int frameCount;

    public TapStep(AgentOutput output) {
        this(1, output);
    }

    public TapStep(int numFrames, AgentOutput output) {
        this.output = output;
        this.numFrames = numFrames;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {
        frameCount++;
        if (frameCount > numFrames) {
            return Optional.empty();
        }
        return Optional.of(output);
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
        return "Muscle memory";
    }
}
