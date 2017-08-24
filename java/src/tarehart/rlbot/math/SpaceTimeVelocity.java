package tarehart.rlbot.math;

import mikera.vectorz.Vector3;

import java.time.LocalDateTime;

public class SpaceTimeVelocity {
    public SpaceTime spaceTime;
    public Vector3 velocity;

    public SpaceTimeVelocity(SpaceTime spaceTime, Vector3 velocity) {
        this.spaceTime = spaceTime;
        this.velocity = velocity;
    }

    public Vector3 getSpace() {
        return spaceTime.space;
    }

    public Vector3 getVelocity() {
        return velocity;
    }

    public LocalDateTime getTime() {
        return spaceTime.time;
    }
}
