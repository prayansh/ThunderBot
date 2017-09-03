package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class AsapAerialStep implements Step {

    private Plan plan;
    private boolean isComplete;

    public AsapAerialStep() {

    }

    public AgentOutput getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        double currentSpeed = input.getMyVelocity().magnitude();
        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(3));

        Optional<SpaceTime> interceptOpportunity = SteerUtil.getInterceptOpportunityAssumingMaxAccel(input, ballPath, AirTouchPlanner.getBoostBudget(input));
        if (interceptOpportunity.isPresent()) {

            if (interceptOpportunity.get().space.z < AirTouchPlanner.NEEDS_AERIAL_THRESHOLD) {
                BotLog.println("AsapAerial failing because ball will be too low.", input.team);
                isComplete = true;
                return new AgentOutput();
            }

            AerialChecklist checklist = AirTouchPlanner.checkAerialReadiness(input, interceptOpportunity.get());
            if (checklist.readyToLaunch()) {
                plan = SetPieces.performAerial();
                plan.begin();
                return plan.getOutput(input);
            } else if (!checklist.notTooClose) {
                double nextSpeed = currentSpeed * .9;

                Optional<SpaceTime> slowerIntercept = SteerUtil.getInterceptOpportunity(input, ballPath, nextSpeed);
                if (slowerIntercept.isPresent()) {
                    return SteerUtil.steerTowardPosition(input, slowerIntercept.get().space)
                            .withAcceleration(0)
                            .withDeceleration(.3)
                            .withBoost(false);
                }
            } else if (!checklist.closeEnough) {
                BotLog.println("AsapAerial failing because we are not close enough", input.team);
                isComplete = true;
            }
            else {
                return getThereAsap(input, interceptOpportunity.get());
            }
        } else {
            BotLog.println("AsapAerial failing because there are no max speed intercepts", input.team);
            isComplete = true;
        }

        return new AgentOutput();
    }

    private AgentOutput getThereAsap(AgentInput input, SpaceTime groundPosition) {

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(input, groundPosition);
        if (sensibleFlip.isPresent()) {
            BotLog.println("Front flip for AsapAerial", input.team);
            this.plan = sensibleFlip.get();
            this.plan.begin();
            return this.plan.getOutput(input);
        }

        AgentOutput output = SteerUtil.steerTowardPosition(input, groundPosition.space);
        if (input.getMyBoost() <= AirTouchPlanner.BOOST_NEEDED_FOR_AERIAL + 5) {
            output.withBoost(false);
        }
        return output;
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
        return "Preparing for aerial";
    }
}
