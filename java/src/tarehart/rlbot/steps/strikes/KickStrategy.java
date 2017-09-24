package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.SpaceTimeVelocity;

public interface KickStrategy {
    Vector3 getKickDirection(AgentInput input);
    Vector3 getKickDirection(AgentInput input, Vector3 ballPosition);
}
