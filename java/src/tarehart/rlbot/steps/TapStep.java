package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;

public class TapStep implements Step {
    private AgentOutput output;
    private boolean isComplete;
    private int numFrames;
    private int frameCount;

    public TapStep(AgentOutput output) {
        this(1, output);
    }

    public TapStep(int numFrames, AgentOutput output) {
        this.output = output;
        this.numFrames = numFrames;
    }

    public AgentOutput getOutput(AgentInput input) {
        frameCount++;
        if (frameCount == numFrames) {
            isComplete = true;
        }
        return output;
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void begin() {
    }
}
