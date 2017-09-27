package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.SteerPlan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class DirectedSideHitStep implements Step {

    private static final double MANEUVER_SECONDS_PER_RADIAN = .1;
    private Plan plan;

    private Vector3 originalIntercept;
    private LocalDateTime doneMoment;
    private KickStrategy kickStrategy;
    private Vector3 interceptModifier = null;
    private double maneuverSeconds = 0;
    private static final double CIRCLE_BACKOFF = 1;

    public DirectedSideHitStep(KickStrategy kickStrategy) {
        this.kickStrategy = kickStrategy;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        if (doneMoment == null && car.position.distance(input.ballPosition) < 4.5) {
            // You get a tiny bit more time
            doneMoment = input.time.plus(Duration.ofMillis(1000));
        }

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        if (doneMoment != null && input.time.isAfter(doneMoment)) {
            return Optional.empty();
        }

        if (ArenaModel.isCarOnWall(car)) {
            BotLog.println("Failed to side hit because we're on the wall", input.team);
            return Optional.empty();
        }

        final Optional<DirectedKickPlan> kickPlanOption;
        if (interceptModifier != null) {
            kickPlanOption = DirectedKickUtil.planKick(input, kickStrategy, true, interceptModifier, maneuverSeconds);
        } else {
            kickPlanOption = DirectedKickUtil.planKick(input, kickStrategy, true);
        }

        if (!kickPlanOption.isPresent()) {
            BallPath ballPath = ArenaModel.predictBallPath(input, input.time, Duration.ofSeconds(4));
            return getNavigation(input, new SteerPlan(input.getMyCarData(), ballPath.getEndpoint().getSpace()));
        }

        DirectedKickPlan kickPlan = kickPlanOption.get();


        if (originalIntercept == null) {
            originalIntercept = kickPlan.ballAtIntercept.getSpace();
        } else {
            if (originalIntercept.distance(kickPlan.ballAtIntercept.getSpace()) > 30) {
                BotLog.println("Failed to make the directed kick", input.team);
                return Optional.empty(); // Failed to kick it soon enough, new stuff has happened.
            }
        }

        Vector2 strikeForceFlat = (Vector2) VectorUtil.flatten(kickPlan.plannedKickForce).normaliseCopy();
        Vector3 carPositionAtIntercept = kickPlan.getCarPositionAtIntercept();
        Vector2 carToIntercept = VectorUtil.flatten((Vector3) carPositionAtIntercept.subCopy(car.position));
        double strikeForceCorrection = DirectedKickUtil.getAngleOfKickFromApproach(car, kickPlan);

        Vector2 steerTarget = VectorUtil.flatten(carPositionAtIntercept);
        SteerPlan circleTurnPlan;


        if (interceptModifier == null) {
            interceptModifier = (Vector3) kickPlan.plannedKickForce.normaliseCopy();
            interceptModifier.scale(-3);
        }

        if (VectorUtil.flatten(car.position).distance(steerTarget) < CIRCLE_BACKOFF) {
            maneuverSeconds = 0;
            circleTurnPlan = new SteerPlan(SteerUtil.steerTowardGroundPosition(car, steerTarget), steerTarget);
        } else {
            circleTurnPlan = getSideFlipWaypoint(car, steerTarget, carToIntercept, strikeForceFlat, kickPlan.distancePlot);
        }

        // If we get here, we can't make the turn for a nose hit, so we'll work on side flips.
        double sideFlipSecs = 0.2;
        Vector2 futureCarPosition = VectorUtil.flatten(car.position.addCopy(car.velocity.scaleCopy(sideFlipSecs)));
        SpaceTimeVelocity ballDuringSideFlip = kickPlan.ballPath.getMotionAt(car.time.plus(TimeUtil.toDuration(sideFlipSecs))).get();
        Vector2 futureCarPositionToBall = (Vector2) VectorUtil.flatten(ballDuringSideFlip.getSpace()).subCopy(futureCarPosition);
        double projectedSideFlipError = Math.abs(SteerUtil.getCorrectionAngleRad(futureCarPositionToBall, strikeForceFlat));
        double projectedDistance = futureCarPositionToBall.magnitude();
        if (projectedDistance < 5) {
            BotLog.println(String.format("Side flip soon. Distance: %.2f SideFlipError: %.2f",
                    projectedDistance, projectedSideFlipError), input.team);
        }
        if (projectedDistance < 5 && projectedSideFlipError < .3) {
            plan = SetPieces.sideFlip(strikeForceCorrection > 0);
            plan.begin();
            return plan.getOutput(input);
        }


        return getNavigation(input, circleTurnPlan);
    }

    private Optional<AgentOutput> getNavigation(AgentInput input, SteerPlan circleTurnOption) {
        CarData car = input.getMyCarData();

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, circleTurnOption.waypoint);
        if (sensibleFlip.isPresent()) {
            BotLog.println("Front flip toward side hit", input.team);
            this.plan = sensibleFlip.get();
            this.plan.begin();
            return this.plan.getOutput(input);
        }

        return Optional.of(circleTurnOption.immediateSteer);
    }

    private SteerPlan getSideFlipWaypoint(CarData car, Vector2 launchPad, Vector2 carToIntercept, Vector2 strikeForce, DistancePlot fullAcceleration) {

        Vector2 facingForSideFlip = VectorUtil.orthogonal(strikeForce);
        if (facingForSideFlip.dotProduct(carToIntercept) < 0) {
            facingForSideFlip.scale(-1);
        }
        double angleCorrection = SteerUtil.getCorrectionAngleRad(carToIntercept, facingForSideFlip);
        maneuverSeconds = Math.abs(angleCorrection) * MANEUVER_SECONDS_PER_RADIAN;

        Vector2 circleTerminus = (Vector2) launchPad.subCopy(facingForSideFlip.scaleCopy(CIRCLE_BACKOFF));
        return SteerUtil.getPlanForCircleTurn(car, fullAcceleration, circleTerminus, (Vector2) facingForSideFlip.normaliseCopy());
    }

    @Override
    public boolean isBlindlyComplete() {
        return false;
    }

    @Override
    public void begin() {

    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return Plan.concatSituation("Directed Side Hit", plan);
    }
}
