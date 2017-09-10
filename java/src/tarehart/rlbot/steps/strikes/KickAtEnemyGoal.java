package tarehart.rlbot.steps.strikes;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.planning.Goal;
import tarehart.rlbot.planning.GoalUtil;

public class KickAtEnemyGoal implements KickStrategy {
    @Override
    public Vector3 getKickDirection(AgentInput input) {
        Goal enemyGoal = GoalUtil.getEnemyGoal(input.team);
        return (Vector3) enemyGoal.getCenter().subCopy(input.ballPosition);
    }

    @Override
    public Vector3 getKickDirection(AgentInput input, SpaceTimeVelocity ballIntercept) {
        Goal enemyGoal = GoalUtil.getEnemyGoal(input.team);
        return (Vector3) enemyGoal.getCenter().subCopy(ballIntercept.getSpace());
    }
}
