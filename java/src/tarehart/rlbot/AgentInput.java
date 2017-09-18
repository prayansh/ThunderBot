package tarehart.rlbot;

import mikera.vectorz.Vector3;
import tarehart.rlbot.input.PyCarInfo;
import tarehart.rlbot.input.PyGameTickPacket;
import tarehart.rlbot.input.PyRotator;
import tarehart.rlbot.input.PyVector3;

import java.time.LocalDateTime;
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
    public LocalDateTime time;
    private static final double URotationToRadians = Math.PI / 32768;


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
        time = LocalDateTime.now();

        ArrayList<Double> neuralInputs = input.get(0);
        ArrayList<Double> scoring = input.get(1);

        blueScore = scoring.get(0).intValue();
        orangeScore = scoring.get(1).intValue();
        blueDemo = scoring.get(2).intValue();
        orangeDemo = scoring.get(3).intValue();

        ballPosition = new Vector3(-neuralInputs.get(7), neuralInputs.get(2), neuralInputs.get(6));
        ballVelocity = new Vector3(-neuralInputs.get(31), neuralInputs.get(33), neuralInputs.get(32));

        orangePosition = new Vector3(-neuralInputs.get(18), neuralInputs.get(3), neuralInputs.get(17));
        orangeVelocity = new Vector3(-neuralInputs.get(34), neuralInputs.get(36), neuralInputs.get(35));
        orangeRotation = new CarRotation(new Vector3(-neuralInputs.get(19), neuralInputs.get(22), neuralInputs.get(25)),
                new Vector3(-neuralInputs.get(21), neuralInputs.get(24), neuralInputs.get(27)));
        orangeBoost = neuralInputs.get(37);

        bluePosition = new Vector3(-neuralInputs.get(5), neuralInputs.get(1), neuralInputs.get(4));
        blueVelocity = new Vector3(-neuralInputs.get(28), neuralInputs.get(30), neuralInputs.get(29));
        blueRotation = new CarRotation(new Vector3(-neuralInputs.get(8), neuralInputs.get(11), neuralInputs.get(14)),
                new Vector3(-neuralInputs.get(10), neuralInputs.get(13),  neuralInputs.get(16)));
        blueBoost = neuralInputs.get(0);
    }

    public AgentInput(PyGameTickPacket gameTickPacket, Bot.Team team) {
        this.team = team;
        time = LocalDateTime.now();
        
        final PyCarInfo blueCar;
        final PyCarInfo orangeCar;
        
        if (gameTickPacket.CarInfo.get(0).Team == 0) {
            blueCar = gameTickPacket.CarInfo.get(0);
            orangeCar = gameTickPacket.CarInfo.get(1);
        } else {
            blueCar = gameTickPacket.CarInfo.get(1);
            orangeCar = gameTickPacket.CarInfo.get(0);
        }

        blueScore = blueCar.Score.Goals + orangeCar.Score.OwnGoals;
        orangeScore = orangeCar.Score.Goals + blueCar.Score.OwnGoals;
        blueDemo = blueCar.Score.Demolitions;
        orangeDemo = orangeCar.Score.Demolitions;
    
        ballPosition = convert(gameTickPacket.gameBall.Location);
        ballVelocity = convert(gameTickPacket.gameBall.Velocity);

        orangePosition = convert(orangeCar.Location);
        orangeVelocity = convert(orangeCar.Velocity);
        orangeRotation = convert(orangeCar.Rotation);
        orangeBoost = orangeCar.Boost;

        bluePosition = convert(blueCar.Location);
        blueVelocity = convert(blueCar.Velocity);
        blueRotation = convert(blueCar.Rotation);
        blueBoost = blueCar.Boost;
    }

    private CarRotation convert(PyRotator rotation) {
        
        double noseX = -1 * Math.cos(rotation.Pitch * URotationToRadians) * Math.cos(rotation.Yaw * URotationToRadians);
        double noseY = Math.cos(rotation.Pitch * URotationToRadians) * Math.sin(rotation.Yaw * URotationToRadians);
        double noseZ = Math.cos(rotation.Pitch * URotationToRadians);
        
        double roofX = Math.cos(rotation.Roll * URotationToRadians) * Math.sin(rotation.Pitch * URotationToRadians) * Math.cos(rotation.Yaw * URotationToRadians) + Math.sin(rotation.Roll * URotationToRadians) * Math.sin(rotation.Yaw * URotationToRadians);
        double roofY = Math.sin(rotation.Roll * URotationToRadians) * Math.cos(rotation.Pitch * URotationToRadians);
        double roofZ = Math.cos(rotation.Roll * URotationToRadians) * Math.cos(rotation.Pitch * URotationToRadians);

        return new CarRotation(new Vector3(noseX, noseY, noseZ), new Vector3(roofX, roofY, roofZ));
    }

    private Vector3 convert(PyVector3 location) {
        // Invert the X value so that the axes make more sense.
        return new Vector3(-location.X, location.Y, location.Z);
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

    public Vector3 getEnemyPosition() {
        return team == Bot.Team.BLUE ? orangePosition : bluePosition;
    }
}
