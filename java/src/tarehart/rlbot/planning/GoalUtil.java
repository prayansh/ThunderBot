package tarehart.rlbot.planning;

import mikera.vectorz.Vector3;
import tarehart.rlbot.Bot;
import tarehart.rlbot.math.SplineHandle;

public class GoalUtil {

    public static final Goal BLUE_GOAL = new Goal(true);
    public static final Goal ORANGE_GOAL = new Goal(false);

    public static Goal getOwnGoal(Bot.Team team) {
        return team == Bot.Team.BLUE ? BLUE_GOAL : ORANGE_GOAL;
    }

    public static Goal getEnemyGoal(Bot.Team team) {
        return team == Bot.Team.BLUE ? ORANGE_GOAL : BLUE_GOAL;
    }
}
