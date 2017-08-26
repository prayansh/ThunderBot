package tarehart.rlbot;

public class AgentOutput {

    public static final int MAX_TILT = 32767;

    // 0 is straight, -1 is hard left, 1 is hard right.
    private double steeringTilt;
    private double pitchTilt;

    // 0 is none, 1 is full
    private double acceleration;
    private double deceleration;

    private boolean jumpDepressed;
    private boolean boostDepressed;
    private boolean slideDepressed;

    public AgentOutput() {
    }

    public AgentOutput withSteer(double steeringTilt) {
        this.steeringTilt = steeringTilt;
        return this;
    }

    public AgentOutput withPitch(double pitchTilt) {
        this.pitchTilt = pitchTilt;
        return this;
    }

    public AgentOutput withAcceleration(double acceleration) {
        this.acceleration = acceleration;
        return this;
    }

    public AgentOutput withDeceleration(double deceleration) {
        this.deceleration = deceleration;
        return this;
    }

    public AgentOutput withJump(boolean jumpDepressed) {
        this.jumpDepressed = jumpDepressed;
        return this;
    }

    public AgentOutput withBoost(boolean boostDepressed) {
        this.boostDepressed = boostDepressed;
        return this;
    }

    public AgentOutput withSlide(boolean slideDepressed) {
        this.slideDepressed = slideDepressed;
        return this;
    }

    public AgentOutput withJump() {
        this.jumpDepressed = true;
        return this;
    }

    public AgentOutput withBoost() {
        this.boostDepressed = true;
        return this;
    }

    public AgentOutput withSlide() {
        this.slideDepressed = true;
        return this;
    }


    public int[] toPython() {
        return new int[] {
                convertMagnitudeWithNegatives(steeringTilt),
                convertMagnitudeWithNegatives(pitchTilt),
                convertMagnitudeOnlyPositive(acceleration),
                convertMagnitudeOnlyPositive(deceleration),
                jumpDepressed ? 1 : 0,
                boostDepressed ? 1 : 0,
                slideDepressed ? 1 : 0
        };
    }

    private int convertMagnitudeWithNegatives(double tilt) {
        double normalized = (tilt + 1) / 2;
        return convertMagnitudeOnlyPositive(normalized);
    }

    private int convertMagnitudeOnlyPositive(double normalized) {
        double intScaled = normalized * MAX_TILT;
        return (int) Math.round(intScaled);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgentOutput that = (AgentOutput) o;

        if (Double.compare(that.steeringTilt, steeringTilt) != 0) return false;
        if (Double.compare(that.pitchTilt, pitchTilt) != 0) return false;
        if (Double.compare(that.acceleration, acceleration) != 0) return false;
        if (Double.compare(that.deceleration, deceleration) != 0) return false;
        if (jumpDepressed != that.jumpDepressed) return false;
        if (boostDepressed != that.boostDepressed) return false;
        return slideDepressed == that.slideDepressed;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(steeringTilt);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(pitchTilt);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(acceleration);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(deceleration);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (jumpDepressed ? 1 : 0);
        result = 31 * result + (boostDepressed ? 1 : 0);
        result = 31 * result + (slideDepressed ? 1 : 0);
        return result;
    }
}
