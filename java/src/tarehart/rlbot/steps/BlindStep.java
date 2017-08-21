package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;

import java.time.Duration;
import java.time.LocalDateTime;

public class BlindStep implements Step {
    private AgentOutput output;
    private Duration duration;
    private LocalDateTime scheduledEndTime;

    public BlindStep(AgentOutput output, Duration duration) {
        this.output = output;
        this.duration = duration;
    }

    public AgentOutput getOutput(AgentInput input) {
        return output;
    }

    @Override
    public boolean isComplete() {
        return scheduledEndTime != null && LocalDateTime.now().isAfter(scheduledEndTime);
    }

    @Override
    public void begin() {
        scheduledEndTime = LocalDateTime.now().plus(duration);
    }
}
