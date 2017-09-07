package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class InterceptStep implements Step {

    private Plan plan;
    private boolean isComplete;
    private Vector3 interceptModifier;

    public InterceptStep(Vector3 interceptModifier) {
        this.interceptModifier = interceptModifier;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(4));
        DistancePlot fullAcceleration = AccelerationModel.simulateAcceleration(input, Duration.ofSeconds(4), input.getMyBoost(), 0);

        List<Intercept> interceptOptions = new ArrayList<>();
        getAerialIntercept(input, ballPath).ifPresent(interceptOptions::add);
        getJumpHitIntercept(input, ballPath, fullAcceleration).ifPresent(interceptOptions::add);
        getFlipHitIntercept(input, ballPath, fullAcceleration).ifPresent(interceptOptions::add);

        Optional<Intercept> chosenIntercept = interceptOptions.stream().sorted(Comparator.comparing(Intercept::getTime)).findFirst();
        Optional<Plan> launchPlan = chosenIntercept.flatMap(cept -> InterceptPlanner.planImmediateLaunch(input, cept.toSpaceTime()));
        if (launchPlan.isPresent()) {
            plan = launchPlan.get();
            plan.begin();
            return plan.getOutput(input);
        }

        return chosenIntercept.map(intercept -> getThereOnTime(input, intercept.toSpaceTime(), intercept.getAirBoost()));
    }

    public Optional<Intercept> getAerialIntercept(AgentInput input, BallPath ballPath) {
        if (input.getMyBoost() >= AirTouchPlanner.BOOST_NEEDED_FOR_AERIAL) {
            DistancePlot budgetAcceleration = AccelerationModel.simulateAcceleration(input, Duration.ofSeconds(4), AirTouchPlanner.getBoostBudget(input), 0);
            Optional<SpaceTime> budgetInterceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(input, ballPath, budgetAcceleration, interceptModifier, AirTouchPlanner::isVerticallyAccessible);
            if (budgetInterceptOpportunity.isPresent()) {
                SpaceTime spaceTime = budgetInterceptOpportunity.get();
                if (budgetInterceptOpportunity.get().space.z > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD) {
                    return Optional.of(new Intercept(spaceTime.space, spaceTime.time, AirTouchPlanner.BOOST_NEEDED_FOR_AERIAL));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Intercept> getJumpHitIntercept(AgentInput input, BallPath ballPath, DistancePlot fullAcceleration) {
        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(input, ballPath, fullAcceleration, interceptModifier, AirTouchPlanner::isJumpHitAccessible);
        if (interceptOpportunity.isPresent()) {
            if (interceptOpportunity.get().space.z > AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD) {
                return Optional.of(new Intercept(interceptOpportunity.get()));
            }
        }
        return Optional.empty();
    }

    public Optional<Intercept> getFlipHitIntercept(AgentInput input, BallPath ballPath, DistancePlot fullAcceleration) {
        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(input, ballPath, fullAcceleration, interceptModifier, AirTouchPlanner::isFlipHitAccessible);
        if (interceptOpportunity.isPresent()) {
            if (interceptOpportunity.get().space.z > AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD) {
                return Optional.of(new Intercept(interceptOpportunity.get()));
            }
        }
        return Optional.empty();
    }

    private AgentOutput getThereOnTime(AgentInput input, SpaceTime groundPosition, double reservedBoost) {

        Optional<AgentOutput> flipOut = Optional.empty();

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(input, groundPosition.space);
        if (sensibleFlip.isPresent()) {
            BotLog.println("Front flip for AsapAerial", input.team);
            this.plan = sensibleFlip.get();
            this.plan.begin();
            flipOut = this.plan.getOutput(input);
        }

        if (flipOut.isPresent()) {
            return flipOut.get();
        }

        AgentOutput output = SteerUtil.getThereOnTime(input, groundPosition);
        if (input.getMyBoost() <= reservedBoost + 5) {
            output.withBoost(false);
        }
        return output;
    }

    @Override
    public boolean isBlindlyComplete() {
        return false;
    }

    @Override
    public void begin() {

    }

    @Override
    public String getSituation() {
        return "Working on intercept";
    }
}
