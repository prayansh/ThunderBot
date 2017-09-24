package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.planning.GoalUtil;

public class KickAtEnemyGoal implements KickStrategy {
    @Override
    public Vector3 getKickDirection(AgentInput input) {
        Vector3 scoreLocation = GoalUtil.getEnemyGoal(input.team).getNearestEntrance(input.ballPosition, 3);
        return (Vector3) scoreLocation.subCopy(input.ballPosition);
    }

    @Override
    public Vector3 getKickDirection(AgentInput input, Vector3 ballPosition) {
        Vector3 scoreLocation = GoalUtil.getEnemyGoal(input.team).getNearestEntrance(input.ballPosition, 3);
        return (Vector3) scoreLocation.subCopy(ballPosition);
    }
}
