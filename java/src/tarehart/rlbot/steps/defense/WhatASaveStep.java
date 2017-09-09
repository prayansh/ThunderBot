package tarehart.rlbot.steps.defense;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SpaceTimeVelocity;
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
    private Double whichPost;

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

        if (whichPost == null) {

            Vector3 carToThreat = (Vector3) threat.space.subCopy(input.getMyPosition());
            double carApproachVsBallApproach = SteerUtil.getCorrectionAngleRad(VectorUtil.flatten(carToThreat), VectorUtil.flatten(input.ballVelocity));
            // When carApproachVsBallApproach < 0, car is to the right of the ball, angle wise. Right is positive X when we're on the positive Y side of the field.
            whichPost = Math.signum(-carApproachVsBallApproach * threat.space.y);

        }

        double distance = VectorUtil.flatDistance(input.getMyPosition(), threat.getSpace());
        Duration timeTillGoal = Duration.between(input.time, threat.getTime());
        DistancePlot plot = AccelerationModel.simulateAcceleration(input, timeTillGoal, input.getMyBoost(), distance - 15);
//        Optional<Double> travelSeconds = AccelerationModel.getTravelSeconds(input, plot, threat.space);

//        double secondsToSpare = 0;
//        if (travelSeconds.isPresent()) {
//            secondsToSpare = TimeUtil.toSeconds(timeTillGoal) - travelSeconds.get();
//        }

//        if (secondsToSpare <= 0) {
//            BotLog.println("Uh oh...", input.team);
//        }


        Optional<SpaceTime> collisionWithBall = getCollisionWithBall(input, ballPath);
        if (collisionWithBall.isPresent() && Duration.between(collisionWithBall.get().time, threat.getTime()).toMillis() > 500) {
            // TODO: Swerve away from ball to avoid own-goaling it
            BotLog.println("I should be swerving...", input.team);
        }


        /*

        if me-to-threat is about the same angle as the ball velocity, we need to get around the ball and increase
        our angle of attack. We'll use getThereWithFacing.
        - Choose which side we want to go around the ball
        - Choose a waypoint position which is the same distance from the car as the max speed intercept
        - Choose a facing that bends back toward the ball's path a little.
        - When we get near the waypoint, we'll forget about it and go for a ball intercept.

         */

        SpaceTime intercept = SteerUtil.getInterceptOpportunityAssumingMaxAccel(input, ballPath, input.getMyBoost()).orElse(threat.toSpaceTime());

        Vector3 carToIntercept = (Vector3) intercept.space.subCopy(input.getMyPosition());
        double carApproachVsBallApproach = SteerUtil.getCorrectionAngleRad(VectorUtil.flatten(carToIntercept), VectorUtil.flatten(input.ballVelocity));
        if (Math.abs(carApproachVsBallApproach) > Math.PI / 6 &&
                Math.abs(SteerUtil.getCorrectionAngleRad(VectorUtil.flatten(input.getMyRotation().noseVector), VectorUtil.flatten(carToIntercept))) < Math.PI / 12) {

            plan = new Plan(Plan.Posture.SAVE).withStep(new InterceptStep(new Vector3(0, Math.signum(goal.navigationSpline.getLocation().y) * .7, 0)));
            plan.begin();
            return plan.getOutput(input);
        }

        Vector2 facing = new Vector2(-whichPost, 0);
        Vector2 waypoint = VectorUtil.flatten(intercept.space);
        waypoint.sub(facing.scaleCopy(2));

        SteerPlan thereWithFacingPlan = SteerUtil.getThereWithFacing(input, plot, waypoint, (Vector2) facing.normaliseCopy());

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(input, thereWithFacingPlan.waypoint);
        if (sensibleFlip.isPresent()) {
            BotLog.println("Front flip for Save", input.team);
            this.plan = sensibleFlip.get();
            this.plan.begin();
            return this.plan.getOutput(input);
        }

        return Optional.of(thereWithFacingPlan.immediateSteer);
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
        return Plan.concatSituation("Making a save", plan);
    }
}
