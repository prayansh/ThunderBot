package tarehart.rlbot.planning;

import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.tuning.Telemetry;

public class PlanOutput {
    private AgentOutput agentOutput;
    private Telemetry telemetry;

    public PlanOutput withAgentOutput(AgentOutput agentOutput) {
        this.agentOutput = agentOutput;
        return this;
    }

    public PlanOutput withTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        return this;
    }

    public AgentOutput getAgentOutput() {
        return agentOutput;
    }
}
