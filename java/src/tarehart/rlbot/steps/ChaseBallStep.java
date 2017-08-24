package tarehart.rlbot.steps;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.PlanOutput;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.SteerUtil;

import java.time.Duration;
import java.time.LocalDateTime;

public class ChaseBallStep implements Step {

    private static final double AERIAL_RISE_RATE = 1;

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
        long millisTillIntercept = timeTillIntercept.toMillis();
        if (Math.abs(correctionAngleRad) < Math.PI / 24 && millisTillIntercept < 5000 && input.getMyBoost() > 50) {

            SpaceTime intercept3d = SteerUtil.predictBallIntercept(input, intercept);

            if (intercept3d.space.z > 3 && AERIAL_RISE_RATE * millisTillIntercept / 1000.0 < intercept3d.space.z) {
                // Time to get up!
                System.out.println("Performing Aerial!");
                plan = SetPieces.performAerial();
                plan.begin();
                return plan.getOutput(input);
            }
        }



        if (flatDistance > 30 && input.getMyBoost() < 1 &&
                Math.abs(correctionAngleRad) < Math.PI / 12) {
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
