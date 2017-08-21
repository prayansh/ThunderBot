package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.planning.SteerUtil;

import java.util.Arrays;
import java.util.List;

public class GetBoostStep implements Step {
    private boolean isComplete = false;
    private Vector3 targetLocation = null;

    private static final float MIDFIELD_BOOST_WIDTH = 72.5f;
    private static final float CORNER_BOOST_WIDTH = 64;
    private static final float CORNER_BOOST_DEPTH = 81;

    private static final List<Vector3> boostLocations = Arrays.asList(
            new Vector3(MIDFIELD_BOOST_WIDTH, 0, 0),
            new Vector3(-MIDFIELD_BOOST_WIDTH, 0, 0),
            new Vector3(-CORNER_BOOST_WIDTH, -CORNER_BOOST_DEPTH, 0),
            new Vector3(-CORNER_BOOST_WIDTH, CORNER_BOOST_DEPTH, 0),
            new Vector3(CORNER_BOOST_WIDTH, -CORNER_BOOST_DEPTH, 0),
            new Vector3(CORNER_BOOST_WIDTH, CORNER_BOOST_DEPTH, 0)
    );

    public AgentOutput getOutput(AgentInput input) {

        if (targetLocation == null) {
            init(input);
        }

        if (input.getMyBoost() > 95) {
            isComplete = true;
            return new AgentOutput().withAcceleration(1);
        } else {
            return SteerUtil.steerTowardPosition(input, targetLocation);
        }
    }

    private void init(AgentInput input) {
        double minDistance = Double.MAX_VALUE;
        for (Vector3 loc: boostLocations) {
            double distance = SteerUtil.getDistanceFromMe(input, loc);
            if (distance < minDistance) {
                minDistance = distance;
                targetLocation = loc;
            }
        }
    }



    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void begin() {
    }
}
