package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector;
import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
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

    public static final double BALL_VELOCITY_INFLUENCE = 1;
    private Plan plan;
    private boolean isComplete;

    private Vector2 originalIntercept;
    private boolean sideFlipMode = false;
    private KickStrategy kickStrategy;

    public DirectedKickStep(KickStrategy kickStrategy) {
        this.kickStrategy = kickStrategy;
    }

    public static boolean canMakeDirectedKick(AgentInput input) {
        return BallPhysics.getGroundBounceEnergy(input) < 50;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }


        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(4));
        DistancePlot fullAcceleration = AccelerationModel.simulateAcceleration(input, Duration.ofSeconds(4), input.getMyBoost(), 0);

        Vector3 interceptModifier = (Vector3) kickStrategy.getKickDirection(input).normaliseCopy().scaleCopy(-2);

        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(input, ballPath, fullAcceleration, interceptModifier, AirTouchPlanner::isJumpHitAccessible);
        Optional<SpaceTimeVelocity> ballMotion = interceptOpportunity.flatMap(inter -> ballPath.getMotionAt(inter.time));


        if (!ballMotion.isPresent()) {
            return Optional.empty();
        }
        SpaceTimeVelocity motion = ballMotion.get();
        Vector2 intercept = VectorUtil.flatten(motion.getSpace());

        if (originalIntercept == null) {
            originalIntercept = intercept;
        } else {
            if (originalIntercept.distance(intercept) > 20) {
                BotLog.println("Failed to make the directed kick", input.team);
                return Optional.empty(); // Failed to kick it soon enough, new stuff has happened.
            }
        }

        Vector3 strikeForceVector = getStrikeForce(input, fullAcceleration, motion);

        Vector2 strikeForceFlat = VectorUtil.flatten(strikeForceVector);
        Vector2 carToIntercept = VectorUtil.flatten((Vector3) motion.space.subCopy(input.getMyPosition()));
        double approachCorrection = SteerUtil.getCorrectionAngleRad(carToIntercept, strikeForceFlat);
        double orientationCorrection = SteerUtil.getCorrectionAngleRad(input, motion.space);


        Optional<Vector2> circleTurnOption = Optional.empty();


        if (!sideFlipMode) {

            Vector3 strikePointModifier = (Vector3) strikeForceVector.normaliseCopy();
            strikePointModifier.scale(-1.4);

            if (Math.abs(approachCorrection) < Math.PI / 8 && Math.abs(orientationCorrection) < Math.PI / 8) {

                plan = new Plan().withStep(new InterceptStep(strikePointModifier));
                plan.begin();
                return plan.getOutput(input);
            }

            if (Math.abs(approachCorrection) < Math.PI / 3) {

                Vector2 waypoint = (Vector2) intercept.addCopy(VectorUtil.flatten(strikePointModifier));

                // Line up for a nose hit
                circleTurnOption = SteerUtil.getWaypointForCircleTurn(input, fullAcceleration, waypoint, (Vector2) strikeForceFlat.normaliseCopy());
            }

            if (!circleTurnOption.isPresent()) {
                sideFlipMode = true;
            }
        }

        if (sideFlipMode) {

            Vector2 launchPad = getSideFlipLaunchPad(intercept, strikeForceFlat);
            circleTurnOption = getSideFlipWaypoint(input, launchPad, carToIntercept, strikeForceFlat, fullAcceleration);

            // If we get here, we can't make the turn for a nose hit, so we'll work on side flips.
            double sideFlipSecs = 0.3;
            Vector2 futureCarPosition = VectorUtil.flatten(input.getMyPosition().addCopy(input.getMyVelocity().scaleCopy(sideFlipSecs)));
            double flipArrival = futureCarPosition.distance(launchPad);
            double flipArrivalSecs = TimeUtil.secondsBetween(input.time, motion.getTime());
            if (flipArrival < 5) {
                BotLog.println(String.format("Side flip soon. FlipArrivalDistance: %s FlipArrivalTime: %s", flipArrival, flipArrivalSecs), input.team);
            }
            if (flipArrival < 1.5 && Math.abs(flipArrivalSecs - sideFlipSecs) < .25) {
                plan = SetPieces.sideFlip(approachCorrection > 0);
                plan.begin();
                return plan.getOutput(input);
            }
        }


        return getNavigation(input, circleTurnOption);
    }

    private Optional<AgentOutput> getNavigation(AgentInput input, Optional<Vector2> circleTurnOption) {
        if (circleTurnOption.isPresent()) {
            Vector2 circleTurn = circleTurnOption.get();
            Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(input, circleTurn);
            if (sensibleFlip.isPresent()) {
                BotLog.println("Front flip toward directed kick", input.team);
                this.plan = sensibleFlip.get();
                this.plan.begin();
                return this.plan.getOutput(input);
            }

            return Optional.of(SteerUtil.steerTowardPosition(input, circleTurn));
        }

        BotLog.println("Failing directed kick because we have no circle turns.", input.team);
        return Optional.empty();
    }

    private Vector3 getStrikeForce(AgentInput input, DistancePlot acceleration, SpaceTimeVelocity ballIntercept) {
        Vector3 kickDirection = kickStrategy.getKickDirection(input, ballIntercept);
        Vector2 orthogonal = VectorUtil.orthogonal(VectorUtil.flatten(kickDirection));
        Vector2 transverseBallVelocity = VectorUtil.project(VectorUtil.flatten(ballIntercept.getVelocity()), orthogonal);
        DistanceTimeSpeed carMotion = acceleration.getMotionAt(ballIntercept.getTime()).get();
        Vector3 exitVector = (Vector3) kickDirection.normaliseCopy().scaleCopy(carMotion.speed);
        Vector3 strikeForceVector = exitVector.copy();
        strikeForceVector.x -= transverseBallVelocity.x * BALL_VELOCITY_INFLUENCE;
        strikeForceVector.y -= transverseBallVelocity.y * BALL_VELOCITY_INFLUENCE;
        return strikeForceVector;
    }

    private Optional<Vector2> getSideFlipWaypoint(AgentInput input, Vector2 launchPad, Vector2 carToIntercept, Vector2 strikeForce, DistancePlot fullAcceleration) {

        Vector2 facingForSideFlip = VectorUtil.orthogonal(strikeForce);
        if (facingForSideFlip.dotProduct(carToIntercept) < 0) {
            facingForSideFlip.scale(-1);
        }
        return SteerUtil.getWaypointForCircleTurn(input, fullAcceleration, launchPad, (Vector2) facingForSideFlip.normaliseCopy());
    }

    private Vector2 getSideFlipLaunchPad(Vector2 intercept, Vector2 strikeForce) {
        Vector2 strikePointModifier = (Vector2) strikeForce.normaliseCopy().scaleCopy(-3);
        return (Vector2) intercept.addCopy(strikePointModifier);
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
