package tarehart.rlbot.planning;

import tarehart.rlbot.Bot;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.physics.BallPath;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class GoalUtil {

    public static final Goal BLUE_GOAL = new Goal(true);
    public static final Goal ORANGE_GOAL = new Goal(false);

    public static Goal getOwnGoal(Bot.Team team) {
        return team == Bot.Team.BLUE ? BLUE_GOAL : ORANGE_GOAL;
    }

    public static Goal getEnemyGoal(Bot.Team team) {
        return team == Bot.Team.BLUE ? ORANGE_GOAL : BLUE_GOAL;
    }

    public static Optional<SpaceTimeVelocity> predictGoalEvent(Goal goal, BallPath ballPath) {
        return ballPath.getPlaneBreak(ballPath.getStartPoint().time, goal.getScorePlane(), true);
    }

    public static boolean ballEntersBox(Goal goal, BallPath ballPath, Duration limit) {
        LocalDateTime lastMoment = ballPath.getStartPoint().getTime().plus(limit);
        return ballPath.findSlice(slice -> goal.isInBox(slice.getSpace()), lastMoment).isPresent();
    }
}
