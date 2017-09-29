package tarehart.rlbot.steps.landing;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.Bot;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.input.CarOrientation;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.rotation.PitchToPlaneStep;
import tarehart.rlbot.steps.rotation.RollToPlaneStep;
import tarehart.rlbot.steps.rotation.YawToPlaneStep;
import tarehart.rlbot.steps.wall.DescendFromWallStep;
import tarehart.rlbot.tuning.BotLog;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class LandGracefullyStep implements Step {
    private static final double SIN_45 = Math.sin(Math.PI / 4);
    public static final Vector3 UP_VECTOR = new Vector3(0, 0, 1);
    public static final int NEEDS_LANDING_HEIGHT = 1;
    private Plan plan = null;
    private Vector2 desiredFacing;

    public LandGracefullyStep() {
    }

    public LandGracefullyStep(Vector2 desiredFacing) {
        this.desiredFacing = desiredFacing;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        CarData car = input.getMyCarData();
        if (ArenaModel.isCarOnWall(car)) {
            plan = new Plan().withStep(new DescendFromWallStep());
            plan.begin();
            return plan.getOutput(input);
        }

        if (desiredFacing == null) {
            CarOrientation rot = car.orientation;
            desiredFacing = new Vector2(rot.noseVector.x, rot.noseVector.y);
            desiredFacing.normalise();
        }

        if (car.position.z < NEEDS_LANDING_HEIGHT || ArenaModel.isBehindGoalLine(car.position)) {
            return Optional.empty();
        }

        if (plan == null || plan.isComplete()) {
            plan = planRotation(car.orientation, desiredFacing, input.team);
            plan.begin();
        }

        return plan.getOutput(input);
    }

    private static Plan planRotation(CarOrientation current, Vector2 desiredFacing, Bot.Team team) {

        return new Plan()
                .withStep(Math.abs(current.roofVector.z) > SIN_45 ? new PitchToPlaneStep(UP_VECTOR, true) : new YawToPlaneStep(UP_VECTOR, true))
                .withStep(new RollToPlaneStep(UP_VECTOR))
                .withStep(new YawToPlaneStep(getFacingPlane(desiredFacing)));
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
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return "Landing gracefully " + (plan != null ? "(" + plan.getSituation() + ")" : "");
    }
}
