package tarehart.rlbot.planning;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.steps.CatchBallStep;
import tarehart.rlbot.steps.DribbleStep;
import tarehart.rlbot.steps.GetBoostStep;
import tarehart.rlbot.steps.GetOnOffenseStep;
import tarehart.rlbot.steps.defense.GetOnDefenseStep;
import tarehart.rlbot.steps.defense.ThreatAssessor;
import tarehart.rlbot.steps.strikes.*;
import tarehart.rlbot.steps.wall.DescendFromWallStep;
import tarehart.rlbot.steps.wall.MountWallStep;
import tarehart.rlbot.steps.wall.WallTouchStep;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class TacticsAdvisor {

    private static final double lookaheadSeconds = 3;

    public TacticsAdvisor() {
    }

    public Plan makePlan(AgentInput input) {

        Duration planHorizon = Duration.ofSeconds(5);

        CarData car = input.getMyCarData();
        BallPath ballPath = ArenaModel.predictBallPath(input, input.time, planHorizon);
        DistancePlot distancePlot = AccelerationModel.simulateAcceleration(car, planHorizon, car.boost);

        TacticalSituation situation = assessSituation(input, ballPath);

        Optional<Intercept> interceptStepOffering = InterceptStep.getSoonestIntercept(input.getMyCarData(), ballPath, distancePlot, new Vector3());
        LocalDateTime ourExpectedContactTime = interceptStepOffering.map(Intercept::getTime).orElse(ballPath.getEndpoint().getTime());

        if (situation.ownGoalFutureProximity > 100) {
            return makePlanWithPlentyOfTime(input, situation, ballPath);
        }

        double raceResult = TimeUtil.secondsBetween(ourExpectedContactTime, situation.expectedEnemyContact.time);

        if (raceResult > 2) {
            // We can take our sweet time. Now figure out whether we want a directed kick, a dribble, an intercept, a catch, etc
            return makePlanWithPlentyOfTime(input, situation, ballPath);
        }

        if (raceResult > .5) {
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new IdealDirectedHitStep(new KickAtEnemyGoal(), input));
        }

        if (raceResult > -.5) {

            if (!interceptStepOffering.isPresent()) {
                // Nobody is getting to the ball any time soon.
                return makePlanWithPlentyOfTime(input, situation, ballPath);
            }

            if (Math.abs(situation.enemyOffensiveApproachCorrection) < Math.PI / 3) {

                // Enemy is threatening us

                if (GetOnOffenseStep.getYAxisWrongSidedness(input) < 0) {

                    // Consider this to be a 50-50. Go hard for the intercept
                    Vector3 ownGoalCenter = GoalUtil.getOwnGoal(input.team).getCenter();
                    Vector3 interceptPosition = interceptStepOffering.get().getSpace();
                    Vector3 toOwnGoal = (Vector3) ownGoalCenter.subCopy(interceptPosition);
                    Vector3 interceptModifier = (Vector3) toOwnGoal.normaliseCopy();

                    return new Plan(Plan.Posture.OFFENSIVE).withStep(new InterceptStep(interceptModifier));
                } else {
                    // We're not in a good position to go for a 50-50. Get on defense.
                    return new Plan(Plan.Posture.DEFENSIVE).withStep(new GetOnDefenseStep());
                }
            } else {
                // Doesn't matter if enemy wins the race, they are out of position.
                return makePlanWithPlentyOfTime(input, situation, ballPath);
            }
        }

        // The enemy is probably going to get there first.
        if (Math.abs(situation.enemyOffensiveApproachCorrection) < Math.PI / 3 && situation.distanceBallIsBehindUs > -50) {
            // Enemy can probably shoot on goal, so get on defense
            return new Plan(Plan.Posture.DEFENSIVE).withStep(new GetOnDefenseStep());
        } else {
            // Enemy is just gonna hit it for the sake of hitting it, presumably. Let's try to stay on offense if possible.
            // TODO: make sure we don't own-goal it with this
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new GetOnOffenseStep()).withStep(new InterceptStep(new Vector3()));
        }

    }

    private Plan makePlanWithPlentyOfTime(AgentInput input, TacticalSituation situation, BallPath ballPath) {

        CarData car = input.getMyCarData();

        if (situation.distanceFromEnemyBackWall < 15) {
            Optional<SpaceTime> catchOpportunity = SteerUtil.getCatchOpportunity(car, ballPath, car.boost);
            if (catchOpportunity.isPresent()) {
                return new Plan().withStep(new CatchBallStep(catchOpportunity.get())).withStep(new DribbleStep());
            }
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new IdealDirectedHitStep(new FunnelTowardEnemyGoal(), input));
        }

        if (DribbleStep.canDribble(input, false) && input.ballVelocity.magnitude() > 15) {
            BotLog.println("Beginning dribble", input.team);
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new DribbleStep());
        }  else if (WallTouchStep.hasWallTouchOpportunity(input, ballPath)) {
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new MountWallStep()).withStep(new WallTouchStep()).withStep(new DescendFromWallStep());
        } else if (DirectedNoseHitStep.canMakeDirectedKick(input)) {
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new IdealDirectedHitStep(new KickAtEnemyGoal(), input));
        } else if (car.boost < 30) {
            return new Plan().withStep(new GetBoostStep());
        } else if (GetOnOffenseStep.getYAxisWrongSidedness(input) > 0) {
            BotLog.println("Getting behind the ball", input.team);
            return new Plan(Plan.Posture.NEUTRAL).withStep(new GetOnOffenseStep());
        } else {
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new InterceptStep(new Vector3()));
        }
    }

    private TacticalSituation assessSituation(AgentInput input, BallPath ballPath) {

        Optional<SpaceTime> enemyIntercept = getEnemyIntercept(input, ballPath);

        SpaceTimeVelocity futureBallMotion = ballPath.getMotionAt(input.time.plus(TimeUtil.toDuration(lookaheadSeconds))).orElse(ballPath.getEndpoint());

        TacticalSituation situation = new TacticalSituation();
        situation.expectedEnemyContact = enemyIntercept.orElse(ballPath.getEndpoint().toSpaceTime());
        situation.ownGoalFutureProximity = VectorUtil.flatDistance(GoalUtil.getOwnGoal(input.team).getCenter(), futureBallMotion.getSpace());
        situation.distanceBallIsBehindUs = measureOutOfPosition(input);
        situation.enemyOffensiveApproachCorrection = measureEnemyApproachError(input, situation.expectedEnemyContact);
        double enemyGoalY = GoalUtil.getEnemyGoal(input.team).getCenter().y;
        situation.distanceFromEnemyBackWall = Math.abs(enemyGoalY - futureBallMotion.space.y);
        situation.distanceFromEnemyCorner = getDistanceFromEnemyCorner(futureBallMotion, enemyGoalY);

        return situation;
    }

    private double getDistanceFromEnemyCorner(SpaceTimeVelocity futureBallMotion, double enemyGoalY) {
        Vector2 corner1 = (Vector2) ArenaModel.CORNER_ANGLE_CENTER.copy();
        Vector2 corner2 = (Vector2) ArenaModel.CORNER_ANGLE_CENTER.copy();

        corner1.y *= Math.signum(enemyGoalY);
        corner2.y *= Math.signum(enemyGoalY);
        corner2.x *= -1;

        Vector2 ballFutureFlat = VectorUtil.flatten(futureBallMotion.space);

        return Math.min(ballFutureFlat.distance(corner1), ballFutureFlat.distance(corner2));
    }

    private Optional<SpaceTime> getEnemyIntercept(AgentInput input, BallPath ballPath) {

        CarData enemyCar = input.getEnemyCarData();
        return SteerUtil.getInterceptOpportunityAssumingMaxAccel(enemyCar, ballPath, enemyCar.boost);
    }

    private double measureEnemyApproachError(AgentInput input, SpaceTime enemyContact) {

        CarData enemyCar = input.getEnemyCarData();
        Goal myGoal = GoalUtil.getOwnGoal(input.team);
        Vector3 ballToGoal = (Vector3) myGoal.getCenter().subCopy(enemyContact.space);

        Vector3 carToBall = (Vector3) enemyContact.space.subCopy(enemyCar.position);

        return SteerUtil.getCorrectionAngleRad(VectorUtil.flatten(ballToGoal), VectorUtil.flatten(carToBall));
    }


    private double measureOutOfPosition(AgentInput input) {
        CarData car = input.getMyCarData();
        Goal myGoal = GoalUtil.getOwnGoal(input.team);
        Vector3 ballToGoal = (Vector3) myGoal.getCenter().subCopy(input.ballPosition);
        Vector3 carToBall = (Vector3) input.ballPosition.subCopy(car.position);
        Vector3 wrongSideVector = VectorUtil.project(carToBall, ballToGoal);
        return wrongSideVector.magnitude() * Math.signum(wrongSideVector.dotProduct(ballToGoal));
    }

}
