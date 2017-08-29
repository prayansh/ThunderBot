package tarehart.rlbot.steps.rotation;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.BlindStep;
import tarehart.rlbot.tuning.BotLog;

public class YawToPlaneStep extends OrientToPlaneStep {

    public YawToPlaneStep(Vector3 planeNormal) {
        super(planeNormal);
    }

    public YawToPlaneStep(Vector3 planeNormal, boolean allowUpsideDown) {
        super(planeNormal, allowUpsideDown);
    }

    @Override
    protected Plan makeOrientationPlan(CarRotation current, Bot.Team team) {

        double radians = getCorrectionRadians(current.noseVector, current.roofVector);

        if (!allowUpsideDown && current.sideVector.dotProduct(planeNormal) < 0) {
            radians += Math.PI;
        }
        radians = RotationUtil.shortWay(radians);

        BotLog.println("Yawing " + radians, team);

        return new Plan()
                .withStep(new BlindStep(new AgentOutput().withSteer(Math.signum(radians)), RotationUtil.getStartingImpulse(radians)))
                .withStep(new BlindStep(new AgentOutput().withSteer(-Math.signum(radians)), RotationUtil.getHaltingImpulse(radians)));
    }

    @Override
    public String getSituation() {
        return "Yawing in midair";
    }
}
