package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.planning.SteerUtil;

public class TurnTowardInterceptStep implements Step {

    private boolean isComplete = false;


    public AgentOutput getOutput(AgentInput input) {

        SpaceTime flatIntercept = SteerUtil.predictBallInterceptFlat(input);

        double correctionAngle = SteerUtil.getCorrectionAngleRad(input, new Vector3(flatIntercept.space.x, flatIntercept.space.y, 0));

        if (correctionAngle < SteerUtil.GOOD_ENOUGH_ANGLE) {
            isComplete = true;
        }

        return SteerUtil.steerTowardPosition(input, flatIntercept.space);
    }


    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void begin() {
    }
}
