package tarehart.rlbot;

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
import tarehart.rlbot.steps.debug.CalibrateStep;
import tarehart.rlbot.steps.defense.GetOnDefenseStep;
import tarehart.rlbot.steps.defense.ThreatAssessor;
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

public class ReliefBot extends Bot {

    public ReliefBot(Team team) {
        super(team);
    }

    @Override
    protected AgentOutput getOutput(AgentInput input) {

        final CarData car = input.getMyCarData();

//        if (canInterruptPlanFor(Plan.Posture.OVERRIDE)) {
//            currentPlan = new Plan(Plan.Posture.OVERRIDE).withStep(new CalibrateStep());
//            currentPlan.begin();
//        }

        // Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (VectorUtil.flatten(input.ballPosition).magnitudeSquared() == 0) {
            currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new GoForKickoffStep());
            currentPlan.begin();
        }

        if (canInterruptPlanFor(Plan.Posture.LANDING) && !ArenaModel.isCarOnWall(car) &&
                car.position.z > LandGracefullyStep.NEEDS_LANDING_HEIGHT &&
                !ArenaModel.isBehindGoalLine(car.position)) {
            currentPlan = new Plan(Plan.Posture.LANDING).withStep(new LandGracefullyStep());
            currentPlan.begin();
        }

        BallPath ballPath = ArenaModel.predictBallPath(input, input.time, Duration.ofSeconds(5));
        if (canInterruptPlanFor(Plan.Posture.SAVE)) {
            Optional<SpaceTimeVelocity> scoredOn = GoalUtil.predictGoalEvent(GoalUtil.getOwnGoal(input.team), ballPath);
            if (scoredOn.isPresent()) {
                BotLog.println("Going for save", input.team);
                currentPlan = new Plan(Plan.Posture.SAVE).withStep(new WhatASaveStep());
                currentPlan.begin();
            }
        }

        if (canInterruptPlanFor(Plan.Posture.CLEAR)) {
            boolean ballEntersOurBox = GoalUtil.ballLingersInBox(GoalUtil.getOwnGoal(input.team), ballPath);
            if (ballEntersOurBox) {
                BotLog.println("Going for clear", input.team);
                currentPlan = new Plan(Plan.Posture.CLEAR).withStep(new IdealDirectedHitStep(new KickAwayFromOwnGoal()));
                currentPlan.begin();
            }
        }

        if (canInterruptPlanFor(Plan.Posture.DEFENSIVE)) {
            if (GetOnDefenseStep.needDefense(input, new ThreatAssessor())) {
                BotLog.println("Going on defense", input.team);
                currentPlan = new Plan(Plan.Posture.DEFENSIVE).withStep(new GetOnDefenseStep(new ThreatAssessor()));
                currentPlan.begin();
            } else if (ArenaModel.isBehindGoalLine(car.position)) {
                currentPlan = new Plan(Plan.Posture.DEFENSIVE).withStep(new EscapeTheGoalStep());
                currentPlan.begin();
            }
        }

        if (canInterruptPlanFor(Plan.Posture.OFFENSIVE)) {
            boolean ballEntersEnemyBox = GoalUtil.ballLingersInBox(GoalUtil.getEnemyGoal(input.team), ballPath);
            if (ballEntersEnemyBox && car.position.distance(input.ballPosition) < 80) {
                BotLog.println("Going for shot", input.team);
                currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new IdealDirectedHitStep(new KickAtEnemyGoal()));
                currentPlan.begin();
            }
        }

        if (currentPlan == null || currentPlan.isComplete()) {
            if (DribbleStep.canDribble(input, false) && input.ballVelocity.magnitude() > 15) {
                BotLog.println("Beginning dribble", input.team);
                currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new DribbleStep());
                currentPlan.begin();
            } else if (car.boost < 30 && GetBoostStep.canRun(car)) {
                currentPlan = new Plan().withStep(new GetBoostStep());
                currentPlan.begin();
            } else if (WallTouchStep.hasWallTouchOpportunity(input, ballPath)) {
                currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new MountWallStep()).withStep(new WallTouchStep()).withStep(new DescendFromWallStep());
                currentPlan.begin();
            } else if (DirectedNoseHitStep.canMakeDirectedKick(input)) {
                currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new IdealDirectedHitStep(new KickAtEnemyGoal()));
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
}
