package tarehart.rlbot.steps.rotation;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.input.CarOrientation;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.Step;

import java.util.Optional;



public abstract class OrientToPlaneStep implements Step {

    public static final double SPIN_DECELERATION = 10; // Radians per second per second

    protected Vector3 planeNormal;
    protected boolean allowUpsideDown;
    protected boolean timeToDecelerate;
    private Double originalCorrection = null;

    public OrientToPlaneStep(Vector3 planeNormal) {
        this(planeNormal, false);
    }

    public OrientToPlaneStep(Vector3 planeNormal, boolean allowUpsideDown) {
        this.planeNormal = planeNormal;
        this.allowUpsideDown = allowUpsideDown;
    }

    private double getRadiansSpentDecelerating(double angularVelocity) {
        double timeDecelerating = Math.abs(angularVelocity) / SPIN_DECELERATION;
        return angularVelocity * timeDecelerating - .5 * SPIN_DECELERATION * timeDecelerating * timeDecelerating;
    }

    /**
     * This does not consider direction. You should only call it if you are already rotating toward your target.
     */
    protected boolean timeToDecelerate(double angularVelocity, double radiansRemaining) {
        return getRadiansSpentDecelerating(angularVelocity) >= Math.abs(radiansRemaining);
    }


    protected double getCorrectionRadians(Vector3 vectorNeedingCorrection, Vector3 axisOfRotation) {
        // We want vectorNeedingCorrection to be resting on the plane. If it's lined up with the planeNormal, then it's
        // doing a very poor job of that.
        Vector3 planeError = VectorUtil.project(vectorNeedingCorrection, planeNormal);

        double distanceAbovePlane = planeError.magnitude() * Math.signum(planeError.dotProduct(planeNormal));

        double maxOrbitHeightAbovePlane = RotationUtil.maxOrbitHeightAbovePlane(axisOfRotation, planeNormal);
        return -Math.asin(distanceAbovePlane / maxOrbitHeightAbovePlane);
    }

    protected abstract double getCorrectionRadians(CarData car);
    protected abstract double getAngularVelocity(CarData car);
    protected abstract AgentOutput accelerate(boolean positiveRadians);

    private AgentOutput accelerateTowardPlane(CarData car) {

        double radians = getCorrectionRadians(car);

        double angularVelocity = getAngularVelocity(car);

        if (angularVelocity * radians < 0) {
            // We're trending toward the plane, that's good.
            if (timeToDecelerate(angularVelocity, radians)) {
                timeToDecelerate = true;
            }
        }

        return accelerate(radians > 0);
    }

    @Override
    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();

        if (originalCorrection == null) {
            originalCorrection = getCorrectionRadians(car);
        }

        AgentOutput output = null;
        if (!timeToDecelerate) {
            output = accelerateTowardPlane(input.getMyCarData());
        }

        // The value of timeToDecelerate can get changed by accelerateTowardPlane.
        if (timeToDecelerate) {
            if (getAngularVelocity(car) * originalCorrection < 0) {
                // We're done decelerating
                return Optional.empty();
            }

            output = accelerate(originalCorrection < 0);
        }

        return Optional.ofNullable(output);
    }

    @Override
    public boolean isBlindlyComplete() {
        return false;
    }

    @Override
    public void begin() {
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }
}
