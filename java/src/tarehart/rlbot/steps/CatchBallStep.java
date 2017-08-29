package tarehart.rlbot.steps;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.planning.SteerUtil;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class CatchBallStep implements Step {

    private boolean isComplete = false;
    private int confusionLevel = 0;
    private SpaceTime latestCatchLocation;
    private boolean firstFrame = true;

    public CatchBallStep(SpaceTime initialCatchLocation) {
        latestCatchLocation = initialCatchLocation;
    }

    public AgentOutput getOutput(AgentInput input) {

        if (firstFrame) {
            firstFrame = false;
            return playCatch(input, latestCatchLocation);
        }

        double distance = input.getMyPosition().distance(input.ballPosition);

        if (distance < 2.5 || confusionLevel > 3) {
            isComplete = true;
            // We'll still get one last frame out output though
        }

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(3));
        Optional<SpaceTime> catchOpportunity = SteerUtil.getCatchOpportunity(input, ballPath);

        // Weed out any intercepts after a catch opportunity. Should just catch it.
        if (catchOpportunity.isPresent()) {
            latestCatchLocation = catchOpportunity.get();
            confusionLevel = 0;
        } else {
            confusionLevel++;
        }

        return playCatch(input, latestCatchLocation);
    }

    private AgentOutput playCatch(AgentInput input, SpaceTime catchLocation) {
        Vector3 enemyGoal = GoalUtil.getEnemyGoal(input.team).getLocation();
        Vector3 awayFromEnemyGoal = (Vector3) catchLocation.space.subCopy(enemyGoal);
        awayFromEnemyGoal.z = 0;
        awayFromEnemyGoal.normalise();
        awayFromEnemyGoal.scale(.85);
        Vector3 target = catchLocation.space.addCopy(awayFromEnemyGoal);

        return getThereOnTime(input, new SpaceTime(target, catchLocation.time));
    }

    private AgentOutput getThereOnTime(AgentInput input, SpaceTime groundPositionAndTime) {
        double flatDistance = flatten(input.getMyPosition()).distance(flatten(groundPositionAndTime.space));

        double secondsTillAppointment = Duration.between(input.time, groundPositionAndTime.time).toMillis() / 1000.0;
        double speed = input.getMyVelocity().magnitude();

        double pace = speed * secondsTillAppointment / flatDistance; // Ideally this should be 1

        if (flatDistance > 50) {
            // Go fast
            return SteerUtil.steerTowardPosition(input, groundPositionAndTime.space);
        } else if (pace < 1) {
            // Go moderate
            return SteerUtil.steerTowardPosition(input, groundPositionAndTime.space).withBoost(false);
        } else {
            // We're going too fast!
            AgentOutput agentOutput = SteerUtil.steerTowardPosition(input, groundPositionAndTime.space);
            agentOutput.withAcceleration(0).withBoost(false).withDeceleration(Math.max(0, pace - 1.3)); // Hit the brakes, but keep steering!
            return agentOutput;
        }
    }

    private Vector2 flatten(Vector3 vector3) {
        return new Vector2(vector3.x, vector3.y);
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
        return "Catching ball";
    }
}
