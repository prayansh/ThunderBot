package tarehart.rlbot.input;

import rlbot.input.PyGameInfo;
import tarehart.rlbot.math.TimeUtil;

import java.time.Duration;
import java.time.LocalDateTime;

public class Chronometer {

    private LocalDateTime gameTime;
    private LocalDateTime previousGameTime;

    private Double previousGameTimeRemaining = null;
    private Double previousTimeSeconds = null;

    public Chronometer() {
        gameTime = LocalDateTime.now();
        previousGameTime = null;
    }

    public void readInput(PyGameInfo timeInfo) {

        if (previousGameTimeRemaining != null && previousTimeSeconds != null) {
            double deltaSeconds;
            if (timeInfo.GameTimeRemaining > 0) {
                deltaSeconds = Math.abs(previousGameTimeRemaining - timeInfo.GameTimeRemaining);
            } else {
                deltaSeconds = timeInfo.TimeSeconds - previousTimeSeconds;
            }

            previousGameTime = gameTime;
            gameTime = gameTime.plus(TimeUtil.toDuration(deltaSeconds));
        }

        previousGameTimeRemaining = timeInfo.GameTimeRemaining;
        previousTimeSeconds = timeInfo.TimeSeconds;
    }

    public LocalDateTime getGameTime() {
        return gameTime;
    }

    public Duration getTimeDiff() {
        if (previousGameTime != null) {
            return Duration.between(previousGameTime, gameTime);
        }
        return Duration.ofMillis(100); // This should be extremely rare.
    }
}
