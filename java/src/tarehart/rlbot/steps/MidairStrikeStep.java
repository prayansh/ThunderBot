package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.List;

public class MidairStrikeStep implements Step {

    private static final double SIDE_DODGE_THRESHOLD = Math.PI / 8;
    public static final int DODGE_TIME = 250;
    public static final double DODGE_DISTANCE = 4;
    private boolean isComplete = false;
    private int confusionCount = 0;

    public AgentOutput getOutput(AgentInput input) {

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(3));
        List<SpaceTime> interceptOpportunities = SteerUtil.getInterceptOpportunities(input, ballPath, input.getMyVelocity().magnitude());
        if (interceptOpportunities.isEmpty()) {
            confusionCount++;
            if (confusionCount > 3) {
                // Front flip out of confusion
                return new AgentOutput().withPitch(-1).withJump();
            }
            return new AgentOutput().withBoost();
        }
        SpaceTime intercept = interceptOpportunities.get(0);
        Vector3 carToIntercept = (Vector3) intercept.space.subCopy(input.getMyPosition());
        long millisTillIntercept = Duration.between(input.time, intercept.time).toMillis();
        double distance = input.getMyPosition().distance(input.ballPosition);
        BotLog.println("Midair strike running... Distance: " + distance, input.team);

        if (millisTillIntercept > DODGE_TIME && carToIntercept.normaliseCopy().dotProduct(input.getMyVelocity().normaliseCopy()) < .6) {
            BotLog.println("Failed aerial on bad angle", input.team);
            isComplete = true;
            return new AgentOutput();
        }

        double correctionAngleRad = SteerUtil.getCorrectionAngleRad(input, intercept.space);

        if (millisTillIntercept < DODGE_TIME || distance < DODGE_DISTANCE) {
            // Let's flip into the ball!
            if (Math.abs(correctionAngleRad) <= SIDE_DODGE_THRESHOLD) {
                BotLog.println("Front flip strike", input.team);
                isComplete = true;
                return new AgentOutput().withPitch(-1).withJump();
            } else {
                // Dodge right
                BotLog.println("Side flip strike", input.team);
                isComplete = true;
                return new AgentOutput().withSteer(correctionAngleRad < 0 ? 1 : -1).withJump();
            }
        }

        // TODO: midair steering!

        return new AgentOutput().withBoost();
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void begin() {
    }

    @Override
    public String getSituation() {
        return "Finishing aerial";
    }
}
