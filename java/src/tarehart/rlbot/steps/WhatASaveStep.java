package tarehart.rlbot.steps;

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
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.Optional;

public class WhatASaveStep implements Step {
    private SpaceTimeVelocity initialThreat;
    private boolean isComplete;
    private Plan plan;
    private boolean hasReachedWaypoint;
    private Vector3 targetGoalPost;

    public WhatASaveStep(SpaceTimeVelocity initialThreat) {
        this.initialThreat = initialThreat;
    }

    @Override
    public AgentOutput getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }



        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(5));
        Goal goal = GoalUtil.getOwnGoal(input.team);
        Optional<SpaceTimeVelocity> currentThreat = GoalUtil.predictGoalEvent(goal, ballPath);
        if (!currentThreat.isPresent()) {
            isComplete = true;
            return new AgentOutput();
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

            Vector2 waypoint = new Vector2((targetGoalPost.x + threat.space.x) / 2, targetGoalPost.y - Math.signum(targetGoalPost.y));
            Vector2 facing = (Vector2) VectorUtil.flatten(threat.space).subCopy(waypoint);


            if (VectorUtil.flatten(input.getMyPosition()).distance(waypoint) < 10) {
                hasReachedWaypoint = true;
            }

            return SteerUtil.getThereWithFacing(input, plot, waypoint, (Vector2) facing.normaliseCopy());
        }

        Optional<Plan> interceptPlan = InterceptPlanner.planFullSpeedIntercept(input, ballPath);
        if (interceptPlan.isPresent()) {
            plan = interceptPlan.get();
            plan.begin();
            return plan.getOutput(input);
        }

        BotLog.println("No intercept plan, am I hosed?", input.team);
        return SteerUtil.getThereOnTime(input, threat.toSpaceTime());
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

    private AgentOutput getThereAsap(AgentInput input, Vector3 target) {

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(input, target);
        if (sensibleFlip.isPresent()) {
            BotLog.println("Front flip toward save", input.team);
            this.plan = sensibleFlip.get();
            this.plan.begin();
            return this.plan.getOutput(input);
        }

        return SteerUtil.steerTowardPosition(input, target);
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void begin() {

    }

    @Override
    public String getSituation() {
        return "Making a save";
    }
}
