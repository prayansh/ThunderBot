package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.*;
import tarehart.rlbot.physics.ArenaModel;
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
    public static final double MAX_NOSE_HIT_ANGLE = Math.PI / 18;
    private static final double MANEUVER_SECONDS_PER_RADIAN = .1;
    private Plan plan;

    private Vector3 originalIntercept;
    private boolean sideFlipMode = false;
    private KickStrategy kickStrategy;
    private Vector3 interceptModifier = null;
    private Vector3 strikeForceVector = null;
    private double maneuverSeconds = 0;
    private double circleBackoff = 0;

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

        if (ArenaModel.isCarOnWall(car)) {
            return Optional.empty();
        }

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(4));
        DistancePlot fullAcceleration = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4), car.boost, 0);

        Vector3 tempInterceptModifier = interceptModifier;
        if (tempInterceptModifier == null) {
            tempInterceptModifier = (Vector3) kickStrategy.getKickDirection(input).normaliseCopy().scaleCopy(-2);
        }

        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(car, ballPath, fullAcceleration, tempInterceptModifier, AirTouchPlanner::isJumpHitAccessible, maneuverSeconds);
        Optional<SpaceTimeVelocity> ballMotion = interceptOpportunity.flatMap(inter -> ballPath.getMotionAt(inter.time));


        if (!ballMotion.isPresent() || !interceptOpportunity.isPresent()) {
            return Optional.empty();
        }
        SpaceTimeVelocity motion = ballMotion.get();
        SpaceTime interceptMoment = interceptOpportunity.get();

        if (originalIntercept == null) {
            originalIntercept = motion.getSpace();
        } else {
            if (originalIntercept.distance(motion.getSpace()) > 30) {
                BotLog.println("Failed to make the directed kick", input.team);
                return Optional.empty(); // Failed to kick it soon enough, new stuff has happened.
            }
        }

        if (strikeForceVector == null) {
            strikeForceVector = getStrikeForce(input, fullAcceleration, motion);
            circleBackoff = car.position.distance(interceptMoment.space) * .2 - 4;
        }

        Vector2 strikeForceFlat = (Vector2) VectorUtil.flatten(strikeForceVector).normaliseCopy();
        Vector2 carToIntercept = VectorUtil.flatten((Vector3) interceptMoment.space.subCopy(car.position));
        double strikeForceCorrection = SteerUtil.getCorrectionAngleRad(carToIntercept, strikeForceFlat);
        double rendezvousCorrection = SteerUtil.getCorrectionAngleRad(car, interceptMoment.space);

        Vector2 steerTarget = VectorUtil.flatten(interceptMoment.space);
        SteerPlan circleTurnPlan = null;


        if (!sideFlipMode) {

            if (interceptModifier == null) {
                interceptModifier = (Vector3) strikeForceVector.normaliseCopy();
                interceptModifier.scale(-1.4);
            }

            if (Math.abs(strikeForceCorrection) < Math.PI / 12 && Math.abs(rendezvousCorrection) < Math.PI / 12) {

                plan = new Plan().withStep(new InterceptStep(interceptModifier));
                plan.begin();
                return plan.getOutput(input);
            }

            if (Math.abs(strikeForceCorrection) < Math.PI / 2) {

                if (Math.abs(strikeForceCorrection) < MAX_NOSE_HIT_ANGLE) {
                    maneuverSeconds = 0;
                    circleTurnPlan = new SteerPlan(SteerUtil.steerTowardGroundPosition(car, steerTarget), steerTarget);
                } else {

                    Vector2 circleTerminus = (Vector2) steerTarget.subCopy(strikeForceFlat.scaleCopy(circleBackoff));
                    double correctionNeeded = strikeForceCorrection - (MAX_NOSE_HIT_ANGLE * Math.signum(strikeForceCorrection));
                    maneuverSeconds = correctionNeeded * MANEUVER_SECONDS_PER_RADIAN;
                    Vector2 terminusFacing = (Vector2) VectorUtil.rotateVector(carToIntercept, correctionNeeded).normaliseCopy();

                    // Line up for a nose hit
                    circleTurnPlan = SteerUtil.getPlanForCircleTurn(car, fullAcceleration, circleTerminus, terminusFacing);
                }

            } else {
                interceptModifier.scale(2); // Back off a little so we have space to side flip.
                sideFlipMode = true;
            }
        }

        if (sideFlipMode) {

            if (VectorUtil.flatten(car.position).distance(steerTarget) < circleBackoff) {
                maneuverSeconds = 0;
                circleTurnPlan = new SteerPlan(SteerUtil.steerTowardGroundPosition(car, steerTarget), steerTarget);
            } else {
                circleTurnPlan = getSideFlipWaypoint(car, steerTarget, carToIntercept, strikeForceFlat, fullAcceleration);
            }

            // If we get here, we can't make the turn for a nose hit, so we'll work on side flips.
            double sideFlipSecs = 0.10;
            Vector2 futureCarPosition = VectorUtil.flatten(car.position.addCopy(car.velocity.scaleCopy(sideFlipSecs)));
            SpaceTimeVelocity ballDuringSideFlip = ballPath.getMotionAt(car.time.plus(TimeUtil.toDuration(sideFlipSecs))).get();
            Vector2 futureCarPositionToBall = (Vector2) VectorUtil.flatten(ballDuringSideFlip.getSpace()).subCopy(futureCarPosition);
            double projectedSideFlipError = Math.abs(SteerUtil.getCorrectionAngleRad(futureCarPositionToBall, strikeForceFlat));
            double projectedDistance = futureCarPosition.distance(steerTarget);
            double timeTillLaunchPad = TimeUtil.secondsBetween(input.time, interceptMoment.time);
            if (projectedDistance < 5) {
                BotLog.println(String.format("Side flip soon. ProjectedDistance: %.2f FlipArrivalTime: %.2f SideFlipError: %.2f",
                        projectedDistance, timeTillLaunchPad - sideFlipSecs, projectedSideFlipError), input.team);
            }
            if (projectedDistance < 4 && Math.abs(timeTillLaunchPad - sideFlipSecs) < .3 && projectedSideFlipError < Math.PI / 10) {
                plan = SetPieces.sideFlip(strikeForceCorrection > 0);
                plan.begin();
                return plan.getOutput(input);
            }
        }

        return getNavigation(input, circleTurnPlan);
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
        double angleCorrection = SteerUtil.getCorrectionAngleRad(car, facingForSideFlip);
        maneuverSeconds = angleCorrection * MANEUVER_SECONDS_PER_RADIAN;

        Vector2 circleTerminus = (Vector2) launchPad.subCopy(facingForSideFlip.scaleCopy(circleBackoff));
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
        return Plan.concatSituation("Directed kick", plan);
    }
}
