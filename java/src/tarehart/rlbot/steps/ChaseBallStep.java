package tarehart.rlbot.steps;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class ChaseBallStep implements Step {


    private boolean isComplete = false;
    private Plan plan;

    public AgentOutput getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        double flatDistance = flatten(input.getMyPosition()).distance(flatten(input.ballPosition));

        if (flatDistance < 1) {
            isComplete = true;
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

                boolean linedUp = Math.abs(correctionAngleRad) < Math.PI / 30;
                boolean closeEnough = timeTillIntercept.toMillis() < 4000;
                boolean notTooClose = timeTillIntercept.toMillis() > 500;
                boolean timeForIgnition = tMinus.toMillis() < 80;
                boolean notSkidding = input.getMyVelocity().normaliseCopy().dotProduct(input.getMyRotation().noseVector) > .99;
                boolean upright = input.getMyRotation().roofVector.dotProduct(new Vector3(0, 0, 1)) > .99;
                boolean onTheGround = input.getMyPosition().z < .36;

                if (linedUp && closeEnough && notTooClose && timeForIgnition && notSkidding && upright && onTheGround) {

                    // Time to get up!
                    BotLog.println("Performing Aerial!", input.team);
                    plan = SetPieces.performAerial();
                    plan.begin();
                    return plan.getOutput(input);
                } else if(notTooClose && notSkidding) {
                    BotLog.println(String.format("Aerial soon... linedUp: %s closeEnough: %s timeForIgnition: %s upright: %s onGround: %s",
                            linedUp, closeEnough, timeForIgnition, upright, onTheGround), input.team);

                    // Hopefully this will line us up and we'll aerial in a future frame.
                    return getThereAsap(input, preferredIntercept.get());
                } else if (catchOpportunity.isPresent()) {
                    BotLog.println(String.format("Going for catch because aerial looks bad. Distance: %s Time: %s",
                            catchOpportunity.get().space.subCopy(input.getMyPosition()).magnitude(),
                            Duration.between(input.time, catchOpportunity.get().time)), input.team);
                    plan = new Plan().withStep(new CatchBallStep(catchOpportunity.get()));
                    plan.begin();
                    return plan.getOutput(input);
                }
            }
            return getThereAsap(input, preferredIntercept.get());

        } else if (catchOpportunity.isPresent()) {
            BotLog.println(String.format("Going for catch because there are no full speed intercepts. Distance: %s Time: %s",
                    catchOpportunity.get().space.subCopy(input.getMyPosition()).magnitude(),
                    Duration.between(input.time, catchOpportunity.get().time)), input.team);
            plan = new Plan().withStep(new CatchBallStep(catchOpportunity.get()));
            plan.begin();
            return plan.getOutput(input);
        } else {
            // Give up on the chase.
            isComplete = true;
            return new AgentOutput();
        }
    }

    private AgentOutput getThereAsap(AgentInput input, SpaceTime groundPosition) {

        Duration timeTillIntercept = Duration.between(input.time, groundPosition.time);
        if (timeTillIntercept.toMillis() > 2000) {

            double correctionAngleRad = SteerUtil.getCorrectionAngleRad(input, groundPosition.space);

            if (input.getMyBoost() < 1 && Math.abs(correctionAngleRad) < Math.PI / 12
                    && input.getMyVelocity().magnitude() > SteerUtil.MAX_SPEED / 4) {

                BotLog.println("Front flipping after ball!", input.team);
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

    @Override
    public String getSituation() {
        return "Chasing ball " + (plan != null ? "(" + plan.getSituation() + ")" : "");
    }
}
