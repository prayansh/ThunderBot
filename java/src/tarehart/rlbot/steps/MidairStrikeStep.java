package tarehart.rlbot.steps;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class MidairStrikeStep implements Step {

    private static final double SIDE_DODGE_THRESHOLD = Math.PI / 12;
    public static final int DODGE_TIME = 250;
    private boolean isComplete = false;

    public AgentOutput getOutput(AgentInput input) {

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(3));
        List<SpaceTime> interceptOpportunities = SteerUtil.getInterceptOpportunities(input, ballPath);
        SpaceTime intercept = interceptOpportunities.get(0);
        Vector3 carToIntercept = (Vector3) intercept.space.subCopy(input.getMyPosition());
        long millisTillIntercept = Duration.between(input.time, intercept.time).toMillis();

        if (millisTillIntercept > DODGE_TIME && interceptOpportunities.isEmpty()) {
            BotLog.println("Failed aerial on missing intercept", input.team);
            isComplete = true;
            return new AgentOutput();
        }

        if (millisTillIntercept > DODGE_TIME && carToIntercept.normaliseCopy().dotProduct(input.getMyVelocity().normaliseCopy()) < .6) {
            BotLog.println("Failed aerial on bad angle", input.team);
            isComplete = true;
            return new AgentOutput();
        }

        double correctionAngleRad = SteerUtil.getCorrectionAngleRad(input, intercept.space);

        if (millisTillIntercept < DODGE_TIME) {
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
