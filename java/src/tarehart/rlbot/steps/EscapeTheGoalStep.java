package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SplineHandle;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.SteerUtil;

import java.util.Arrays;
import java.util.List;

public class EscapeTheGoalStep implements Step {
    private boolean isComplete = false;

    public AgentOutput getOutput(AgentInput input) {

        if (!ArenaModel.isBehindGoalLine(input.getMyPosition())) {
            isComplete = true;
        }

        Vector3 target = new Vector3(0, 0, 0);
        return SteerUtil.steerTowardPosition(input, target).withBoost(false);
    }

    private void init(AgentInput input) {
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
        return "Escaping the goal";
    }
}
