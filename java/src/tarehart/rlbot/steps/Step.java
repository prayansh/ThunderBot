package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.planning.PlanOutput;

import java.time.LocalDateTime;

public interface Step {

    AgentOutput getOutput(AgentInput input);
    boolean isComplete();
    void begin();
}
