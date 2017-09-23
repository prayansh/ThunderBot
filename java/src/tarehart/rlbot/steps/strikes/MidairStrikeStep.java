package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.TapStep;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class MidairStrikeStep implements Step {

    private static final double SIDE_DODGE_THRESHOLD = Math.PI / 8;
    public static final int DODGE_TIME = 400;
    public static final double DODGE_DISTANCE = 5;
    private static final Duration maxTimeForAirDodge = Duration.ofMillis(1500);
    private int confusionCount = 0;
    private Plan plan;
    private LocalDateTime lastMomentForDodge;
    private Duration timeInAirAtStart;

    public MidairStrikeStep(Duration timeInAirAtStart) {
        this.timeInAirAtStart = timeInAirAtStart;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null) {
            if (plan.isComplete()) {
                return Optional.empty();
            }
            return plan.getOutput(input);
        }

        if (lastMomentForDodge == null) {
            lastMomentForDodge = input.time.plus(maxTimeForAirDodge).minus(timeInAirAtStart);
        }

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(3));
        CarData car = input.getMyCarData();
        Optional<SpaceTime> interceptOpportunity = SteerUtil.getInterceptOpportunity(car, ballPath, car.velocity.magnitude());
        if (!interceptOpportunity.isPresent()) {
            confusionCount++;
            if (confusionCount > 3) {
                // Front flip out of confusion
                plan = new Plan().withStep(new TapStep(2, new AgentOutput().withPitch(-1).withJump()));
                plan.begin();
                return plan.getOutput(input);
            }
            return Optional.of(new AgentOutput().withBoost());
        }
        SpaceTime intercept = interceptOpportunity.get();
        Vector3 carToIntercept = (Vector3) intercept.space.subCopy(car.position);
        long millisTillIntercept = Duration.between(input.time, intercept.time).toMillis();
        double distance = car.position.distance(input.ballPosition);
        BotLog.println("Midair strike running... Distance: " + distance, input.team);

        double correctionAngleRad = SteerUtil.getCorrectionAngleRad(car, intercept.space);

        if (input.time.isBefore(lastMomentForDodge) && distance < DODGE_DISTANCE) {
            // Let's flip into the ball!
            if (Math.abs(correctionAngleRad) <= SIDE_DODGE_THRESHOLD) {
                BotLog.println("Front flip strike", input.team);
                plan = new Plan().withStep(new TapStep(2, new AgentOutput().withPitch(-1).withJump()));
                plan.begin();
                return plan.getOutput(input);
            } else {
                // Dodge to the side
                BotLog.println("Side flip strike", input.team);
                plan = new Plan().withStep(new TapStep(2, new AgentOutput().withSteer(correctionAngleRad < 0 ? 1 : -1).withJump()));
                plan.begin();
                return plan.getOutput(input);
            }
        }

        if (millisTillIntercept > DODGE_TIME && carToIntercept.normaliseCopy().dotProduct(car.velocity.normaliseCopy()) < .6) {
            BotLog.println("Failed aerial on bad angle", input.team);
            return Optional.empty();
        }

        Vector3 idealDirection = (Vector3) carToIntercept.normaliseCopy();
        Vector3 currentMotion = (Vector3) car.velocity.normaliseCopy();
        Vector3 currentPitch = car.orientation.noseVector;

        Vector2 sidescrollerCurrentVelocity = getPitchVector(currentMotion);
        Vector2 sidescrollerIdealVelocity = getPitchVector(idealDirection);
        Vector2 sidescrollerOrientation = getPitchVector(currentPitch);

        double currentVelocityAngle = SteerUtil.getCorrectionAngleRad(new Vector2(1, 0), sidescrollerCurrentVelocity);
        double idealVelocityAngle = SteerUtil.getCorrectionAngleRad(new Vector2(1, 0), sidescrollerIdealVelocity);
        double currentOrientation = SteerUtil.getCorrectionAngleRad(new Vector2(1, 0), sidescrollerOrientation);

        double desiredOrientation = idealVelocityAngle + Math.PI / 6 + (idealVelocityAngle - currentVelocityAngle) * .5;
        double orientationChange = desiredOrientation - currentOrientation;

        // TODO: midair steering!

        return Optional.of(new AgentOutput().withBoost().withPitch(orientationChange * 2));
    }

    /**
     * Pretend this is suddenly a 2D sidescroller where the car can't steer, it just boosts up and down.
     * Translate into that world.
     *
     * @param unitDirection normalized vector pointing in some direction
     * @return A unit vector in two dimensions, with positive x, and z equal to unitDirection z.
     */
    private Vector2 getPitchVector(Vector3 unitDirection) {
        return new Vector2(Math.sqrt(1 - unitDirection.z * unitDirection.z), unitDirection.z);
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
        return "Finishing aerial";
    }
}
