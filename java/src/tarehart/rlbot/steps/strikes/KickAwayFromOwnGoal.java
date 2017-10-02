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
        return getKickDirection(input, input.ballPosition);
    }

    @Override
    public Vector3 getKickDirection(AgentInput input, Vector3 ballPosition) {
        CarData car = input.getMyCarData();
        Vector3 toBall = (Vector3) ballPosition.subCopy(car.position);
        return getDirection(input.getMyCarData(), ballPosition, toBall);
    }

    @Override
    public Vector3 getKickDirection(AgentInput input, Vector3 ballPosition, Vector3 easyKick) {
        return getDirection(input.getMyCarData(), ballPosition, easyKick);
    }

    private Vector3 getDirection(CarData car, Vector3 ballPosition, Vector3 easyKick) {
        Vector2 easyKickFlat = VectorUtil.flatten(easyKick);
        Vector2 toLeftPost = VectorUtil.flatten((Vector3) GoalUtil.getOwnGoal(car.team).getLeftPost().subCopy(ballPosition));
        Vector2 toRightPost = VectorUtil.flatten((Vector3) GoalUtil.getOwnGoal(car.team).getRightPost().subCopy(ballPosition));

        Vector2 safeDirectionRight = VectorUtil.rotateVector(toRightPost, -Math.PI/4);
        Vector2 safeDirectionLeft = VectorUtil.rotateVector(toLeftPost, Math.PI/4);

        double safeRightCorrection = SteerUtil.getCorrectionAngleRad(easyKickFlat, safeDirectionRight);
        double safeLeftCorrection = SteerUtil.getCorrectionAngleRad(easyKickFlat, safeDirectionLeft);
        if (safeRightCorrection > 0 || safeLeftCorrection < 0) {
            // The easy kick is already wide. Go with the easy kick.
            return new Vector3(easyKickFlat.x, easyKickFlat.y, 0);
        } else if (Math.abs(safeRightCorrection) < Math.abs(safeLeftCorrection)) {
            return new Vector3(safeDirectionRight.x, safeDirectionRight.y, 0);
        } else {
            return new Vector3(safeDirectionLeft.x, safeDirectionLeft.y, 0);
        }
    }
}
