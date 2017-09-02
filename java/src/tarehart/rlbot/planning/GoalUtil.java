package tarehart.rlbot.planning;

import mikera.vectorz.Vector3;
import tarehart.rlbot.Bot;
import tarehart.rlbot.math.SplineHandle;

public class GoalUtil {

    private static final float GOAL_DISTANCE = 102;
    private static final float HANDLE_LENGTH = 40;

    public static final SplineHandle BLUE_GOAL = new SplineHandle(new Vector3(0, -GOAL_DISTANCE, 0), new Vector3(-HANDLE_LENGTH, 0, 0), new Vector3(HANDLE_LENGTH, 0, 0));
    public static final SplineHandle ORANGE_GOAL = new SplineHandle(new Vector3(0, GOAL_DISTANCE, 0), new Vector3(-HANDLE_LENGTH, 0, 0), new Vector3(HANDLE_LENGTH, 0, 0));


    public static SplineHandle getOwnGoal(Bot.Team team) {
        return team == Bot.Team.BLUE ? BLUE_GOAL : ORANGE_GOAL;
    }

    public static SplineHandle getEnemyGoal(Bot.Team team) {
        return team == Bot.Team.BLUE ? ORANGE_GOAL : BLUE_GOAL;
    }
}
