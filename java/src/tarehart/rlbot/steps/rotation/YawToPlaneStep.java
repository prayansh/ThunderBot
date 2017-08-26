package tarehart.rlbot.steps.rotation;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.BlindStep;

public class YawToPlaneStep extends OrientToPlaneStep {

    public YawToPlaneStep(Vector3 planeNormal) {
        super(planeNormal);
    }

    public YawToPlaneStep(Vector3 planeNormal, boolean allowUpsideDown) {
        super(planeNormal, allowUpsideDown);
    }

    @Override
    protected Plan makeOrientationPlan(CarRotation current) {

        double radians = getCorrectionRadians(current.noseVector, current.roofVector);

        if (!allowUpsideDown && current.sideVector.dotProduct(planeNormal) < 0) {
            radians += Math.PI;
        }
        radians = RotationUtil.shortWay(radians);

        System.out.println("Yawing " + radians);

        return new Plan()
                .withStep(new BlindStep(new AgentOutput().withSteer(Math.signum(radians)), RotationUtil.getStartingImpulse(radians)))
                .withStep(new BlindStep(new AgentOutput().withSteer(-Math.signum(radians)), RotationUtil.getHaltingImpulse(radians)));
    }
}
