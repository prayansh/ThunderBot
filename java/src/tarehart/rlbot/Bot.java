package tarehart.rlbot;

import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.steps.GetBoostStep;
import tarehart.rlbot.steps.GetOnDefenseStep;

public class Bot {

    private final Team team;
    Plan currentPlan = null;

    public enum Team {
        BLUE,
        ORANGE
    }

    public Bot(Team team) {
        this.team = team;
    }


    public AgentOutput getOutput(AgentInput input) {

        if (GetOnDefenseStep.needDefense(input) && (currentPlan == null || currentPlan.getPosture() != Plan.Posture.DEFENSIVE)) {
            currentPlan = new Plan(Plan.Posture.DEFENSIVE).withStep(new GetOnDefenseStep());
            currentPlan.begin();
        }

        if (currentPlan == null || currentPlan.isComplete()) {
            if (input.getMyBoost() < 30 && input.getMyPosition().distance(input.ballPosition) > 80) {
                currentPlan = new Plan().withStep(new GetBoostStep());
                currentPlan.begin();
            } else {
                currentPlan = SetPieces.chaseBall();
                currentPlan.begin();
            }
        }

        if (currentPlan != null) {
            if (currentPlan.isComplete()) {
                currentPlan = null;
            } else {
                return currentPlan.getOutput(input);
            }
        }

        return new AgentOutput();
    }
}
