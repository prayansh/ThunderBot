package tarehart.rlbot.steps.landing;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.rotation.PitchToPlaneStep;
import tarehart.rlbot.steps.rotation.RollToPlaneStep;
import tarehart.rlbot.steps.rotation.YawToPlaneStep;
import tarehart.rlbot.tuning.BotLog;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class LandGracefullyStep implements Step {
    private static final double SIN_45 = Math.sin(Math.PI / 4);
    public static final Vector3 UP_VECTOR = new Vector3(0, 0, 1);
    private Plan plan = null;
    private Vector2 desiredFacing;

    public LandGracefullyStep() {
    }

    public LandGracefullyStep(Vector2 desiredFacing) {
        this.desiredFacing = desiredFacing;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (desiredFacing == null) {
            CarRotation rot = input.getMyRotation();
            desiredFacing = new Vector2(rot.noseVector.x, rot.noseVector.y);
            desiredFacing.normalise();
        }

        if (input.getMyPosition().z < .40f || isOnWall(input)) {
            return Optional.empty();
        }

        if (plan == null || plan.isComplete()) {
            plan = planRotation(input.getMyRotation(), desiredFacing, input.team);
            plan.begin();
        }

        return plan.getOutput(input);
    }

    public static boolean isOnWall(AgentInput input) {
        return ArenaModel.isCarNearWall(input) && Math.abs(input.getMyRotation().roofVector.z) < 0.02;
    }

    private static Plan planRotation(CarRotation current, Vector2 desiredFacing, Bot.Team team) {

        // Step 1: get a clean axis
        // If front has no Z, we can roll flat
        // If roof has no Z, we can pitch nose toward desired direction, then roll
        // If side has no Z, we can pitch flat, in direction closest to desired, then roll

        BotLog.println("Nose: " + current.noseVector + " Roof: " + current.roofVector + " Side: " + current.sideVector, team);

        // What's closest to being true?
        Vector3 minZ = Stream.of(current.noseVector, current.roofVector, current.sideVector).min(Comparator.comparingDouble(a -> Math.abs(a.z))).get();

        if (minZ == current.noseVector) {
            BotLog.println("Nose points to horizon", team);
            // Pitch or yaw nose vector to zero
            return new Plan()
                    .withStep(Math.abs(current.roofVector.z) > SIN_45 ? new PitchToPlaneStep(UP_VECTOR, true) : new YawToPlaneStep(UP_VECTOR, true))
                    .withStep(new RollToPlaneStep(UP_VECTOR))
                    .withStep(new YawToPlaneStep(getFacingPlane(desiredFacing)));

        } else if (minZ == current.roofVector) {
            BotLog.println("Roof points to horizon", team);
            // Roll or pitch roof vector to zero
            return new Plan()
                    .withStep(new RollToPlaneStep(UP_VECTOR, true))
                    .withStep(new PitchToPlaneStep(getFacingPlane(desiredFacing)))
                    .withStep(new RollToPlaneStep(UP_VECTOR));

        } else {
            BotLog.println("Side points to horizon", team);
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
    public boolean isBlindlyComplete() {
        return false;
    }

    @Override
    public void begin() {
    }

    @Override
    public String getSituation() {
        return "Landing gracefully " + (plan != null ? "(" + plan.getSituation() + ")" : "");
    }
}
