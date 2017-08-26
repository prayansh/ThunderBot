package tarehart.rlbot.steps.rotation;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.BlindStep;

public class PitchToPlaneStep extends OrientToPlaneStep {

    public PitchToPlaneStep(Vector3 planeNormal) {
        super(planeNormal);
    }

    public PitchToPlaneStep(Vector3 planeNormal, boolean allowUpsideDown) {
        super(planeNormal, allowUpsideDown);
    }

    @Override
    protected Plan makeOrientationPlan(CarRotation current) {

        Vector3 vectorNeedingCorrection = current.noseVector;
        Vector3 axisOfRotation = current.sideVector;

        double radians = getCorrectionRadians(vectorNeedingCorrection, axisOfRotation);

        if (!allowUpsideDown && current.roofVector.dotProduct(planeNormal) < 0) {
            radians += Math.PI;
        }
        radians = RotationUtil.shortWay(radians);

        System.out.println("Pitching " + radians);

        return new Plan()
                .withStep(new BlindStep(new AgentOutput().withPitch(Math.signum(radians)), RotationUtil.getStartingImpulse(radians)))
                .withStep(new BlindStep(new AgentOutput().withPitch(-Math.signum(radians)), RotationUtil.getHaltingImpulse(radians)));
    }


}
