package tarehart.rlbot;

import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.*;
import tarehart.rlbot.steps.defense.GetOnDefenseStep;
import tarehart.rlbot.steps.defense.WhatASaveStep;
import tarehart.rlbot.steps.landing.LandGracefullyStep;
import tarehart.rlbot.tuning.BotLog;
import tarehart.rlbot.tuning.Telemetry;
import tarehart.rlbot.ui.Readout;

import javax.swing.*;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class Bot {

    private final Team team;
    Plan currentPlan = null;
    private Readout readout;
    private String previousSituation = null;

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
        BallPath ballPath = arenaModel.simulateBall(new SpaceTimeVelocity(input.ballPosition, input.time, input.ballVelocity), Duration.ofSeconds(5));
        Telemetry.forTeam(input.team).setBallPath(ballPath);

        //BallRecorder.recordPosition(new SpaceTimeVelocity(input.ballPosition, input.time, input.ballVelocity));
        //Optional<SpaceTimeVelocity> afterBounce = ballPath.getMotionAfterWallBounce(1);
        // Just for data gathering / debugging.
        //afterBounce.ifPresent(stv -> BallRecorder.startRecording(new SpaceTimeVelocity(input.ballPosition, input.time, input.ballVelocity), stv.getTime().plusSeconds(1)));


        AgentOutput output = getOutput(input);
        Plan.Posture posture = currentPlan != null ? currentPlan.getPosture() : Plan.Posture.NEUTRAL;
        String situation = currentPlan != null ? currentPlan.getSituation() : "";
        if (!Objects.equals(situation, previousSituation)) {
            BotLog.println("[Sitch] " + situation, input.team);
        }
        previousSituation = situation;
        readout.update(input, posture, situation, BotLog.collect(input.team), Telemetry.forTeam(input.team).getBallPath());
        Telemetry.forTeam(team).reset();
        return output;
    }

    private AgentOutput getOutput(AgentInput input) {

        // Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (VectorUtil.flatten(input.ballPosition).magnitudeSquared() == 0) {
            currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new GoForKickoffStep());
            currentPlan.begin();
            return currentPlan.getOutput(input);
        }

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(5));
        if (currentPlan == null || currentPlan.getPosture().lessUrgentThan(Plan.Posture.SAVE)) {
            Optional<SpaceTimeVelocity> scoredOn = GoalUtil.predictGoalEvent(GoalUtil.getOwnGoal(input.team), ballPath);
            if (scoredOn.isPresent()) {
                currentPlan = new Plan(Plan.Posture.SAVE).withStep(new WhatASaveStep(scoredOn.get()));
                currentPlan.begin();
                return currentPlan.getOutput(input);
            }
        }

        if (currentPlan == null || currentPlan.getPosture().lessUrgentThan(Plan.Posture.DEFENSIVE)) {
            if (GetOnDefenseStep.needDefense(input)) {
                BotLog.println("Going on defense", input.team);
                currentPlan = new Plan(Plan.Posture.DEFENSIVE).withStep(new GetOnDefenseStep());
                currentPlan.begin();
            } else if (ArenaModel.isBehindGoalLine(input.getMyPosition())) {
                currentPlan = new Plan(Plan.Posture.DEFENSIVE).withStep(new EscapeTheGoalStep());
                currentPlan.begin();
            }
        }

        if (currentPlan == null || currentPlan.isComplete()) {
            BotLog.println("Making fresh plans", input.team);
            if (input.getMyPosition().z > 1) {
                currentPlan = new Plan().withStep(new LandGracefullyStep()).withStep(new ChaseBallStep());
                currentPlan.begin();
            } else if (input.getMyBoost() < 30 && GetBoostStep.canRun(input)) {
                currentPlan = new Plan().withStep(new GetBoostStep());
                currentPlan.begin();
            } else if (GetOnDefenseStep.getWrongSidedness(input) > 0) {
                BotLog.println("Getting behind the ball", input.team);
                currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new GetOnOffenseStep()).withStep(new ChaseBallStep());
                currentPlan.begin();
            } else {
                currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new ChaseBallStep());
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
