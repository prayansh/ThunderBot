package tarehart.rlbot.steps.rotation;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.BlindStep;
import tarehart.rlbot.tuning.BotLog;

public class PitchToPlaneStep extends OrientToPlaneStep {

    public PitchToPlaneStep(Vector3 planeNormal) {
        super(planeNormal);
    }

    public PitchToPlaneStep(Vector3 planeNormal, boolean allowUpsideDown) {
        super(planeNormal, allowUpsideDown);
    }

    @Override
    protected Plan makeOrientationPlan(CarRotation current, Bot.Team team) {

        Vector3 vectorNeedingCorrection = current.noseVector;
        Vector3 axisOfRotation = current.sideVector;

        double radians = getCorrectionRadians(vectorNeedingCorrection, axisOfRotation);

        if (!allowUpsideDown && current.roofVector.dotProduct(planeNormal) < 0) {
            radians += Math.PI;
        }
        radians = RotationUtil.shortWay(radians);

        BotLog.println("Pitching " + radians, team);

        return new Plan()
                .withStep(new BlindStep(new AgentOutput().withPitch(Math.signum(radians)), RotationUtil.getStartingImpulse(radians)))
                .withStep(new BlindStep(new AgentOutput().withPitch(-Math.signum(radians)), RotationUtil.getHaltingImpulse(radians)));
    }


    @Override
    public String getSituation() {
        return "Pitching in midair";
    }

}
