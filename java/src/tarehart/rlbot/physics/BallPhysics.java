package tarehart.rlbot.physics;

import tarehart.rlbot.AgentInput;

public class BallPhysics {
    public static double getGroundBounceEnergy(AgentInput input) {
        double potentialEnergy = (input.ballPosition.z - ArenaModel.BALL_RADIUS) * ArenaModel.GRAVITY;
        double verticalKineticEnergy = 0.5 * input.ballVelocity.z * input.ballVelocity.z;
        return potentialEnergy + verticalKineticEnergy;
    }
}
