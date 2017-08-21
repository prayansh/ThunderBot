package tarehart.rlbot.planning;

import com.sun.javafx.geom.Vec3f;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;

public class SteerUtil {

    public static AgentOutput steerTowardPosition(AgentInput input, Vector3 position) {

        Vector3 myPosition = input.getMyPosition();
        CarRotation myRotation = input.getMyRotation();

        float playerDirectionRad = (float) Math.atan2(myRotation.noseX, myRotation.noseY);

        float relativeAngleToBallRad = (float) Math.atan2(position.x - myPosition.x, position.y - myPosition.y);

        if (Math.abs(playerDirectionRad - relativeAngleToBallRad) > Math.PI) {
            if (playerDirectionRad < 0) {
                playerDirectionRad += Math.PI * 2;
            }
            if (relativeAngleToBallRad < 0) {
                relativeAngleToBallRad += Math.PI * 2;
            }
        }

        float turnSharpness = 1;
        float difference = Math.abs(playerDirectionRad - relativeAngleToBallRad);
        if (difference < Math.PI / 6) {
            turnSharpness = 0.5f;

            if (difference < Math.PI / 40) {
                turnSharpness = 0;
            }
        }

        return new AgentOutput()
                .withAcceleration(1)
                .withSteer(Math.signum(playerDirectionRad - relativeAngleToBallRad) * turnSharpness)
                .withSlide(difference > Math.PI / 2)
                .withBoost(difference < Math.PI / 6);
    }

    public static double getDistanceFromMe(AgentInput input, Vector3 loc) {
        return loc.distance(input.getMyPosition());
    }

}
