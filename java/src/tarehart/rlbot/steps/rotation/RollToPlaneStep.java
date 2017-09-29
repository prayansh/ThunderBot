package tarehart.rlbot.steps.rotation;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.input.CarOrientation;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.BlindStep;
import tarehart.rlbot.tuning.BotLog;

public class RollToPlaneStep extends OrientToPlaneStep {

    public RollToPlaneStep(Vector3 planeNormal) {
        super(planeNormal);
    }

    public RollToPlaneStep(Vector3 planeNormal, boolean allowUpsideDown) {
        super(planeNormal, allowUpsideDown);
    }

    @Override
    protected double getCorrectionRadians(CarData car) {
        Vector3 vectorNeedingCorrection = car.orientation.rightVector;
        Vector3 axisOfRotation = car.orientation.noseVector;
        double radians = getCorrectionRadians(vectorNeedingCorrection, axisOfRotation);

        if (!allowUpsideDown && car.orientation.roofVector.dotProduct(planeNormal) < 0) {
            radians += Math.PI;
        }
        return RotationUtil.shortWay(radians);
    }

    @Override
    protected double getAngularVelocity(CarData car) {
        return car.spin.rollRate;
    }

    @Override
    protected AgentOutput accelerate(boolean positiveRadians) {
        return  new AgentOutput().withSteer(positiveRadians ? 1 : -1).withSlide();
    }

    @Override
    public String getSituation() {
        return "Rolling in midair";
    }
}
