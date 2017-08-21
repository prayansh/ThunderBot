package tarehart.rlbot;

import mikera.vectorz.Vector3;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.steps.GetBoostStep;
import tarehart.rlbot.steps.GetOnDefenseStep;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

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

        if (input.ballPosition.z > 5) {
            System.out.println(String.format("P: %s\tV: %s\tT: %s", input.ballPosition.z, input.ballVelocity.z, new Date().getTime()));
        }

        if (GetOnDefenseStep.needDefense(input)) {
            currentPlan = new Plan().withStep(new GetOnDefenseStep());
            currentPlan.begin();
        }

        if (currentPlan == null || currentPlan.isComplete()) {

            currentPlan = SetPieces.chaseBall();
            currentPlan.begin();

            if (Math.random() < .01) {
                System.out.println("AERIAL!");
                currentPlan = SetPieces.getBoostAndAerial();
                currentPlan.begin();
            } else if (Math.random() < .05) {
                System.out.println("Going for boost!");
                currentPlan = new Plan().withStep(new GetBoostStep());
                currentPlan.begin();
            } else {
                System.out.println("Executing ball chase!");
                currentPlan = SetPieces.chaseBall();
                currentPlan.begin();
            }
        }

        if (currentPlan != null) {
            if (currentPlan.isComplete()) {
                System.out.println("Finished plan.");
                currentPlan = null;
            } else {
                return currentPlan.getOutput(input);
            }
        }

        return new AgentOutput();
    }
}
