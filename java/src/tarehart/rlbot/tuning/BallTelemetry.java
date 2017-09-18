package tarehart.rlbot.tuning;

import tarehart.rlbot.physics.BallPath;

import java.util.Optional;

public class BallTelemetry {

    private static BallPath ballPath;

    public static void setPath(BallPath ballPath) {
        BallTelemetry.ballPath = ballPath;
    }

    public static void reset() {
        ballPath = null;
    }


    public static Optional<BallPath> getPath() {
        return Optional.ofNullable(ballPath);
    }
}
