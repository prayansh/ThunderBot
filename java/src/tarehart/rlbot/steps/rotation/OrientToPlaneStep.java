package tarehart.rlbot.steps.rotation;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarOrientation;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.Step;

import java.util.Optional;

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

    protected abstract Plan makeOrientationPlan(CarOrientation current, Bot.Team team);

    protected double getCorrectionRadians(Vector3 vectorNeedingCorrection, Vector3 axisOfRotation) {
        // We want vectorNeedingCorrection to be resting on the plane. If it's lined up with the planeNormal, then it's
        // doing a very poor job of that.
        Vector3 planeError = VectorUtil.project(vectorNeedingCorrection, planeNormal);

        double distanceAbovePlane = planeError.magnitude() * Math.signum(planeError.dotProduct(planeNormal));

        double maxOrbitHeightAbovePlane = RotationUtil.maxOrbitHeightAbovePlane(axisOfRotation, planeNormal);
        return -Math.asin(distanceAbovePlane / maxOrbitHeightAbovePlane);
    }

    @Override
    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan == null) {
            plan = makeOrientationPlan(input.getMyCarData().orientation, input.team);
            plan.begin();
        }

        return plan.getOutput(input);
    }

    @Override
    public boolean isBlindlyComplete() {
        return plan != null && plan.isComplete();
    }

    @Override
    public void begin() {

    }
}
