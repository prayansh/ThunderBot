package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector;
import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.BallPhysics;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.Optional;

public class DirectedKickStep implements Step {

    private Plan plan;
    private boolean isComplete;
    private Vector3 target;
    private Vector2 originalIntercept;
    private boolean sideFlipMode = false;

    public DirectedKickStep(Vector3 target) {
        this.target = target;
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

        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(input, ballPath, fullAcceleration, new Vector3(), AirTouchPlanner::isJumpHitAccessible);

        Optional<SpaceTimeVelocity> ballMotion = interceptOpportunity.flatMap(inter -> ballPath.getMotionAt(inter.time));


        if (!ballMotion.isPresent()) {
            return Optional.empty();
        }
        SpaceTimeVelocity motion = ballMotion.get();

        if (originalIntercept == null) {
            originalIntercept = VectorUtil.flatten(motion.getSpace());
        } else {
            if (originalIntercept.distance(VectorUtil.flatten(motion.getSpace())) > 20) {
                BotLog.println("Failed to make the directed kick", input.team);
                return Optional.empty(); // Failed to kick it soon enough, new stuff has happened.
            }
        }

        Vector3 direction = (Vector3) target.subCopy(motion.getSpace());

        Vector2 orthogonal = VectorUtil.orthogonal(VectorUtil.flatten(direction));

        Vector2 transverseBallVelocity = VectorUtil.project(VectorUtil.flatten(motion.getVelocity()), orthogonal);
        DistanceTimeSpeed carMotion = fullAcceleration.getMotionAt(motion.getTime()).get();
        Vector3 exitVector = (Vector3) direction.normaliseCopy().scaleCopy(carMotion.speed);
        Vector3 strikeForceVector = exitVector.copy();
        strikeForceVector.x -= transverseBallVelocity.x * 1.5;
        strikeForceVector.y -= transverseBallVelocity.y * 1.5;

        Vector3 strikePointModifier = (Vector3) strikeForceVector.normaliseCopy();
        strikePointModifier.scale(-1.4);

        Vector2 strikeForceFlat = VectorUtil.flatten(strikeForceVector);

        Vector2 carToIntercept = VectorUtil.flatten((Vector3) motion.space.subCopy(input.getMyPosition()));
        double approachCorrection = SteerUtil.getCorrectionAngleRad(carToIntercept, strikeForceFlat);
        double orientationCorrection = SteerUtil.getCorrectionAngleRad(input, motion.space);
        Vector2 waypoint = VectorUtil.flatten(motion.getSpace());
        waypoint.add(VectorUtil.flatten(strikePointModifier));

        if (Math.abs(approachCorrection) < Math.PI / 8 && Math.abs(orientationCorrection) < Math.PI / 8) {

            plan = new Plan().withStep(new InterceptStep(strikePointModifier));
            plan.begin();
            return plan.getOutput(input);
        }

        // If we get here, we can't make the turn for a nose hit, so we'll work on side flips.
        if (sideFlipMode && Duration.between(input.time, motion.getTime()).toMillis() < 500) {
            plan = SetPieces.sideFlip(approachCorrection > 0);
            plan.begin();
            return plan.getOutput(input);
        }

        Optional<Vector2> circleTurnOption = Optional.empty();

        if (!sideFlipMode && Math.abs(approachCorrection) < Math.PI / 3) {
            // Line up for a nose hit
            circleTurnOption = SteerUtil.getWaypointForCircleTurn(input, fullAcceleration, waypoint, (Vector2) strikeForceFlat.normaliseCopy());
        }

        if (!circleTurnOption.isPresent()) {
            Vector2 facingForSideFlip = VectorUtil.orthogonal(strikeForceFlat);
            if (facingForSideFlip.dotProduct(carToIntercept) < 0) {
                facingForSideFlip.scale(-1);
            }
            circleTurnOption = SteerUtil.getWaypointForCircleTurn(input, fullAcceleration, waypoint, (Vector2) facingForSideFlip.normaliseCopy());
            if (circleTurnOption.isPresent()) {
                sideFlipMode = true;
            }
        }


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
