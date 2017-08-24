package tarehart.rlbot.tuning;

import tarehart.rlbot.Bot;
import tarehart.rlbot.physics.BallPath;

public class Telemetry {

    private BallPath ballPath;

    public void setBallPath(BallPath ballPath) {
        this.ballPath = ballPath;
    }

    public void reset() {
        this.ballPath = null;
    }

    private static Telemetry blueTelemetry = new Telemetry();
    private static Telemetry orangeTelemetry = new Telemetry();

    public static Telemetry forTeam(Bot.Team team) {
        return team == Bot.Team.BLUE ? blueTelemetry : orangeTelemetry;
    }

    public BallPath getBallPath() {
        return ballPath;
    }
}
