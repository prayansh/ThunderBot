package tarehart.rlbot.planning;

import mikera.vectorz.Vector;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.TimeUtil;

import java.time.Duration;

public class AirTouchPlanner {

    private static final double AERIAL_RISE_RATE = 7;
    private static final double JUMP_RISE_RATE = 12;
    public static final double BOOST_NEEDED_FOR_AERIAL = 20;
    public static final double NEEDS_AERIAL_THRESHOLD = 6;
    public static final double MAX_JUMP_HIT = NEEDS_AERIAL_THRESHOLD;
    public static final double NEEDS_JUMP_HIT_THRESHOLD = 4;
    public static final double NEEDS_FRONT_FLIP_THRESHOLD = 2;
    private static final double CAR_BASE_HEIGHT = 0.33;
    private static final double MAX_FLIP_HIT = NEEDS_JUMP_HIT_THRESHOLD;


    public static AerialChecklist checkAerialReadiness( AgentInput input, SpaceTime carPositionAtContact) {

        AerialChecklist checklist = new AerialChecklist();
        checkLaunchReadiness(checklist, input, carPositionAtContact);

        checklist.notSkidding = input.getMyVelocity().normaliseCopy().dotProduct(input.getMyRotation().noseVector) > .99;
        checklist.hasBoost = input.getMyBoost() >= BOOST_NEEDED_FOR_AERIAL;

        return checklist;
    }

    public static LaunchChecklist checkJumpHitReadiness(AgentInput input, SpaceTime carPositionAtContact) {

        LaunchChecklist checklist = new LaunchChecklist();
        checkLaunchReadiness(checklist, input, carPositionAtContact);
        return checklist;
    }

    public static LaunchChecklist checkFlipHitReadiness(AgentInput input, SpaceTime intercept) {
        LaunchChecklist checklist = new LaunchChecklist();
        checkLaunchReadiness(checklist, input, intercept);
        checklist.notTooClose = true;
        checklist.timeForIgnition = TimeUtil.secondsBetween(input.time, intercept.time) < .6;
        return checklist;
    }

    private static void checkLaunchReadiness(LaunchChecklist checklist, AgentInput input, SpaceTime carPositionAtContact) {

        double correctionAngleRad = SteerUtil.getCorrectionAngleRad(input, carPositionAtContact.space);
        double secondsTillIntercept = TimeUtil.secondsBetween(input.time, carPositionAtContact.time);
        double tMinus = getAerialLaunchCountdown(carPositionAtContact.space.z, secondsTillIntercept);

        checklist.linedUp = Math.abs(correctionAngleRad) < Math.PI / 60;
        checklist.closeEnough = secondsTillIntercept < 4;
        checklist.notTooClose = isVerticallyAccessible(input, carPositionAtContact);
        checklist.timeForIgnition = tMinus < 0.1;
        checklist.upright = input.getMyRotation().roofVector.dotProduct(new Vector3(0, 0, 1)) > .99;
        checklist.onTheGround = input.getMyPosition().z < CAR_BASE_HEIGHT + 0.03; // Add a little wiggle room
    }

    public static boolean isVerticallyAccessible(AgentInput input, SpaceTime intercept) {
        double secondsTillIntercept = TimeUtil.secondsBetween(input.time, intercept.time);

        if (intercept.space.z < NEEDS_AERIAL_THRESHOLD) {
            double tMinus = getJumpLaunchCountdown(intercept.space.z, secondsTillIntercept);
            return tMinus >= -0.1;
        }

        if (input.getMyBoost() > BOOST_NEEDED_FOR_AERIAL) {
            double tMinus = getAerialLaunchCountdown(intercept.space.z, secondsTillIntercept);
            return tMinus >= -0.1;
        }
        return false;
    }

    public static boolean isJumpHitAccessible(AgentInput input, SpaceTime intercept) {
        if (intercept.space.z > MAX_JUMP_HIT) {
            return false;
        }

        double secondsTillIntercept = TimeUtil.secondsBetween(input.time, intercept.time);
        double tMinus = getJumpLaunchCountdown(intercept.space.z, secondsTillIntercept);
        return tMinus >= -0.1;
    }

    public static boolean isFlipHitAccessible(AgentInput input, SpaceTime intercept) {
        return intercept.space.z <= MAX_FLIP_HIT;
    }

    private static double getAerialLaunchCountdown(double height, double secondsTillIntercept) {
        double expectedAerialSeconds = (height - CAR_BASE_HEIGHT) / AERIAL_RISE_RATE;
        return secondsTillIntercept - expectedAerialSeconds;
    }

    private static double getJumpLaunchCountdown(double height, double secondsTillIntercept) {
        double expectedJumpSeconds = (height - CAR_BASE_HEIGHT) / JUMP_RISE_RATE;
        return secondsTillIntercept - expectedJumpSeconds;
    }

    public static double getBoostBudget(AgentInput input) {
        return input.getMyBoost() - BOOST_NEEDED_FOR_AERIAL - 5;
    }
}
