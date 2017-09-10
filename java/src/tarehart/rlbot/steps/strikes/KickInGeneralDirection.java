package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.planning.Goal;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.SteerUtil;

public class KickInGeneralDirection implements KickStrategy {

    private Vector2 direction;
    private double toleranceInRadians;

    public KickInGeneralDirection(Vector2 direction, double toleranceInRadians) {
        this.direction = direction;
        this.toleranceInRadians = toleranceInRadians;
    }

    @Override
    public Vector3 getKickDirection(AgentInput input) {
        return getDirection(input, input.ballPosition);
    }

    @Override
    public Vector3 getKickDirection(AgentInput input, SpaceTimeVelocity ballIntercept) {
        return getDirection(input, ballIntercept.space);
    }

    private Vector3 getDirection(AgentInput input, Vector3 ballPosition) {
        Vector2 easyKick = VectorUtil.flatten((Vector3) ballPosition.subCopy(input.getMyPosition()));

        double correctionRad = SteerUtil.getCorrectionAngleRad(easyKick, direction);
        if (Math.abs(correctionRad) < toleranceInRadians) {
            return new Vector3(easyKick.x, easyKick.y, 0);
        }

        double excess = correctionRad - Math.signum(correctionRad) * toleranceInRadians;
        Vector2 fixed = VectorUtil.rotateVector(easyKick, excess);
        return new Vector3(fixed.x, fixed.y, 0);
    }
}
