package tarehart.rlbot.steps;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.CarRotation;
import tarehart.rlbot.planning.Plan;

import java.time.Duration;

public class LandGracefullyStep implements Step {
    private boolean isComplete = false;
    private Plan plan = null;
    private boolean rollNext = false;

    public AgentOutput getOutput(AgentInput input) {

        if (input.getMyPosition().z < .35f) {
            isComplete = true;
            return new AgentOutput().withAcceleration(1);
        } else {

            if (plan != null && !plan.isComplete()) {
                return plan.getOutput(input);
            }

            CarRotation rot = input.getMyRotation();

            plan = new Plan();

            if (rollNext) {
                double rollDirection = Math.signum(rot.rollRightDesire);
                double rollNeed = Math.abs(rot.rollRightDesire);
                int rollDuration = (int) (rollNeed * 60);
                //System.out.println(String.format("Rolling %s for %sms.", rollDirection, rollDuration));
                plan.withStep(new BlindStep(new AgentOutput().withSlide(true).withSteer(-rollDirection), Duration.ofMillis(rollDuration)))
                        .withStep(new BlindStep(new AgentOutput().withSlide(true).withSteer(rollDirection).withAcceleration(1), Duration.ofMillis(rollDuration)));
                rollNext = false;
            } else {

                double pitchDirection = -Math.signum(rot.noseZ) * Math.signum(rot.roofZ);
                double pitchNeed = Math.abs(rot.noseZ);
                int pitchDuration = (int) (pitchNeed * 400);

                //System.out.println(String.format("Pitching %s for %sms.", pitchDirection, pitchDuration));
                plan.withStep(new BlindStep(new AgentOutput().withPitch(pitchDirection), Duration.ofMillis(pitchDuration)))
                        .withStep(new BlindStep(new AgentOutput().withPitch(-pitchDirection).withAcceleration(1), Duration.ofMillis(pitchDuration)));

                rollNext = true;
            }

            plan.begin();
            return plan.getOutput(input);
        }
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void begin() {
    }
}
