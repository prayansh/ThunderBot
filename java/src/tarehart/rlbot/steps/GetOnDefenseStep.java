package tarehart.rlbot.steps;

import com.sun.javafx.geom.Vec3f;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.planning.SteerUtil;

public class GetOnDefenseStep implements Step {
    private boolean isComplete = false;
    private Vector3 targetLocation = null;

    private static final float GOAL_DISTANCE = 97;

    public static final Vector3 BLUE_GOAL = new Vector3(0, GOAL_DISTANCE, 0);
    public static final Vector3 ORANGE_GOAL = new Vector3(0, GOAL_DISTANCE, 0);

    public AgentOutput getOutput(AgentInput input) {

        if (targetLocation == null) {
            init(input);
        }

        if (!needDefense(input) || SteerUtil.getDistanceFromMe(input, targetLocation) < 20) {
            isComplete = true;
            return new AgentOutput().withSlide(true);
        } else {
            return SteerUtil.steerTowardPosition(input, targetLocation);
        }
    }

    private void init(AgentInput input) {
        targetLocation = input.team == Bot.Team.BLUE ? BLUE_GOAL : ORANGE_GOAL;
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void begin() {
    }

    public static boolean needDefense(AgentInput input) {

        Vector3 myGoal = input.team == Bot.Team.BLUE ? GetOnDefenseStep.BLUE_GOAL : GetOnDefenseStep.ORANGE_GOAL;

        double relativeBallY = input.ballPosition.y - input.getMyPosition().y;
        double relativeGoalY = myGoal.y - input.getMyPosition().y;
        return relativeBallY * relativeGoalY > 0 && Math.abs(relativeGoalY) > 10;
    }
}
