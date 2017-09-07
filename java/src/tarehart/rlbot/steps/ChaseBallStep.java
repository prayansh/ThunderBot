package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.strikes.AsapAerialStep;
import tarehart.rlbot.steps.strikes.FlipHitStep;
import tarehart.rlbot.steps.strikes.InterceptStep;
import tarehart.rlbot.steps.strikes.JumpHitStep;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.Optional;

public class ChaseBallStep implements Step {


    private boolean isComplete = false;
    private Plan plan;

    public AgentOutput getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        if (input.getMyPosition().z > 1 && !ArenaModel.isCarNearWall(input)) {
            isComplete = true;
            return new AgentOutput();
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
        AgentOutput output = interceptStep.getOutput(input);
        if (true) {
            plan = new Plan().withStep(interceptStep);
            plan.begin();
            return output;
        }

        isComplete = true;
        return new AgentOutput();
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void begin() {
    }

    @Override
    public String getSituation() {
        return "Chasing ball " + (plan != null && !plan.isComplete() ? "(" + plan.getSituation() + ")" : "");
    }
}
