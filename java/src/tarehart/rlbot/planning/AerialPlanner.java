package tarehart.rlbot.planning;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.SpaceTime;

import java.time.Duration;

public class AerialPlanner {

    private static final double AERIAL_RISE_RATE = 7;
    public static final double NEEDS_AERIAL_THRESHOLD = 4;
    public static final double BOOST_NEEDED = 20;


    public static class AerialChecklist {
        public boolean linedUp;
        public boolean closeEnough;
        public boolean notTooClose;
        public boolean timeForIgnition;
        public boolean notSkidding;
        public boolean upright;
        public boolean onTheGround;
        public boolean hasBoost;

        public boolean readyForAerial() {
            return linedUp && closeEnough && notTooClose && timeForIgnition && notSkidding && upright && onTheGround && hasBoost;
        }
    }

    public static AerialChecklist checkAerialReadiness(AgentInput input, SpaceTime carPositionAtContact) {

        double correctionAngleRad = SteerUtil.getCorrectionAngleRad(input, carPositionAtContact.space);
        Duration timeTillIntercept = Duration.between(input.time, carPositionAtContact.time);
        Duration tMinus = getAerialLaunchCountdown(carPositionAtContact, timeTillIntercept);

        AerialChecklist checklist = new AerialChecklist();

        checklist.linedUp = Math.abs(correctionAngleRad) < Math.PI / 30;
        checklist.closeEnough = timeTillIntercept.toMillis() < 4000;
        checklist.notTooClose = timeTillIntercept.toMillis() > 500;
        checklist.timeForIgnition = tMinus.toMillis() < 80;
        checklist.notSkidding = input.getMyVelocity().normaliseCopy().dotProduct(input.getMyRotation().noseVector) > .99;
        checklist.upright = input.getMyRotation().roofVector.dotProduct(new Vector3(0, 0, 1)) > .99;
        checklist.onTheGround = input.getMyPosition().z < .36;
        checklist.hasBoost = input.getMyBoost() >= BOOST_NEEDED;

        return checklist;
    }

    public static boolean isVerticallyAccessible(AgentInput input, SpaceTime intercept) {
        Duration timeTillIntercept = Duration.between(input.time, intercept.time);

        if (intercept.space.z < NEEDS_AERIAL_THRESHOLD) {
            // We can probably just get it by jumping
            return true;
        }

        if (input.getMyBoost() > BOOST_NEEDED) {
            Duration tMinus = getAerialLaunchCountdown(intercept, timeTillIntercept);
            return tMinus.toMillis() >= -100;
        }
        return false;
    }

    public static Duration getAerialLaunchCountdown(SpaceTime intercept, Duration timeTillIntercept) {
        Duration expectedAerialTime = Duration.ofMillis((long) (1000 * intercept.space.z / AERIAL_RISE_RATE));
        return timeTillIntercept.minus(expectedAerialTime);
    }

}
