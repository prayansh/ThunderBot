package tarehart.rlbot;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.input.CarData;
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
import tarehart.rlbot.steps.strikes.*;
import tarehart.rlbot.steps.wall.DescendFromWallStep;
import tarehart.rlbot.steps.wall.MountWallStep;
import tarehart.rlbot.steps.wall.WallTouchStep;
import tarehart.rlbot.tuning.BallTelemetry;
import tarehart.rlbot.tuning.BotLog;
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
        BallTelemetry.setPath(ballPath);

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
        readout.update(input, posture, situation, BotLog.collect(input.team), BallTelemetry.getPath().get());
        BallTelemetry.reset();
        return output;
    }

    private AgentOutput getOutput(AgentInput input) {

        final CarData car = input.getMyCarData();

//        if (canInterruptPlanFor(Plan.Posture.OVERRIDE)) {
//            currentPlan = new Plan(Plan.Posture.OVERRIDE).withStep(new TagAlongStep());
//            currentPlan.begin();
//        }

        // Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (VectorUtil.flatten(input.ballPosition).magnitudeSquared() == 0) {
            currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new GoForKickoffStep());
            currentPlan.begin();
        }

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(5));
        if (canInterruptPlanFor(Plan.Posture.SAVE)) {
            Optional<SpaceTimeVelocity> scoredOn = GoalUtil.predictGoalEvent(GoalUtil.getOwnGoal(input.team), ballPath);
            if (scoredOn.isPresent()) {
                BotLog.println("Going for save", input.team);
                currentPlan = new Plan(Plan.Posture.SAVE).withStep(new DirectedKickStep(new KickAwayFromOwnGoal()));
                currentPlan.begin();
            }
        }

        if (canInterruptPlanFor(Plan.Posture.CLEAR)) {
            boolean ballEntersOurBox = GoalUtil.ballEntersBox(GoalUtil.getOwnGoal(input.team), ballPath, Duration.ofSeconds(5));
            if (ballEntersOurBox) {
                BotLog.println("Going for clear", input.team);
                currentPlan = new Plan(Plan.Posture.CLEAR).withStep(new DirectedKickStep(new KickAwayFromOwnGoal()));
                currentPlan.begin();
            }
        }

        if (canInterruptPlanFor(Plan.Posture.DEFENSIVE)) {
            if (GetOnDefenseStep.needDefense(input)) {
                BotLog.println("Going on defense", input.team);
                currentPlan = new Plan(Plan.Posture.DEFENSIVE).withStep(new GetOnDefenseStep());
                currentPlan.begin();
            } else if (ArenaModel.isBehindGoalLine(car.position)) {
                currentPlan = new Plan(Plan.Posture.DEFENSIVE).withStep(new EscapeTheGoalStep());
                currentPlan.begin();
            }
        }

        if (canInterruptPlanFor(Plan.Posture.SHOT)) {
            boolean ballEntersEnemyBox = GoalUtil.ballEntersBox(GoalUtil.getEnemyGoal(input.team), ballPath, Duration.ofSeconds(2));
            if (ballEntersEnemyBox) {
                BotLog.println("Going for shot", input.team);
                currentPlan = new Plan(Plan.Posture.SHOT).withStep(new DirectedKickStep(new KickAtEnemyGoal()));
                currentPlan.begin();
            }
        }

        if (currentPlan == null || currentPlan.isComplete()) {
            if (car.position.z > LandGracefullyStep.NEEDS_LANDING_HEIGHT) {
                currentPlan = new Plan(Plan.Posture.LANDING).withStep(new LandGracefullyStep());
                currentPlan.begin();
            } else if (DribbleStep.canDribble(input, false)) {
                BotLog.println("Beginning dribble", input.team);
                currentPlan = new Plan().withStep(new DribbleStep());
                currentPlan.begin();
            } else if (car.boost < 30 && GetBoostStep.canRun(car)) {
                currentPlan = new Plan().withStep(new GetBoostStep());
                currentPlan.begin();
            } else if (WallTouchStep.hasWallTouchOpportunity(input, ballPath)) {
                currentPlan = new Plan().withStep(new MountWallStep()).withStep(new WallTouchStep()).withStep(new DescendFromWallStep());
                currentPlan.begin();
            } else if (DirectedKickStep.canMakeDirectedKick(input)) {
                currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new DirectedKickStep(new KickAtEnemyGoal()));
                currentPlan.begin();
            }
            else if (GetOnDefenseStep.getWrongSidedness(input) > 0) {
                BotLog.println("Getting behind the ball", input.team);
                currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new GetOnOffenseStep());
                currentPlan.begin();
            } else {
                currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new InterceptStep(new Vector3()));
                currentPlan.begin();
            }
        }

        if (currentPlan != null) {
            if (currentPlan.isComplete()) {
                currentPlan = null;
            } else {
                Optional<AgentOutput> output = currentPlan.getOutput(input);
                if (output.isPresent()) {
                    return output.get();
                }
            }
        }

        return SteerUtil.steerTowardGroundPosition(car, input.ballPosition);
    }

    private boolean canInterruptPlanFor(Plan.Posture posture) {
        return currentPlan == null || currentPlan.getPosture().lessUrgentThan(posture) && currentPlan.canInterrupt();
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
