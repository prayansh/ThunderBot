package tarehart.rlbot;

import mikera.vectorz.Vector3;

import javax.vecmath.Quat4f;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class CarData {
    public final Vector3 position;
    public final Vector3 velocity;
    public final CarRotation rotation;
    public final Quat4f rotationalVelocity;
    public final double boost;
    public final Bot.Team team;
    public final LocalDateTime time;


    public CarData(Vector3 position, Vector3 velocity, CarRotation rotation, Quat4f rotationalVelocity, double boost,
                   Bot.Team team, LocalDateTime time) {
        this.position = position;
        this.velocity = velocity;
        this.rotation = rotation;
        this.rotationalVelocity = rotationalVelocity;
        this.boost = boost;
        this.team = team;
        this.time = time;
    }
}
