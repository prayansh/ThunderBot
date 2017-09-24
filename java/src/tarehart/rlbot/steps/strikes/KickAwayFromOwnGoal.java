package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.SteerUtil;

public class KickAwayFromOwnGoal implements KickStrategy {


    @Override
    public Vector3 getKickDirection(AgentInput input) {
        return getDirection(input.getMyCarData(), input.ballPosition);
    }

    @Override
    public Vector3 getKickDirection(AgentInput input, Vector3 ballPosition) {
        return getDirection(input.getMyCarData(), ballPosition);
    }

    private Vector3 getDirection(CarData car, Vector3 ballPosition) {
        Vector2 easyKick = VectorUtil.flatten((Vector3) ballPosition.subCopy(car.position));
        Vector2 toLeftPost = VectorUtil.flatten((Vector3) GoalUtil.getOwnGoal(car.team).getLeftPost().subCopy(ballPosition));
        Vector2 toRightPost = VectorUtil.flatten((Vector3) GoalUtil.getOwnGoal(car.team).getRightPost().subCopy(ballPosition));

        Vector2 safeDirectionRight = VectorUtil.rotateVector(toRightPost, -Math.PI/6);
        Vector2 safeDirectionLeft = VectorUtil.rotateVector(toLeftPost, Math.PI/6);

        double safeRightCorrection = SteerUtil.getCorrectionAngleRad(easyKick, safeDirectionRight);
        double safeLeftCorrection = SteerUtil.getCorrectionAngleRad(easyKick, safeDirectionLeft);
        if (safeRightCorrection > 0 || safeLeftCorrection < 0) {
            // The easy kick is already wide. Go with the easy kick.
            return new Vector3(easyKick.x, easyKick.y, 0);
        } else if (Math.abs(safeRightCorrection) < Math.abs(safeLeftCorrection)) {
            return new Vector3(safeDirectionRight.x, safeDirectionRight.y, 0);
        } else {
            return new Vector3(safeDirectionLeft.x, safeDirectionLeft.y, 0);
        }
    }
}
