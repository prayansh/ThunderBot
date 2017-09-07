package tarehart.rlbot.steps.defense;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.strikes.InterceptStep;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.Optional;

public class WhatASaveStep implements Step {
    private Plan plan;
    private boolean hasReachedWaypoint;
    private Vector3 targetGoalPost;

    @Override
    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(5));
        Goal goal = GoalUtil.getOwnGoal(input.team);
        Optional<SpaceTimeVelocity> currentThreat = GoalUtil.predictGoalEvent(goal, ballPath);
        if (!currentThreat.isPresent()) {
            return Optional.empty();
        }

        SpaceTimeVelocity threat = currentThreat.get();

        if (targetGoalPost == null) {
            double whichPost;

            if (isBetweenThePosts(input.getMyPosition())) {
                whichPost = Math.signum(threat.space.x) * -1; // Opposite side from threat
            } else {
                whichPost = Math.signum(input.getMyPosition().x); // Same side as car
            }
            targetGoalPost = new Vector3(whichPost * Goal.EXTENT, goal.navigationSpline.getLocation().y, 0);
        }

        double distance = VectorUtil.flatDistance(input.getMyPosition(), threat.getSpace());
        Duration timeTillGoal = Duration.between(input.time, threat.getTime());
        DistancePlot plot = AccelerationModel.simulateAcceleration(input, timeTillGoal, input.getMyBoost(), distance - 15);
        Optional<Double> travelSeconds = AccelerationModel.getTravelSeconds(input, plot, threat.space);

        double secondsToSpare = 0;
        if (travelSeconds.isPresent()) {
            secondsToSpare = TimeUtil.toSeconds(timeTillGoal) - travelSeconds.get();
        }

        if (secondsToSpare <= 0) {
            BotLog.println("Uh oh...", input.team);
        }


        Optional<SpaceTime> collisionWithBall = getCollisionWithBall(input, ballPath);
        if (collisionWithBall.isPresent() && Duration.between(collisionWithBall.get().time, threat.getTime()).toMillis() > 500) {
            // TODO: Swerve away from ball to avoid own-goaling it
            BotLog.println("I should be swerving...", input.team);
        }

        if (!hasReachedWaypoint) {

            /*

            if me-to-threat is about the same angle as the ball velocity, we need to get around the ball and increase
            our angle of attack. We'll use getThereWithFacing.
            - Choose which side we want to go around the ball
            - Choose a waypoint position which is the same distance from the car as the max speed intercept
            - Choose a facing that bends back toward the ball's path a little.
            - When we get near the waypoint, we'll forget about it and go for a ball intercept.

             */
            Optional<SpaceTime> hypotheticalIntercept = SteerUtil.getInterceptOpportunityAssumingMaxAccel(input, ballPath, input.getMyBoost());

            Vector2 waypoint = new Vector2((targetGoalPost.x + threat.space.x) / 2, targetGoalPost.y - Math.signum(targetGoalPost.y));
            Vector2 facing = (Vector2) VectorUtil.flatten(threat.space).subCopy(waypoint);


            if (VectorUtil.flatten(input.getMyPosition()).distance(waypoint) < 10) {
                hasReachedWaypoint = true;
            }

            return Optional.of(SteerUtil.getThereWithFacing(input, plot, waypoint, (Vector2) facing.normaliseCopy()));
        }

        plan = new Plan(Plan.Posture.SAVE).withStep(new InterceptStep(new Vector3(0, Math.signum(targetGoalPost.y) * .7, 0)));
        plan.begin();
        return plan.getOutput(input);

    }

    private boolean isBetweenThePosts(Vector3 position) {
        return Math.abs(position.x) < Goal.EXTENT;
    }

    /**
     * Assumes that the car proceeds along its nose vector without turning.
     */
    private Optional<SpaceTime> getCollisionWithBall(AgentInput input, BallPath ballPath) {
        Optional<SpaceTime> maxSpeedIntercept = SteerUtil.getInterceptOpportunityAssumingMaxAccel(input, ballPath, input.getMyBoost());
        if (maxSpeedIntercept.isPresent()) {
            SpaceTime intercept = maxSpeedIntercept.get();
            Vector2 carToIntercept = VectorUtil.flatten((Vector3) intercept.space.subCopy(input.getMyPosition()));
            Vector2 noseWithSameLength = (Vector2) VectorUtil.flatten(input.getMyRotation().noseVector).normaliseCopy().scaleCopy(carToIntercept.magnitude());
            double passingDistance = carToIntercept.distance(noseWithSameLength);
            if (passingDistance > 2.5) {
                return Optional.empty();
            }
            Vector3 longNose = new Vector3(noseWithSameLength.x, noseWithSameLength.y, 0);

            return Optional.of(new SpaceTime(input.getMyPosition().addCopy(longNose), intercept.time));
        }
        return Optional.empty();
    }

    @Override
    public boolean isBlindlyComplete() {
        return false;
    }

    @Override
    public void begin() {

    }

    @Override
    public String getSituation() {
        return "Making a save";
    }
}
