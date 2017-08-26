package tarehart.rlbot.steps.rotation;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.Step;

public abstract class OrientToPlaneStep implements Step {

    private Plan plan;
    protected Vector3 planeNormal;
    protected boolean allowUpsideDown;

    public OrientToPlaneStep(Vector3 planeNormal) {
        this(planeNormal, false);
    }

    public OrientToPlaneStep(Vector3 planeNormal, boolean allowUpsideDown) {
        this.planeNormal = planeNormal;
        this.allowUpsideDown = allowUpsideDown;
    }

    protected abstract Plan makeOrientationPlan(CarRotation current);

    protected double getCorrectionRadians(Vector3 vectorNeedingCorrection, Vector3 axisOfRotation) {
        Vector3 planeError = VectorUtil.project(vectorNeedingCorrection, planeNormal);
        return -Math.asin(planeError.magnitude() * Math.signum(planeError.dotProduct(planeNormal)) / RotationUtil.inclination(axisOfRotation, planeNormal));
    }

    @Override
    public AgentOutput getOutput(AgentInput input) {

        if (plan == null) {
            plan = makeOrientationPlan(input.getMyRotation());
            plan.begin();
        }

        return plan.getOutput(input);
    }

    @Override
    public boolean isComplete() {
        return plan != null && plan.isComplete();
    }

    @Override
    public void begin() {

    }
}
