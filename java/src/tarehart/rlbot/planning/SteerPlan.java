package tarehart.rlbot.planning;

import mikera.vectorz.Vector2;
import tarehart.rlbot.AgentOutput;

public class SteerPlan {

    public AgentOutput immediateSteer;
    public Vector2 waypoint;

    public SteerPlan(AgentOutput immediateSteer, Vector2 waypoint) {
        this.immediateSteer = immediateSteer;
        this.waypoint = waypoint;
    }
}
