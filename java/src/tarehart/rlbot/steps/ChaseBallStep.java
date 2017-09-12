package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.AirTouchPlanner;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.strikes.InterceptStep;

import java.time.Duration;
import java.util.Optional;

public class ChaseBallStep implements Step {

    private Plan plan;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        if (input.getMyPosition().z > 1 && !ArenaModel.isCarNearWall(input)) {
            return Optional.empty();
        }


        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(3));

        if (input.getEnemyPosition().distance(input.ballPosition) > 50) {
            if (input.getMyBoost() < 10 && GetBoostStep.seesOpportunisticBoost(input)) {
                plan = new Plan().withStep(new GetBoostStep());
                plan.begin();
                return plan.getOutput(input);
            }

            Optional<SpaceTime> catchOpportunity = SteerUtil.getCatchOpportunity(input, ballPath, AirTouchPlanner.getBoostBudget(input));
            if (catchOpportunity.isPresent()) {
                plan = new Plan().withStep(new CatchBallStep(catchOpportunity.get())).withStep(new DribbleStep());
                plan.begin();
                return plan.getOutput(input);
            }
        }

        InterceptStep interceptStep = new InterceptStep(new Vector3());
        Optional<AgentOutput> output = interceptStep.getOutput(input);
        if (output.isPresent()) {
            plan = new Plan().withStep(interceptStep);
            plan.begin();
            return output;
        }

        return Optional.of(SteerUtil.steerTowardGroundPosition(input, input.ballPosition));
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
        return Plan.concatSituation("Chasing ball", plan);
    }
}
