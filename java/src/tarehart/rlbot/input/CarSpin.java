package tarehart.rlbot.input;

/**
 * All values are in radians per second.
 */
public class CarSpin {
    public double pitchRate;
    public double yawRate;
    public double rollRate;

    public CarSpin(double pitchRate, double yawRate, double rollRate) {
        this.pitchRate = pitchRate;
        this.yawRate = yawRate;
        this.rollRate = rollRate;
    }
}
