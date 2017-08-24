package tarehart.rlbot;

import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.steps.GetBoostStep;
import tarehart.rlbot.steps.GetOnDefenseStep;
import tarehart.rlbot.tuning.PredictionWarehouse;
import tarehart.rlbot.tuning.Telemetry;
import tarehart.rlbot.ui.Readout;

import javax.swing.*;
import java.time.Duration;
import java.time.LocalDateTime;

public class Bot {

    private final Team team;
    Plan currentPlan = null;
    private Readout readout;

    private ArenaModel arenaModel;

    public enum Team {
        BLUE,
        ORANGE
    }

    public Bot(Team team) {
        this.team = team;
        readout = new Readout();
        launchReadout();
        arenaModel = new ArenaModel();
    }


    public AgentOutput processInput(AgentInput input) {

        // Just for now, always calculate ballpath so we can learn some stuff.
        BallPath ballPath = arenaModel.simulateBall(input.ballPosition, input.ballVelocity, input.time, Duration.ofSeconds(5));
        Telemetry.forTeam(input.team).setBallPath(ballPath);

        AgentOutput output = getOutput(input);
        Plan.Posture posture = currentPlan != null ? currentPlan.getPosture() : Plan.Posture.NEUTRAL;
        readout.update(input, posture, Telemetry.forTeam(input.team).getBallPath());
        Telemetry.forTeam(team).reset();
        return output;
    }

    private AgentOutput getOutput(AgentInput input) {
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


    private void launchReadout() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("Readout - " + team.name());
        frame.setContentPane(readout.getRootPanel());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
