package tarehart.rlbot.math;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimeUtil {

    public static double secondsBetween(LocalDateTime a, LocalDateTime b) {
        return Duration.between(a, b).toMillis() / 1000.0;
    }

    public static Duration toDuration(double seconds) {
        return Duration.ofMillis((long) (seconds * 1000));
    }
}
