package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.*;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.AccelerationModel;
import tarehart.rlbot.planning.AirTouchPlanner;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.planning.StrikeProfile;

import java.time.Duration;
import java.util.Optional;

public class DirectedKickUtil {
    private static final double BALL_VELOCITY_INFLUENCE = .7;
    private static final double SIDE_HIT_SPEED = 20;

    public static Optional<DirectedKickPlan> planKick(AgentInput input, KickStrategy kickStrategy, boolean isSideHit) {
        Vector3 interceptModifier = (Vector3) kickStrategy.getKickDirection(input).normaliseCopy().scaleCopy(-2);
        return planKick(input, kickStrategy, isSideHit, interceptModifier, new StrikeProfile(.5, 0, 0));
    }

    static Optional<DirectedKickPlan> planKick(AgentInput input, KickStrategy kickStrategy, boolean isSideHit, Vector3 interceptModifier, StrikeProfile strikeProfile) {
        final DirectedKickPlan kickPlan = new DirectedKickPlan();
        kickPlan.interceptModifier = interceptModifier;

        CarData car = input.getMyCarData();

        kickPlan.ballPath = ArenaModel.predictBallPath(input, input.time, Duration.ofSeconds(4));
        kickPlan.distancePlot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4), car.boost, 0);

        Optional<SpaceTime> interceptOpportunity = SteerUtil.getFilteredInterceptOpportunity(car, kickPlan.ballPath, kickPlan.distancePlot, interceptModifier, AirTouchPlanner::isJumpSideFlipAccessible, strikeProfile);
        Optional<SpaceTimeVelocity> ballMotion = interceptOpportunity.flatMap(inter -> kickPlan.ballPath.getMotionAt(inter.time));

        if (!ballMotion.isPresent() || !interceptOpportunity.isPresent()) {
            return Optional.empty();
        }
        kickPlan.ballAtIntercept = ballMotion.get();

        double secondsTillImpactRoughly = TimeUtil.secondsBetween(input.time, kickPlan.ballAtIntercept.getTime());
        double impactSpeed = isSideHit ? SIDE_HIT_SPEED :
                kickPlan.distancePlot.getMotionAfterSeconds(secondsTillImpactRoughly).map(dts -> dts.speed).orElse(AccelerationModel.SUPERSONIC_SPEED);

        Vector3 easyForce;
        if (isSideHit) {
            Vector2 carToIntercept = VectorUtil.flatten((Vector3) interceptOpportunity.get().space.subCopy(car.position));
            Vector2 sideHit = VectorUtil.orthogonal(carToIntercept);
            if (sideHit.dotProduct(VectorUtil.flatten(interceptModifier)) > 0) {
                sideHit.scale(-1);
            }
            easyForce = new Vector3(sideHit.x, sideHit.y, 0);
        } else {
            easyForce = (Vector3) kickPlan.ballAtIntercept.getSpace().subCopy(car.position);
        }

        easyForce.scaleToMagnitude(impactSpeed);

        Vector3 easyKick = bump(kickPlan.ballAtIntercept.getVelocity(), easyForce);
        Vector3 kickDirection = kickStrategy.getKickDirection(input, kickPlan.ballAtIntercept.getSpace(), easyKick);

        if (easyKick.x == kickDirection.x && easyKick.y == kickDirection.y) {
            // The kick strategy is fine with the easy kick.
            kickPlan.plannedKickForce = easyForce;
            kickPlan.desiredBallVelocity = easyKick;
        } else {

            // TODO: this is a rough approximation.
            Vector2 orthogonal = VectorUtil.orthogonal(VectorUtil.flatten(kickDirection));
            Vector2 transverseBallVelocity = VectorUtil.project(VectorUtil.flatten(kickPlan.ballAtIntercept.getVelocity()), orthogonal);
            kickPlan.desiredBallVelocity = (Vector3) kickDirection.normaliseCopy().scaleCopy(impactSpeed + transverseBallVelocity.magnitude() * .7);
            kickPlan.plannedKickForce = kickPlan.desiredBallVelocity.copy();
            kickPlan.plannedKickForce.x -= transverseBallVelocity.x * BALL_VELOCITY_INFLUENCE;
            kickPlan.plannedKickForce.y -= transverseBallVelocity.y * BALL_VELOCITY_INFLUENCE;
        }

        return Optional.of(kickPlan);
    }


    /**
     * https://math.stackexchange.com/questions/13261/how-to-get-a-reflection-vector
     */
    private static Vector3 reflect(Vector3 incident, Vector3 normal) {
        normal = (Vector3) normal.normaliseCopy();
        return (Vector3) incident.subCopy(normal.scaleCopy(2 * incident.dotProduct(normal)));
    }

    private static Vector3 bump(Vector3 incident, Vector3 movingWall) {
        // Move into reference frame of moving wall
        Vector3 incidentAccordingToWall = (Vector3) incident.subCopy(movingWall);
        Vector3 reflectionAccordingToWall = reflect(incidentAccordingToWall, movingWall);
        return reflectionAccordingToWall.addCopy(movingWall);
    }


    static double getAngleOfKickFromApproach(CarData car, DirectedKickPlan kickPlan) {
        Vector2 strikeForceFlat = VectorUtil.flatten(kickPlan.plannedKickForce);
        Vector3 carPositionAtIntercept = kickPlan.getCarPositionAtIntercept();
        Vector2 carToIntercept = VectorUtil.flatten((Vector3) carPositionAtIntercept.subCopy(car.position));
        return SteerUtil.getCorrectionAngleRad(carToIntercept, strikeForceFlat);
    }
}
