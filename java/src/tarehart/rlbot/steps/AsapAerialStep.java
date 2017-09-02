package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
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

        List<SpaceTime> currentIntercepts = SteerUtil.getInterceptOpportunitiesAssumingMaxAccel(input, ballPath, input.getMyBoost() - AerialPlanner.BOOST_NEEDED - 5);
        if (currentIntercepts.size() > 0) {

            if (currentIntercepts.get(0).space.z < AerialPlanner.NEEDS_AERIAL_THRESHOLD) {
                BotLog.println("AsapAerial failing because ball will be too low.", input.team);
                isComplete = true;
                return new AgentOutput();
            }

            AerialPlanner.AerialChecklist checklist = AerialPlanner.checkAerialReadiness(input, currentIntercepts.get(0));
            if (checklist.readyForAerial()) {
                plan = SetPieces.performAerial();
                plan.begin();
                return plan.getOutput(input);
            } else if (!checklist.notTooClose) {
                double nextSpeed = currentSpeed * .9;

                List<SpaceTime> slowerIntercepts = SteerUtil.getInterceptOpportunities(input, ballPath, nextSpeed);
                if (slowerIntercepts.size() > 0) {
                    return SteerUtil.steerTowardPosition(input, slowerIntercepts.get(0).space)
                            .withAcceleration(0)
                            .withDeceleration(.3)
                            .withBoost(false);
                }
            } else if (!checklist.closeEnough) {
                BotLog.println("AsapAerial failing because we are not close enough", input.team);
                isComplete = true;
            }
            else {
                return getThereAsap(input, currentIntercepts.get(0));
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
        if (input.getMyBoost() <= AerialPlanner.BOOST_NEEDED + 5) {
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
