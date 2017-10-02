package tarehart.rlbot.planning;

import tarehart.rlbot.math.SpaceTime;

import java.time.LocalDateTime;

public class TacticalSituation {

    public double ownGoalFutureProximity;
    public double distanceBallIsBehindUs;
    public double enemyOffensiveApproachCorrection; // If the enemy wants to shoot on our goal, how many radians away from a direct approach?
    public SpaceTime expectedEnemyContact;
}
