package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.planning.SteerUtil;

public class TurnTowardCenterStep implements Step {

    private boolean isComplete = false;


    public AgentOutput getOutput(AgentInput input) {

        Vector3 center = new Vector3(0, 0, 0);
        double correctionAngle = Math.abs(SteerUtil.getCorrectionAngleRad(input, center));

        if (correctionAngle < SteerUtil.GOOD_ENOUGH_ANGLE) {
            isComplete = true;
        }

        return SteerUtil.steerTowardPosition(input, center);
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
        return "Turning toward center field";
    }
}
