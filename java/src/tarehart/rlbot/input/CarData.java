package tarehart.rlbot.input;

import mikera.vectorz.Vector3;
import tarehart.rlbot.Bot;

import java.time.LocalDateTime;

public class CarData {
    public final Vector3 position;
    public final Vector3 velocity;
    public final CarOrientation rotation;
    public final CarSpin spin;
    public final double boost;
    public boolean isSupersonic;
    public final Bot.Team team;
    public final LocalDateTime time;


    public CarData(Vector3 position, Vector3 velocity, CarOrientation orientation, CarSpin spin, double boost,
                   boolean isSupersonic, Bot.Team team, LocalDateTime time) {
        this.position = position;
        this.velocity = velocity;
        this.rotation = orientation;
        this.spin = spin;
        this.boost = boost;
        this.isSupersonic = isSupersonic;
        this.team = team;
        this.time = time;
    }
}
