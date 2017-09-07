package tarehart.rlbot.planning;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.steps.Step;

import java.util.ArrayList;
import java.util.Optional;

public class Plan {

    private final Posture posture;
    private ArrayList<Step> steps = new ArrayList<>();
    private int currentStepIndex = 0;
    private boolean hasBegun = false;
    private boolean isComplete = false;

    public enum Posture {
        NEUTRAL(0),
        DEFENSIVE(5),
        SAVE(10),
        OFFENSIVE(1);

        private int urgency;

        Posture(int urgency) {
            this.urgency = urgency;
        }

        public boolean lessUrgentThan(Posture other) {
            return urgency < other.urgency;
        }
    }

    public Plan() {
        this(Posture.NEUTRAL);
    }

    public Plan(Posture posture) {
        this.posture = posture;
    }

    public Plan withStep(Step step) {
        steps.add(step);
        return this;
    }

    public Plan appendPlan(Plan plan) {
        steps.addAll(plan.steps);
        return this;
    }

    public void begin() {
        hasBegun = true;
        steps.get(currentStepIndex).begin();
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (!hasBegun) {
            throw new RuntimeException("Need to call begin on plan!");
        }

        if (isComplete) {
            throw new RuntimeException("Plan is already complete!");
        }

        while (currentStepIndex < steps.size()) {
            Step currentStep = steps.get(currentStepIndex);
            if (currentStep.isBlindlyComplete()) {
                nextStep();
                continue;
            }

            Optional<AgentOutput> output = currentStep.getOutput(input);
            if (output.isPresent()) {
                return output;
            }

            nextStep();
        }

        isComplete = true;
        return Optional.empty();
    }

    private void nextStep() {
        currentStepIndex++;
        steps.get(currentStepIndex).begin();
    }

    public String getSituation() {
        return (currentStepIndex + 1) + ". " + steps.get(currentStepIndex).getSituation();
    }

    public boolean isComplete() {
        if (isComplete) {
            return true;
        } else if (currentStepIndex == steps.size() - 1 &&
                steps.get(currentStepIndex).isBlindlyComplete()) {
            isComplete = true;
            return true;
        }
        return isComplete;
    }

    public Posture getPosture() {
        return posture;
    }
}
