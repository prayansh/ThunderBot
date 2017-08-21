package tarehart.rlbot.planning;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.steps.Step;

import java.util.ArrayList;

public class Plan {

    private ArrayList<Step> steps = new ArrayList<>();
    private int currentStepIndex = 0;
    private boolean hasBegun = false;
    private boolean isComplete = false;

    public Plan() {
    }

    public Plan withStep(Step step) {
        steps.add(step);
        return this;
    }

    public Plan withSubPlan(Plan plan) {
        steps.addAll(plan.steps);
        return this;
    }

    public void begin() {
        hasBegun = true;
        steps.get(currentStepIndex).begin();
    }

    public AgentOutput getOutput(AgentInput input) {

        if (!hasBegun) {
            throw new RuntimeException("Need to call begin on plan!");
        }

        if (isComplete) {
            throw new RuntimeException("Plan is already complete!");
        }

        Step currentStep = steps.get(currentStepIndex);
        if (currentStep.isComplete()) {
            currentStepIndex++;
            currentStep = steps.get(currentStepIndex);
            currentStep.begin();
        }

        return currentStep.getOutput(input);
    }

    public boolean isComplete() {
        if (isComplete) {
            return true;
        } else if (currentStepIndex == steps.size() - 1 &&
                steps.get(currentStepIndex).isComplete()) {
            isComplete = true;
            return true;
        }
        return isComplete;
    }

}
