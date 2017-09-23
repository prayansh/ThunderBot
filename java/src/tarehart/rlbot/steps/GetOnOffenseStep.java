package tarehart.rlbot.steps;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.defense.GetOnDefenseStep;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.Optional;

public class GetOnOffenseStep implements Step {

    private Plan plan;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        Goal enemyGoal = GoalUtil.getEnemyGoal(input.team);
        Goal ownGoal = GoalUtil.getOwnGoal(input.team);

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(2));

        Vector3 target = input.ballPosition.copy();
        SpaceTimeVelocity futureMotion = ballPath.getMotionAt(input.time.plusSeconds(2)).get();
        if (input.ballVelocity.y * (enemyGoal.getCenter().y - input.ballPosition.y) < 0) {
            // if ball is rolling away from the enemy goal
            target = futureMotion.getSpace().clone();
        }

        if (futureMotion.getSpace().distance(enemyGoal.getCenter())  < ArenaModel.SIDE_WALL * .8) {
            // Get into a strike position, 10 units behind the ball
            Vector3 goalToBall = (Vector3) target.subCopy(enemyGoal.getCenter());
            Vector3 goalToBallNormal = (Vector3) goalToBall.normaliseCopy();
            target.add(goalToBallNormal.scaleCopy(10));

        } else {
            // Get into a backstop position
            Vector3 goalToBall = (Vector3) target.subCopy(ownGoal.getCenter());
            Vector3 goalToBallNormal = (Vector3) goalToBall.normaliseCopy();
            target.sub(goalToBallNormal.scaleCopy(10));
        }

        CarData car = input.getMyCarData();

        double flatDistance = VectorUtil.flatDistance(target, car.position);
        if (flatDistance < 20 && GetOnDefenseStep.getWrongSidedness(input) < 0) {
            return Optional.empty();
        }
        Vector3 targetToBallFuture = (Vector3) futureMotion.getSpace().subCopy(target);

        DistancePlot plot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(4), 0);


        Optional<Vector2> circleTurnOption = SteerUtil.getWaypointForCircleTurn(car, plot, VectorUtil.flatten(target), (Vector2) VectorUtil.flatten(targetToBallFuture).normaliseCopy());

        if (circleTurnOption.isPresent()) {
            Vector2 circleTurn = circleTurnOption.get();
            Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, circleTurn);
            if (sensibleFlip.isPresent()) {
                BotLog.println("Front flip onto offense", input.team);
                this.plan = sensibleFlip.get();
                this.plan.begin();
                return this.plan.getOutput(input);
            }

            return Optional.of(SteerUtil.steerTowardGroundPosition(car, circleTurn).withBoost(false));
        }

        return Optional.of(SteerUtil.steerTowardGroundPosition(car, target).withBoost(false));
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
        return "Getting on offense";
    }
}
