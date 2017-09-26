package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.AccelerationModel;
import tarehart.rlbot.planning.AirTouchPlanner;
import tarehart.rlbot.planning.SteerUtil;

import java.time.Duration;
import java.util.Optional;

public class DirectedKickUtil {
    private static final double BALL_VELOCITY_INFLUENCE = .7;

    public static Optional<DirectedKickPlan> planKick(AgentInput input, KickStrategy kickStrategy, boolean isSideHit) {
        Vector3 interceptModifier = (Vector3) kickStrategy.getKickDirection(input).normaliseCopy().scaleCopy(-2);
        return planKick(input, kickStrategy, isSideHit, interceptModifier, .5);
    }

    static Optional<DirectedKickPlan> planKick(AgentInput input, KickStrategy kickStrategy, boolean isSideHit, Vector3 interceptModifier, Double maneuverSeconds) {
        final DirectedKickPlan kickPlan = new DirectedKickPlan();
        kickPlan.interceptModifier = interceptModifier;

        CarData car = input.getMyCarData();

        kickPlan.ballPath = ArenaModel.predictBallPath(input, input.time, Duration.ofSeconds(4));
        kickPlan.distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4), car.boost, 0);

        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(car, kickPlan.ballPath, kickPlan.distancePlot, interceptModifier, AirTouchPlanner::isJumpHitAccessible, maneuverSeconds);
        Optional<SpaceTimeVelocity> ballMotion = interceptOpportunity.flatMap(inter -> kickPlan.ballPath.getMotionAt(inter.time));

        if (!ballMotion.isPresent() || !interceptOpportunity.isPresent()) {
            return Optional.empty();
        }
        kickPlan.ballAtIntercept = ballMotion.get();

        Vector3 kickDirection;
        if (isSideHit) {

            Vector2 carToIntercept = VectorUtil.flatten((Vector3) interceptOpportunity.get().space.subCopy(car.position));
            Vector2 sideHit = VectorUtil.orthogonal(carToIntercept);
            if (sideHit.dotProduct(VectorUtil.flatten(interceptModifier)) > 0) {
                sideHit.scale(-1);
            }

            kickDirection = kickStrategy.getKickDirection(input, kickPlan.ballAtIntercept.getSpace(), new Vector3(sideHit.x, sideHit.y, 0));
        } else {
            kickDirection = kickStrategy.getKickDirection(input, kickPlan.ballAtIntercept.getSpace());
        }

        DistanceTimeSpeed carMotion = kickPlan.distancePlot.getMotionAt(kickPlan.ballAtIntercept.getTime()).get();
        kickPlan.desiredBallVelocity = (Vector3) kickDirection.normaliseCopy().scaleCopy(carMotion.speed);
        Vector2 orthogonal = VectorUtil.orthogonal(VectorUtil.flatten(kickDirection));
        Vector2 transverseBallVelocity = VectorUtil.project(VectorUtil.flatten(kickPlan.ballAtIntercept.getVelocity()), orthogonal);
        kickPlan.plannedKickForce = kickPlan.desiredBallVelocity.copy();
        kickPlan.plannedKickForce.x -= transverseBallVelocity.x * BALL_VELOCITY_INFLUENCE;
        kickPlan.plannedKickForce.y -= transverseBallVelocity.y * BALL_VELOCITY_INFLUENCE;

        return Optional.of(kickPlan);
    }

    static double getAngleOfKickFromApproach(CarData car, DirectedKickPlan kickPlan) {
        Vector2 strikeForceFlat = VectorUtil.flatten(kickPlan.plannedKickForce);
        Vector3 carPositionAtIntercept = kickPlan.getCarPositionAtIntercept();
        Vector2 carToIntercept = VectorUtil.flatten((Vector3) carPositionAtIntercept.subCopy(car.position));
        return SteerUtil.getCorrectionAngleRad(carToIntercept, strikeForceFlat);
    }
}
