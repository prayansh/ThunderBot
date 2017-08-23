package tarehart.rlbot.math;

import mikera.vectorz.Vector3;

import java.time.LocalDateTime;

public class SpaceTime {

    public Vector3 space;
    public LocalDateTime time;

    public SpaceTime(Vector3 space, LocalDateTime time) {
        this.space = space;
        this.time = time;
    }
}
