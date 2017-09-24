package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.*;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.BallPhysics;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.Optional;

public class DirectedKickStep implements Step {

    private static final double BALL_VELOCITY_INFLUENCE = .7;
    private Plan plan;
    private boolean isComplete;

    private Vector3 originalIntercept;
    private boolean sideFlipMode = false;
    private KickStrategy kickStrategy;
    private Vector3 ballToSteerGoal = null;
    private Vector3 strikeForceVector = null;

    public DirectedKickStep(KickStrategy kickStrategy) {
        this.kickStrategy = kickStrategy;
    }

    public static boolean canMakeDirectedKick(AgentInput input) {
        return BallPhysics.getGroundBounceEnergy(
                new SpaceTimeVelocity(input.ballPosition, input.time, input.ballVelocity)) < 50;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        CarData car = input.getMyCarData();
        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(4));
        DistancePlot fullAcceleration = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4), car.boost, 0);

        Vector3 interceptModifier = (Vector3) kickStrategy.getKickDirection(input).normaliseCopy().scaleCopy(-2);
        if (ballToSteerGoal != null) {
            interceptModifier = ballToSteerGoal;
        }

        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(car, ballPath, fullAcceleration, interceptModifier, AirTouchPlanner::isJumpHitAccessible);
        Optional<SpaceTimeVelocity> ballMotion = interceptOpportunity.flatMap(inter -> ballPath.getMotionAt(inter.time));


        if (!ballMotion.isPresent() || !interceptOpportunity.isPresent()) {
            return Optional.empty();
        }
        SpaceTimeVelocity motion = ballMotion.get();
        SpaceTime interceptMoment = interceptOpportunity.get();

        if (originalIntercept == null) {
            originalIntercept = motion.getSpace();
        } else {
            if (originalIntercept.distance(motion.getSpace()) > 20) {
                BotLog.println("Failed to make the directed kick", input.team);
                return Optional.empty(); // Failed to kick it soon enough, new stuff has happened.
            }
        }

        if (strikeForceVector == null) {
            strikeForceVector = getStrikeForce(input, fullAcceleration, motion);
        }

        Vector2 strikeForceFlat = VectorUtil.flatten(strikeForceVector);
        Vector2 carToIntercept = VectorUtil.flatten((Vector3) interceptMoment.space.subCopy(car.position));
        double strikeForceCorrection = SteerUtil.getCorrectionAngleRad(carToIntercept, strikeForceFlat);
        double rendezvousCorrection = SteerUtil.getCorrectionAngleRad(car, interceptMoment.space);


        Vector2 intercept = VectorUtil.flatten(interceptMoment.space);
        SteerPlan circleTurnOption = null;


        if (!sideFlipMode) {

            if (ballToSteerGoal == null) {
                ballToSteerGoal = (Vector3) strikeForceVector.normaliseCopy();
                ballToSteerGoal.scale(-1.4);
            }

            if (Math.abs(strikeForceCorrection) < Math.PI / 12 && Math.abs(rendezvousCorrection) < Math.PI / 12) {

                plan = new Plan().withStep(new InterceptStep(ballToSteerGoal));
                plan.begin();
                return plan.getOutput(input);
            }

            if (Math.abs(strikeForceCorrection) < Math.PI / 2) {

                // Line up for a nose hit
                circleTurnOption = SteerUtil.getPlanForCircleTurn(car, fullAcceleration, intercept, (Vector2) strikeForceFlat.normaliseCopy());
            } else {
                ballToSteerGoal.scale(2); // Back off a little so we have space to side flip.
                sideFlipMode = true;
            }
        }

        if (sideFlipMode) {

            circleTurnOption = getSideFlipWaypoint(car, intercept, carToIntercept, strikeForceFlat, fullAcceleration);

            // If we get here, we can't make the turn for a nose hit, so we'll work on side flips.
            double sideFlipSecs = 0.18;
            Vector2 futureCarPosition = VectorUtil.flatten(car.position.addCopy(car.velocity.scaleCopy(sideFlipSecs)));
            double projectedDistance = futureCarPosition.distance(intercept);
            double timeTillLaunchPad = TimeUtil.secondsBetween(input.time, interceptMoment.time);
            if (projectedDistance < 5) {
                BotLog.println(String.format("Side flip soon. ProjectedDistance: %s FlipArrivalTime: %s", projectedDistance, timeTillLaunchPad - sideFlipSecs), input.team);
            }
            if (projectedDistance < 1.5 && Math.abs(timeTillLaunchPad - sideFlipSecs) < .25) {
                plan = SetPieces.sideFlip(strikeForceCorrection > 0);
                plan.begin();
                return plan.getOutput(input);
            }
        }


        return getNavigation(input, circleTurnOption);
    }

    private Optional<AgentOutput> getNavigation(AgentInput input, SteerPlan circleTurnOption) {
        CarData car = input.getMyCarData();

        Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, circleTurnOption.waypoint);
        if (sensibleFlip.isPresent()) {
            BotLog.println("Front flip toward directed kick", input.team);
            this.plan = sensibleFlip.get();
            this.plan.begin();
            return this.plan.getOutput(input);
        }

        return Optional.of(circleTurnOption.immediateSteer);
    }

    private Vector3 getStrikeForce(AgentInput input, DistancePlot acceleration, SpaceTimeVelocity ballIntercept) {
        Vector3 kickDirection = kickStrategy.getKickDirection(input, ballIntercept.getSpace());
        Vector2 orthogonal = VectorUtil.orthogonal(VectorUtil.flatten(kickDirection));
        Vector2 transverseBallVelocity = VectorUtil.project(VectorUtil.flatten(ballIntercept.getVelocity()), orthogonal);
        DistanceTimeSpeed carMotion = acceleration.getMotionAt(ballIntercept.getTime()).get();
        Vector3 exitVector = (Vector3) kickDirection.normaliseCopy().scaleCopy(carMotion.speed);
        Vector3 strikeForceVector = exitVector.copy();
        strikeForceVector.x -= transverseBallVelocity.x * BALL_VELOCITY_INFLUENCE;
        strikeForceVector.y -= transverseBallVelocity.y * BALL_VELOCITY_INFLUENCE;
        return strikeForceVector;
    }

    private SteerPlan getSideFlipWaypoint(CarData car, Vector2 launchPad, Vector2 carToIntercept, Vector2 strikeForce, DistancePlot fullAcceleration) {

        Vector2 facingForSideFlip = VectorUtil.orthogonal(strikeForce);
        if (facingForSideFlip.dotProduct(carToIntercept) < 0) {
            facingForSideFlip.scale(-1);
        }
        return SteerUtil.getPlanForCircleTurn(car, fullAcceleration, launchPad, (Vector2) facingForSideFlip.normaliseCopy());
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
        return Plan.concatSituation("Directed kick", plan);
    }
}
