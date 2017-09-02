package tarehart.rlbot.planning;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;

public class AccelerationModel {

    public static final int SUPERSONIC_SPEED = 46;
    public static final int MEDIUM_SPEED = 28;
    public static final double FRONT_FLIP_SECONDS = 1.5;

    private static final Double TIME_STEP = 0.1;
    private static final double FRONT_FLIP_SPEED_BOOST = 10;
    private static final double SUB_MEDIUM_ACCELERATION = 15; // zero to medium in about 2 seconds.
    private static final double INCREMENTAL_BOOST_ACCELERATION = 8;
    private static final double BOOST_CONSUMED_PER_SECOND = 25;

    public static double simulateTravelTime(AgentInput input, Vector3 target, double boostBudget) {

        double distance = VectorUtil.flatten(input.getMyPosition()).distance(VectorUtil.flatten(target));
        double boostRemaining = boostBudget;

        double distanceSoFar = 0;
        double secondsSoFar = 0;
        double currentSpeed = input.getMyVelocity().magnitude();
        double steerPenaltySeconds = Math.abs(SteerUtil.getCorrectionAngleRad(input, target)) / (currentSpeed + 1);

        while (distanceSoFar < distance) {
            double hypotheticalFrontFlipDistance = ((currentSpeed * 2 + FRONT_FLIP_SPEED_BOOST) / 2) * FRONT_FLIP_SECONDS;
            if (boostRemaining <= 0 && distanceSoFar + hypotheticalFrontFlipDistance < distance) {
                secondsSoFar += FRONT_FLIP_SECONDS;
                distanceSoFar += hypotheticalFrontFlipDistance;
                currentSpeed += FRONT_FLIP_SPEED_BOOST;
                continue;
            }

            double acceleration = getAcceleration(currentSpeed, boostRemaining > 0);
            currentSpeed += acceleration * TIME_STEP;
            distanceSoFar += currentSpeed * TIME_STEP;
            secondsSoFar += TIME_STEP;
            boostRemaining -= BOOST_CONSUMED_PER_SECOND * TIME_STEP;
        }

        double overshoot = distanceSoFar - distance;
        secondsSoFar -= overshoot / currentSpeed;

        return secondsSoFar + steerPenaltySeconds;
    }

    private static double getAcceleration(double currentSpeed, boolean hasBoost) {

        if (currentSpeed >= SUPERSONIC_SPEED || !hasBoost && currentSpeed >= MEDIUM_SPEED) {
            return 0;
        }

        double accel = 0;
        if (currentSpeed < MEDIUM_SPEED) {
            accel += SUB_MEDIUM_ACCELERATION;
        }
        if (hasBoost) {
            accel += INCREMENTAL_BOOST_ACCELERATION;
        }

        return accel;
    }

}
