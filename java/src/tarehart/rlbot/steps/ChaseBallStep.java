package tarehart.rlbot.steps;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.SteerUtil;

import java.time.Duration;

public class ChaseBallStep implements Step {

    private static final double AERIAL_RISE_RATE = 10;

    private boolean isComplete = false;
    private Plan plan;


    public AgentOutput getOutput(AgentInput input) {

        double flatDistance = flatten(input.getMyPosition()).distance(flatten(input.ballPosition));

        if (flatDistance < 1) {
            isComplete = true;
        }

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        if (input.getMyBoost() < 10 && GetBoostStep.seesOpportunisticBoost(input)) {
            plan = new Plan().withStep(new GetBoostStep());
            plan.begin();
            return plan.getOutput(input);
        }

        SpaceTime intercept = SteerUtil.predictBallInterceptFlat(input);
        Duration timeTillIntercept = Duration.between(input.time, intercept.time);
        double correctionAngleRad = SteerUtil.getCorrectionAngleRad(input, intercept.space);
        SpaceTime intercept3d = SteerUtil.predictBallIntercept3d(input, intercept);

        if (intercept3d.space.z > 5 && Math.abs(correctionAngleRad) < Math.PI / 24) {


            boolean hasAerialBoost = input.getMyBoost() > 30;
            Duration expectedAerialTime = Duration.ofMillis((long) (1000 * intercept3d.space.z / AERIAL_RISE_RATE));
            Duration tMinus = timeTillIntercept.minus(expectedAerialTime);
            boolean tooLateToAerial = tMinus.toMillis() < -100;

            if (hasAerialBoost && timeTillIntercept.toMillis() < 4000 &&
                    tMinus.toMillis() < 10 && !tooLateToAerial) {

                    // Time to get up!
                    System.out.println("Performing Aerial!");
                    plan = SetPieces.performAerial();
                    plan.begin();
                    return plan.getOutput(input);
            } else {
                if ((!hasAerialBoost || tooLateToAerial) && flatDistance < 25) {
                    // Slow down and catch it
                    return new AgentOutput();
                }
            }
        }

        if (flatDistance > 60 && input.getMyBoost() < 1 &&
                Math.abs(correctionAngleRad) < Math.PI / 12 && input.getMyVelocity().magnitude() > SteerUtil.MAX_SPEED / 4) {
            System.out.println("Front flipping after ball!");
            plan = SetPieces.frontFlip();
            plan.begin();
        }

        return SteerUtil.steerTowardPosition(input, intercept.space);
    }

    private Vector2 flatten(Vector3 vector3) {
        return new Vector2(vector3.x, vector3.y);
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void begin() {
    }
}
