package tarehart.rlbot.steps;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.rotation.PitchToPlaneStep;
import tarehart.rlbot.steps.rotation.RollToPlaneStep;
import tarehart.rlbot.steps.rotation.YawToPlaneStep;

import java.util.Comparator;
import java.util.stream.Stream;

public class LandGracefullyStep implements Step {
    private static final double SIN_45 = Math.sin(Math.PI / 4);
    public static final Vector3 UP_VECTOR = new Vector3(0, 0, 1);
    private boolean isComplete = false;
    private Plan plan = null;
    private Vector2 desiredFacing;

    public LandGracefullyStep() {
    }

    public LandGracefullyStep(Vector2 desiredFacing) {
        this.desiredFacing = desiredFacing;
    }

    public AgentOutput getOutput(AgentInput input) {

        if (desiredFacing == null) {
            CarRotation rot = input.getMyRotation();
            desiredFacing = new Vector2(rot.noseVector.x, rot.noseVector.y);
            desiredFacing.normalise();
        }

        if (input.getMyPosition().z < .40f) {
            isComplete = true;
            return new AgentOutput().withAcceleration(1);
        }

        if (plan == null) {
            plan = planRotation(input.getMyRotation(), desiredFacing);
            plan.begin();
        }

        if (plan.isComplete()) {
            return new AgentOutput().withAcceleration(1);
        }

        return plan.getOutput(input);
    }

    private static Plan planRotation(CarRotation current, Vector2 desiredFacing) {

        // Step 1: get a clean axis
        // If front has no Z, we can roll flat
        // If roof has no Z, we can pitch nose toward desired direction, then roll
        // If side has no Z, we can pitch flat, in direction closest to desired, then roll

        System.out.println("Nose: " + current.noseVector + " Roof: " + current.roofVector + " Side: " + current.sideVector);

        // What's closest to being true?
        Vector3 minZ = Stream.of(current.noseVector, current.roofVector, current.sideVector).min(Comparator.comparingDouble(a -> Math.abs(a.z))).get();

        if (minZ == current.noseVector) {
            System.out.println("Nose points to horizon");
            // Pitch or yaw nose vector to zero
            return new Plan()
                    .withStep(Math.abs(current.roofVector.z) > SIN_45 ? new PitchToPlaneStep(UP_VECTOR, true) : new YawToPlaneStep(UP_VECTOR, true))
                    .withStep(new RollToPlaneStep(UP_VECTOR))
                    .withStep(new YawToPlaneStep(getFacingPlane(desiredFacing)));

        } else if (minZ == current.roofVector) {
            System.out.println("Roof points to horizon");
            // Roll or pitch roof vector to zero
            return new Plan()
                    .withStep(new RollToPlaneStep(UP_VECTOR, true))
                    .withStep(new PitchToPlaneStep(getFacingPlane(desiredFacing)))
                    .withStep(new RollToPlaneStep(UP_VECTOR));

        } else {
            System.out.println("Side points to horizon");
            // Roll or yaw side vector to zero
            return new Plan()
                    .withStep(new RollToPlaneStep(UP_VECTOR, true))
                    .withStep(new PitchToPlaneStep(UP_VECTOR))
                    .withStep(new YawToPlaneStep(getFacingPlane(desiredFacing)));

        }
    }

    private static Vector3 getFacingPlane(Vector2 desiredFacing) {
        return new Vector3(-desiredFacing.y, -desiredFacing.x, 0);
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void begin() {
    }
}
