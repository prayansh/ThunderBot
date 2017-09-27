package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.steps.Step;

import java.util.Optional;

public class IdealDirectedHitStep implements Step {

    private final KickStrategy kickStrategy;
    private Step proxyStep;

    public IdealDirectedHitStep(KickStrategy kickStrategy) {
        this.kickStrategy = kickStrategy;
        proxyStep = new DirectedNoseHitStep(kickStrategy);
    }

    @Override
    public Optional<AgentOutput> getOutput(AgentInput input) {

        Optional<AgentOutput> output = proxyStep.getOutput(input);

        if (proxyStep instanceof DirectedNoseHitStep) {
            double estimatedAngleOfKickFromApproach = ((DirectedNoseHitStep) proxyStep).getEstimatedAngleOfKickFromApproach();
            if (Math.abs(estimatedAngleOfKickFromApproach) > Math.PI / 2) {
                proxyStep = new DirectedSideHitStep(kickStrategy);
                return proxyStep.getOutput(input);
            }
        }

        return output;
    }

    @Override
    public boolean isBlindlyComplete() {
        return proxyStep.isBlindlyComplete();
    }

    @Override
    public void begin() {

    }

    @Override
    public String getSituation() {
        return proxyStep.getSituation();
    }

    @Override
    public boolean canInterrupt() {
        return proxyStep.canInterrupt();
    }
}
