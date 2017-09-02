package tarehart.rlbot.steps;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
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

        if (DribbleStep.canDribble(input)) {
            plan = new Plan().withStep(new DribbleStep());
            plan.begin();
            return plan.getOutput(input);
        }

        if (input.getMyBoost() < 10 && GetBoostStep.seesOpportunisticBoost(input)) {
            plan = new Plan().withStep(new GetBoostStep());
            plan.begin();
            return plan.getOutput(input);
        }

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(3));
        List<SpaceTime> interceptOpportunities = SteerUtil.getInterceptOpportunitiesAssumingMaxAccel(input, ballPath, input.getMyBoost());
        Optional<SpaceTime> catchOpportunity = SteerUtil.getCatchOpportunity(input, ballPath);

        // Weed out any intercepts after a catch opportunity. Should just catch it.
        if (catchOpportunity.isPresent()) {
            for (int i = interceptOpportunities.size() - 1; i >= 0; i--) {
                if (interceptOpportunities.get(i).time.isAfter(catchOpportunity.get().time)) {
                    interceptOpportunities.remove(i);
                }
            }
        }

        Optional<SpaceTime> preferredIntercept = interceptOpportunities.stream().findFirst();
        if (preferredIntercept.isPresent()) {

            if (preferredIntercept.get().space.z > AerialPlanner.NEEDS_AERIAL_THRESHOLD) {

                AerialPlanner.AerialChecklist checklist = AerialPlanner.checkAerialReadiness(input, preferredIntercept.get());
                if (checklist.readyForAerial()) {
                    this.plan = SetPieces.performAerial();
                    this.plan.begin();
                    return this.plan.getOutput(input);
                } else if(checklist.notTooClose && checklist.closeEnough && checklist.hasBoost) {
                    // Hopefully this will line us up and we'll aerial in a future frame.
                    return getThereAsap(input, preferredIntercept.get());
                } else if (checklist.closeEnough && checklist.hasBoost) {
                    BotLog.println("Lining up for aerial as soon as possible...", input.team);
                    this.plan = new Plan().withStep(new AsapAerialStep());
                    this.plan.begin();
                    return this.plan.getOutput(input);
                } else if (catchOpportunity.isPresent()) {
                    BotLog.println(String.format("Going for catch because aerial looks bad. Distance: %s Time: %s",
                            catchOpportunity.get().space.subCopy(input.getMyPosition()).magnitude(),
                            Duration.between(input.time, catchOpportunity.get().time)), input.team);
                    this.plan = new Plan().withStep(new CatchBallStep(catchOpportunity.get())).withStep(new DribbleStep());
                    this.plan.begin();
                    return this.plan.getOutput(input);
                }
            }
            return getThereAsap(input, preferredIntercept.get());

        } else if (catchOpportunity.isPresent()) {
            BotLog.println(String.format("Going for catch because there are no full speed intercepts. Distance: %s Time: %s",
                    catchOpportunity.get().space.subCopy(input.getMyPosition()).magnitude(),
                    Duration.between(input.time, catchOpportunity.get().time)), input.team);
            plan = new Plan().withStep(new CatchBallStep(catchOpportunity.get())).withStep(new DribbleStep());
            plan.begin();
            return plan.getOutput(input);
        } else {
            return getThereAsap(input, new SpaceTime(input.ballPosition, input.time.plusSeconds(3)));
        }
    }

    private AgentOutput getThereAsap(AgentInput input, SpaceTime groundPosition) {

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(input, groundPosition);
        if (sensibleFlip.isPresent()) {
            BotLog.println("Front flip to chase ball", input.team);
            this.plan = sensibleFlip.get();
            this.plan.begin();
            return this.plan.getOutput(input);
        }

        return SteerUtil.steerTowardPosition(input, groundPosition.space);
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
        return "Chasing ball " + (plan != null && !plan.isComplete() ? "(" + plan.getSituation() + ")" : "");
    }
}
