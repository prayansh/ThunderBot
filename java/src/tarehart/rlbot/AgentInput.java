package tarehart.rlbot;

import com.sun.javafx.geom.Vec3f;
import mikera.vectorz.Vector3;

import java.util.ArrayList;

public class AgentInput {
    public final int blueScore;
    public final int orangeScore;
    public final int blueDemo;
    public final int orangeDemo;
    public final Vector3 ballPosition;
    public final Vector3 ballVelocity;
    public final Vector3 orangePosition;
    public final Vector3 bluePosition;
    public final CarRotation blueRotation;
    public final CarRotation orangeRotation;
    public final float orangeBoost;
    public final float blueBoost;
    public final Bot.Team team;
    public final Vector3 orangeVelocity;
    public final Vector3 blueVelocity;


    /**
     * This is based on PlayHelper.py.
     *
     * They define the axes strangely such that Z goes along the ground, and I don't like that,
     * so I'm transposing them here. In my world, Z points toward the ceiling.
     *
     * @param input
     */
    public AgentInput(ArrayList<ArrayList<Number>> input, Bot.Team team) {

        this.team = team;

        ArrayList<Number> neuralInputs = input.get(0);
        ArrayList<Number> scoring = input.get(1);

        blueScore = scoring.get(0).intValue();
        orangeScore = scoring.get(1).intValue();
        blueDemo = scoring.get(2).intValue();
        orangeDemo = scoring.get(3).intValue();

        ballPosition = new Vector3(neuralInputs.get(7).floatValue(), neuralInputs.get(2).floatValue(), neuralInputs.get(6).floatValue());
        ballVelocity = new Vector3(neuralInputs.get(31).floatValue(), neuralInputs.get(33).floatValue(), neuralInputs.get(32).floatValue());

        orangePosition = new Vector3(neuralInputs.get(18).floatValue(), neuralInputs.get(3).floatValue(), neuralInputs.get(17).floatValue());
        orangeVelocity = new Vector3(neuralInputs.get(34).floatValue(), neuralInputs.get(36).floatValue(), neuralInputs.get(35).floatValue());
        orangeRotation = new CarRotation(neuralInputs.get(19).floatValue(), neuralInputs.get(22).floatValue(), neuralInputs.get(25).floatValue(),
                neuralInputs.get(27).floatValue(), neuralInputs.get(26).floatValue());
        orangeBoost = neuralInputs.get(37).floatValue();

        bluePosition = new Vector3(neuralInputs.get(5).floatValue(), neuralInputs.get(1).floatValue(), neuralInputs.get(4).floatValue());
        blueVelocity = new Vector3(neuralInputs.get(28).floatValue(), neuralInputs.get(30).floatValue(), neuralInputs.get(29).floatValue());
        blueRotation = new CarRotation(neuralInputs.get(8).floatValue(), neuralInputs.get(4).floatValue(), neuralInputs.get(7).floatValue(),
                neuralInputs.get(16).floatValue(), neuralInputs.get(15).floatValue());
        blueBoost = neuralInputs.get(0).floatValue();
    }

    public Vector3 getMyPosition() {
        return team == Bot.Team.BLUE ? bluePosition : orangePosition;
    }

    public Vector3 getMyVelocity() {
        return team == Bot.Team.BLUE ? blueVelocity : orangeVelocity;
    }

    public CarRotation getMyRotation() {
        return team == Bot.Team.BLUE ? blueRotation : orangeRotation;
    }

    public float getMyBoost() {
        return team == Bot.Team.BLUE ? blueBoost : orangeBoost;
    }
}
