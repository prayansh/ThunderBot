package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class InterceptStep implements Step {

    private Plan plan;
    private Vector3 interceptModifier;
    private LocalDateTime doneMoment;
    private Vector3 originalIntercept;

    public InterceptStep(Vector3 interceptModifier) {
        this.interceptModifier = interceptModifier;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        if (doneMoment != null && input.time.isAfter(doneMoment)) {
            return Optional.empty();
        }

        CarData carData = input.getMyCarData();

        if (doneMoment == null && carData.position.distance(input.ballPosition) < 4.5) {
            // You get a tiny bit more time
            doneMoment = input.time.plus(Duration.ofMillis(1000));
        }

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(4));
        DistancePlot fullAcceleration = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4), carData.boost, 0);

        List<Intercept> interceptOptions = new ArrayList<>();
        getAerialIntercept(carData, ballPath).ifPresent(interceptOptions::add);
        getJumpHitIntercept(carData, ballPath, fullAcceleration).ifPresent(interceptOptions::add);
        getFlipHitIntercept(carData, ballPath, fullAcceleration).ifPresent(interceptOptions::add);

        Optional<Intercept> chosenIntercept = interceptOptions.stream().sorted(Comparator.comparing(Intercept::getTime)).findFirst();
        Optional<Plan> launchPlan = chosenIntercept.flatMap(cept -> InterceptPlanner.planImmediateLaunch(input.getMyCarData(), cept.toSpaceTime()));
        if (launchPlan.isPresent()) {
            plan = launchPlan.get();
            plan.begin();
            return plan.getOutput(input);
        }

        if (chosenIntercept.isPresent()) {
            if (originalIntercept == null) {
                originalIntercept = chosenIntercept.get().getSpace();
            } else {
                if (originalIntercept.distance(chosenIntercept.get().getSpace()) > 20) {
                    BotLog.println("Failed to make the intercept", input.team);
                    return Optional.empty(); // Failed to kick it soon enough, new stuff has happened.
                }
            }
        }


        return chosenIntercept.map(intercept -> getThereOnTime(input, intercept.toSpaceTime(), intercept.getAirBoost()));
    }

    public Optional<Intercept> getAerialIntercept(CarData carData, BallPath ballPath) {
        if (carData.boost >= AirTouchPlanner.BOOST_NEEDED_FOR_AERIAL) {
            DistancePlot budgetAcceleration = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4), AirTouchPlanner.getBoostBudget(carData), 0);
            Optional<SpaceTime> budgetInterceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(carData, ballPath, budgetAcceleration, interceptModifier, AirTouchPlanner::isVerticallyAccessible);
            if (budgetInterceptOpportunity.isPresent()) {
                SpaceTime spaceTime = budgetInterceptOpportunity.get();
                if (budgetInterceptOpportunity.get().space.z > AirTouchPlanner.NEEDS_AERIAL_THRESHOLD) {
                    return Optional.of(new Intercept(spaceTime.space, spaceTime.time, AirTouchPlanner.BOOST_NEEDED_FOR_AERIAL));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Intercept> getJumpHitIntercept(CarData carData, BallPath ballPath, DistancePlot fullAcceleration) {
        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(carData, ballPath, fullAcceleration, interceptModifier, AirTouchPlanner::isJumpHitAccessible);
        if (interceptOpportunity.isPresent()) {
            if (interceptOpportunity.get().space.z > AirTouchPlanner.NEEDS_JUMP_HIT_THRESHOLD) {
                return Optional.of(new Intercept(interceptOpportunity.get()));
            }
        }
        return Optional.empty();
    }

    public Optional<Intercept> getFlipHitIntercept(CarData carData, BallPath ballPath, DistancePlot fullAcceleration) {
        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(carData, ballPath, fullAcceleration, interceptModifier, AirTouchPlanner::isFlipHitAccessible);
        return interceptOpportunity.map(Intercept::new);
    }

    private AgentOutput getThereOnTime(AgentInput input, SpaceTime groundPosition, double reservedBoost) {

        Optional<AgentOutput> flipOut = Optional.empty();
        CarData car = input.getMyCarData();

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, groundPosition.space);
        if (sensibleFlip.isPresent()) {
            BotLog.println("Front flip for AsapAerial", input.team);
            this.plan = sensibleFlip.get();
            this.plan.begin();
            flipOut = this.plan.getOutput(input);
        }

        if (flipOut.isPresent()) {
            return flipOut.get();
        }

        AgentOutput output = SteerUtil.getThereOnTime(car, groundPosition);
        if (car.boost <= reservedBoost + 5) {
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
        return Plan.concatSituation("Working on intercept", plan);
    }
}
