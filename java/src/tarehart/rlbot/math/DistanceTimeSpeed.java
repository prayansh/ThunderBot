package tarehart.rlbot.math;

import java.time.LocalDateTime;

public class DistanceTimeSpeed {

    public double distance;
    public LocalDateTime time;
    public double speed;

    public DistanceTimeSpeed(double distance, LocalDateTime time, double speed) {
        this.distance = distance;
        this.time = time;
        this.speed = speed;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
