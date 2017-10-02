package tarehart.rlbot;

import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.planning.TacticsAdvisor;
import tarehart.rlbot.steps.*;
import tarehart.rlbot.steps.defense.GetOnDefenseStep;
import tarehart.rlbot.steps.defense.ThreatAssessor;
import tarehart.rlbot.steps.defense.WhatASaveStep;
import tarehart.rlbot.steps.landing.LandGracefullyStep;
import tarehart.rlbot.steps.strikes.*;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.Optional;

public class ReliefBot extends Bot {

    private TacticsAdvisor tacticsAdvisor;

    public ReliefBot(Team team) {
        super(team);
        tacticsAdvisor = new TacticsAdvisor();
    }

    @Override
    protected AgentOutput getOutput(AgentInput input) {

        final CarData car = input.getMyCarData();

//        if (canInterruptPlanFor(Plan.Posture.OVERRIDE)) {
//            currentPlan = new Plan(Plan.Posture.OVERRIDE).withStep(new InterceptStep(new Vector3()));
//            currentPlan.begin();
//        }

        // Kickoffs can happen unpredictably because the bot doesn't know about goals at the moment.
        if (VectorUtil.flatten(input.ballPosition).magnitudeSquared() == 0) {
            currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new GoForKickoffStep());
            currentPlan.begin();
        }

        if (canInterruptPlanFor(Plan.Posture.LANDING) && !ArenaModel.isCarOnWall(car) &&
                !ArenaModel.isNearFloorEdge(car) &&
                car.position.z > 5 &&
                !ArenaModel.isBehindGoalLine(car.position)) {
            currentPlan = new Plan(Plan.Posture.LANDING).withStep(new LandGracefullyStep(LandGracefullyStep.FACE_BALL));
            currentPlan.begin();
        }

        BallPath ballPath = ArenaModel.predictBallPath(input, input.time, Duration.ofSeconds(7));
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
                currentPlan = new Plan(Plan.Posture.CLEAR).withStep(new IdealDirectedHitStep(new KickAwayFromOwnGoal(), input));
                currentPlan.begin();
            }
        }

        if (canInterruptPlanFor(Plan.Posture.OFFENSIVE)) {
            boolean ballEntersEnemyBox = GoalUtil.ballLingersInBox(GoalUtil.getEnemyGoal(input.team), ballPath);
            if (ballEntersEnemyBox && car.position.distance(input.ballPosition) < 80) {
                BotLog.println("Going for shot", input.team);
                currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new IdealDirectedHitStep(new KickAtEnemyGoal(), input));
                currentPlan.begin();
            }
        }

        if (currentPlan == null || currentPlan.isComplete()) {
            currentPlan = tacticsAdvisor.makePlan(input);
            currentPlan.begin();
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
