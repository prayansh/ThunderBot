package tarehart.rlbot.planning;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.steps.DribbleStep;
import tarehart.rlbot.steps.GetBoostStep;
import tarehart.rlbot.steps.GetOnOffenseStep;
import tarehart.rlbot.steps.defense.GetOnDefenseStep;
import tarehart.rlbot.steps.defense.ThreatAssessor;
import tarehart.rlbot.steps.strikes.DirectedNoseHitStep;
import tarehart.rlbot.steps.strikes.IdealDirectedHitStep;
import tarehart.rlbot.steps.strikes.InterceptStep;
import tarehart.rlbot.steps.strikes.KickAtEnemyGoal;
import tarehart.rlbot.steps.wall.DescendFromWallStep;
import tarehart.rlbot.steps.wall.MountWallStep;
import tarehart.rlbot.steps.wall.WallTouchStep;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class TacticsAdvisor {

    private ThreatAssessor threatAssessor;

    public TacticsAdvisor() {
        this.threatAssessor = new ThreatAssessor();
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

        // TODO: this logic needs improvement.
        // Sometimes we should catch and dribble.

        if (DribbleStep.canDribble(input, false) && input.ballVelocity.magnitude() > 15) {
            BotLog.println("Beginning dribble", input.team);
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new DribbleStep());
        }  else if (WallTouchStep.hasWallTouchOpportunity(input, ballPath)) {
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new MountWallStep()).withStep(new WallTouchStep()).withStep(new DescendFromWallStep());
        } else if (DirectedNoseHitStep.canMakeDirectedKick(input)) {
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new IdealDirectedHitStep(new KickAtEnemyGoal(), input));
        } else if (GetOnOffenseStep.getYAxisWrongSidedness(input) > 0) {
            BotLog.println("Getting behind the ball", input.team);
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new GetOnOffenseStep());
        } else if (car.boost < 30) {
            return new Plan().withStep(new GetBoostStep());
        } else {
            return new Plan(Plan.Posture.OFFENSIVE).withStep(new InterceptStep(new Vector3()));
        }
    }

    private TacticalSituation assessSituation(AgentInput input, BallPath ballPath) {

        Optional<SpaceTime> enemyIntercept = getEnemyIntercept(input, ballPath);

        TacticalSituation situation = new TacticalSituation();
        situation.expectedEnemyContact = enemyIntercept.orElse(ballPath.getEndpoint().toSpaceTime());
        situation.ownGoalFutureProximity = measureBallToOwnGoalDistanceInFuture(input);
        situation.distanceBallIsBehindUs = measureOutOfPosition(input);
        situation.enemyOffensiveApproachCorrection = measureEnemyApproachError(input, situation.expectedEnemyContact);

        return situation;
    }

    private double measureBallToOwnGoalDistanceInFuture(AgentInput input) {
        BallPath ballPath = ArenaModel.predictBallPath(input, input.time, Duration.ofSeconds(4));
        Goal myGoal = GoalUtil.getOwnGoal(input.team);
        return VectorUtil.flatDistance(myGoal.getCenter(), ballPath.getEndpoint().getSpace());
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
