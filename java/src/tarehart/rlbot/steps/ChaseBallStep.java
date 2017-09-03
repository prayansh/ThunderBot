package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.strikes.FlipHitStep;
import tarehart.rlbot.steps.strikes.JumpHitStep;
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
        Optional<SpaceTime> interceptOpportunity = SteerUtil.getInterceptOpportunityAssumingMaxAccel(input, ballPath, AirTouchPlanner.getBoostBudget(input));
        Optional<SpaceTime> catchOpportunity = SteerUtil.getCatchOpportunity(input, ballPath, AirTouchPlanner.getBoostBudget(input));

        if (interceptOpportunity.isPresent()) {

            SpaceTime intercept = interceptOpportunity.get();

            if (intercept.space.z > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD) {

                AerialChecklist checklist = AirTouchPlanner.checkAerialReadiness(input, intercept);
                if (checklist.readyToLaunch()) {
                    this.plan = SetPieces.performAerial();
                    this.plan.begin();
                    return this.plan.getOutput(input);
                } else if(checklist.notTooClose && checklist.closeEnough && checklist.hasBoost) {
                    // Hopefully this will line us up and we'll aerial in a future frame.
                    return getThereAsap(input, intercept);
                } else if (checklist.closeEnough && checklist.hasBoost) {
                    BotLog.println("Lining up for aerial as soon as possible...", input.team);
                    this.plan = new Plan().withStep(new AsapAerialStep());
                    this.plan.begin();
                    return this.plan.getOutput(input);
                } else {

                    BotLog.println(String.format("Going for jump hit because aerial looks bad. Boost: %s Close enough: %s Distance: %s Time: %s",
                            checklist.hasBoost, checklist.closeEnough,
                            catchOpportunity.get().space.subCopy(input.getMyPosition()).magnitude(),
                            Duration.between(input.time, catchOpportunity.get().time)), input.team);
                    this.plan = new Plan().withStep(new JumpHitStep());
                    this.plan.begin();
                    return this.plan.getOutput(input);
                }
            } else if (intercept.space.z > AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD) {
                BotLog.println("Lining up jump hit...", input.team);
                this.plan = new Plan().withStep(new JumpHitStep());
                this.plan.begin();
                return this.plan.getOutput(input);
            } else if (intercept.space.z > AirTouchPlanner.NEEDS_FRONT_FLIP_THRESHOLD) {
                BotLog.println("Lining up flip hit...", input.team);
                this.plan = new Plan().withStep(new FlipHitStep());
                this.plan.begin();
                return this.plan.getOutput(input);
            }
            return getThereAsap(input, intercept);

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

        AgentOutput steer = SteerUtil.steerTowardPosition(input, groundPosition.space);
        if (input.getMyBoost() < AirTouchPlanner.BOOST_NEEDED_FOR_AERIAL + 5) {
            steer.withBoost(false);
        }
        return steer;
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
