package tarehart.rlbot;

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
    public final double orangeBoost;
    public final double blueBoost;
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
    public AgentInput(ArrayList<ArrayList<Double>> input, Bot.Team team) {

        this.team = team;

        ArrayList<Double> neuralInputs = input.get(0);
        ArrayList<Double> scoring = input.get(1);

        blueScore = scoring.get(0).intValue();
        orangeScore = scoring.get(1).intValue();
        blueDemo = scoring.get(2).intValue();
        orangeDemo = scoring.get(3).intValue();

        ballPosition = new Vector3(neuralInputs.get(7), neuralInputs.get(2), neuralInputs.get(6));
        ballVelocity = new Vector3(neuralInputs.get(31), neuralInputs.get(33), neuralInputs.get(32));

        orangePosition = new Vector3(neuralInputs.get(18), neuralInputs.get(3), neuralInputs.get(17));
        orangeVelocity = new Vector3(neuralInputs.get(34), neuralInputs.get(36), neuralInputs.get(35));
        orangeRotation = new CarRotation(neuralInputs.get(19), neuralInputs.get(22), neuralInputs.get(25),
                neuralInputs.get(27), neuralInputs.get(26));
        orangeBoost = neuralInputs.get(37);

        bluePosition = new Vector3(neuralInputs.get(5), neuralInputs.get(1), neuralInputs.get(4));
        blueVelocity = new Vector3(neuralInputs.get(28), neuralInputs.get(30), neuralInputs.get(29));
        blueRotation = new CarRotation(neuralInputs.get(8), neuralInputs.get(4), neuralInputs.get(7),
                neuralInputs.get(16), neuralInputs.get(15));
        blueBoost = neuralInputs.get(0);
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

    public double getMyBoost() {
        return team == Bot.Team.BLUE ? blueBoost : orangeBoost;
    }
}
