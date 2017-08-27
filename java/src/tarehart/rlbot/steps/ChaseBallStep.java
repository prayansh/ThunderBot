package tarehart.rlbot.steps;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.SteerUtil;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class ChaseBallStep implements Step {



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

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(3));
        List<SpaceTime> interceptOpportunities = SteerUtil.getInterceptOpportunities(input, ballPath);
        Optional<SpaceTime> catchOpportunity = SteerUtil.getCatchOpportunity(input, ballPath);

        // Weed out any intercepts after a catch opportunity. Should just catch it.
        if (catchOpportunity.isPresent()) {
            for (int i = interceptOpportunities.size() - 1; i >= 0; i--) {
                if (interceptOpportunities.get(i).time.isAfter(catchOpportunity.get().time)) {
                    interceptOpportunities.remove(i);
                }
            }
        }

        Optional<SpaceTime> preferredIntercept = SteerUtil.getPreferredIntercept(input, interceptOpportunities);
        if (preferredIntercept.isPresent()) {

            if (preferredIntercept.get().space.z > SteerUtil.NEEDS_AERIAL_THRESHOLD) {
                double correctionAngleRad = SteerUtil.getCorrectionAngleRad(input, preferredIntercept.get().space);
                Duration timeTillIntercept = Duration.between(input.time, preferredIntercept.get().time);
                Duration tMinus = SteerUtil.getAerialLaunchCountdown(preferredIntercept.get(), timeTillIntercept);
                if (correctionAngleRad < Math.PI / 24 && timeTillIntercept.toMillis() < 4000
                        && timeTillIntercept.toMillis() > 1000 && tMinus.toMillis() < 10
                        && input.getMyVelocity().dotProduct(input.getMyRotation().noseVector) > .95) {

                    // Time to get up!
                    System.out.println("Performing Aerial!");
                    plan = SetPieces.performAerial();
                    plan.begin();
                    return plan.getOutput(input);
                }
            }
            return getThereAsap(input, preferredIntercept.get());

        } else if (catchOpportunity.isPresent()) {
            System.out.println(String.format("Going for catch. Distance: %s Time: %s",
                    catchOpportunity.get().space.subCopy(input.getMyPosition()).magnitude(),
                    Duration.between(input.time, catchOpportunity.get().time)));
            return getThereOnTime(input, catchOpportunity.get());
        } else {
            return getThereAsap(input, ballPath.getEndpoint());
        }
    }

    private AgentOutput getThereOnTime(AgentInput input, SpaceTime groundPositionAndTime) {
        double flatDistance = flatten(input.getMyPosition()).distance(flatten(input.ballPosition));
        double correctionAngleRad = SteerUtil.getCorrectionAngleRad(input, groundPositionAndTime.space);

        if (Math.abs(correctionAngleRad) > Math.PI / 12) {
            return getThereAsap(input, groundPositionAndTime);
        }

        double speed = input.getMyVelocity().magnitude();
        double secondsTillAppointment = Duration.between(input.time, groundPositionAndTime.time).toMillis() / 1000.0;
        if (speed * secondsTillAppointment > flatDistance && secondsTillAppointment < 2) {
            // We're going too fast!
            return new AgentOutput().withDeceleration(1); // Hit the brakes!
        } else {
            return getThereAsap(input, groundPositionAndTime);
        }
    }

    private AgentOutput getThereAsap(AgentInput input, SpaceTime groundPosition) {

        Duration timeTillIntercept = Duration.between(input.time, groundPosition.time);
        if (timeTillIntercept.toMillis() > 2000) {

            double correctionAngleRad = SteerUtil.getCorrectionAngleRad(input, groundPosition.space);

            if (input.getMyBoost() < 1 && Math.abs(correctionAngleRad) < Math.PI / 12
                    && input.getMyVelocity().magnitude() > SteerUtil.MAX_SPEED / 4) {

                System.out.println("Front flipping after ball!");
                plan = SetPieces.frontFlip();
                plan.begin();
                return plan.getOutput(input);
            }
        }

        return SteerUtil.steerTowardPosition(input, groundPosition.space);
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
