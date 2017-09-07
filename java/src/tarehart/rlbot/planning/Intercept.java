package tarehart.rlbot.planning;

import mikera.vectorz.Vector3;
import tarehart.rlbot.math.SpaceTime;

import java.time.LocalDateTime;

public class Intercept {
    private Vector3 space;
    private LocalDateTime time;
    private double airBoost;

    public Intercept(Vector3 space, LocalDateTime time, double airBoost) {
        this.space = space;
        this.time = time;
        this.airBoost = airBoost;
    }

    public Intercept(SpaceTime spaceTime) {
        this(spaceTime.space, spaceTime.time, 0);
    }

    public double getAirBoost() {
        return airBoost;
    }

    public Vector3 getSpace() {
        return space;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public SpaceTime toSpaceTime() {
        return new SpaceTime(space, time);
    }
}
