package tarehart.rlbot;

import mikera.vectorz.Vector3;
import tarehart.rlbot.input.PyCarInfo;
import tarehart.rlbot.input.PyGameTickPacket;
import tarehart.rlbot.input.PyRotator;
import tarehart.rlbot.input.PyVector3;
import tarehart.rlbot.planning.AccelerationModel;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class AgentInput {

    public final CarData blueCar;
    public final CarData orangeCar;

    public final int blueScore;
    public final int orangeScore;
    public final int blueDemo;
    public final int orangeDemo;
    public final Vector3 ballPosition;
    public final Vector3 ballVelocity;
    public final Bot.Team team;
    public LocalDateTime time;
    private static final double URotationToRadians = Math.PI / 32768;
    private static final double PACKET_DISTANCE_TO_CLASSIC = 50;


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

        Vector3 orangePosition = new Vector3(-neuralInputs.get(18), neuralInputs.get(3), neuralInputs.get(17));
        Vector3 orangeVelocity = new Vector3(-neuralInputs.get(34), neuralInputs.get(36), neuralInputs.get(35));
        CarRotation orangeRotation = new CarRotation(new Vector3(-neuralInputs.get(19), neuralInputs.get(22), neuralInputs.get(25)),
                new Vector3(-neuralInputs.get(21), neuralInputs.get(24), neuralInputs.get(27)));
        double orangeBoost = neuralInputs.get(37);
        boolean orangeSupersonic = AccelerationModel.SUPERSONIC_SPEED - orangeVelocity.magnitude() < .01;

        orangeCar = new CarData(orangePosition, orangeVelocity, orangeRotation, null, orangeBoost,
                orangeSupersonic, Bot.Team.ORANGE, time);

        Vector3 bluePosition = new Vector3(-neuralInputs.get(5), neuralInputs.get(1), neuralInputs.get(4));
        Vector3 blueVelocity = new Vector3(-neuralInputs.get(28), neuralInputs.get(30), neuralInputs.get(29));
        CarRotation blueRotation = new CarRotation(new Vector3(-neuralInputs.get(8), neuralInputs.get(11), neuralInputs.get(14)),
                new Vector3(-neuralInputs.get(10), neuralInputs.get(13),  neuralInputs.get(16)));
        double blueBoost = neuralInputs.get(0);
        boolean blueSupersonic = AccelerationModel.SUPERSONIC_SPEED - blueVelocity.magnitude() < .01;

        blueCar = new CarData(bluePosition, blueVelocity, blueRotation, null, blueBoost,
                blueSupersonic, Bot.Team.BLUE, time);
    }


    public AgentInput(PyGameTickPacket gameTickPacket, Bot.Team team) {
        this.team = team;
        time = LocalDateTime.now();

        final PyCarInfo blueCarInput;
        final PyCarInfo orangeCarInput;

        if (gameTickPacket.CarInfo.get(0).Team == 0) {
            blueCarInput = gameTickPacket.CarInfo.get(0);
            orangeCarInput = gameTickPacket.CarInfo.get(1);
        } else {
            blueCarInput = gameTickPacket.CarInfo.get(1);
            orangeCarInput = gameTickPacket.CarInfo.get(0);
        }

        blueScore = blueCarInput.Score.Goals + orangeCarInput.Score.OwnGoals;
        orangeScore = orangeCarInput.Score.Goals + blueCarInput.Score.OwnGoals;
        blueDemo = blueCarInput.Score.Demolitions;
        orangeDemo = orangeCarInput.Score.Demolitions;

        ballPosition = convert(gameTickPacket.gameBall.Location);
        ballVelocity = convert(gameTickPacket.gameBall.Velocity);

        Vector3 orangePosition = convert(orangeCarInput.Location);
        Vector3 orangeVelocity = convert(orangeCarInput.Velocity);
        CarRotation orangeRotation = convert(orangeCarInput.Rotation);
        double orangeBoost = orangeCarInput.Boost;
        orangeCar = new CarData(orangePosition, orangeVelocity, orangeRotation, null, orangeBoost,
                orangeCarInput.SuperSonic, Bot.Team.ORANGE, time);

        Vector3 bluePosition = convert(blueCarInput.Location);
        Vector3 blueVelocity = convert(blueCarInput.Velocity);
        CarRotation blueRotation = convert(blueCarInput.Rotation);
        double blueBoost = blueCarInput.Boost;
        blueCar = new CarData(bluePosition, blueVelocity, blueRotation, null, blueBoost,
                blueCarInput.SuperSonic, Bot.Team.BLUE, time);
    }


    private CarRotation convert(PyRotator rotation) {

        double noseX = -1 * Math.cos(rotation.Pitch * URotationToRadians) * Math.cos(rotation.Yaw * URotationToRadians);
        double noseY = Math.cos(rotation.Pitch * URotationToRadians) * Math.sin(rotation.Yaw * URotationToRadians);
        double noseZ = Math.sin(rotation.Pitch * URotationToRadians);

        double roofX = Math.cos(rotation.Roll * URotationToRadians) * Math.sin(rotation.Pitch * URotationToRadians) * Math.cos(rotation.Yaw * URotationToRadians) + Math.sin(rotation.Roll * URotationToRadians) * Math.sin(rotation.Yaw * URotationToRadians);
        double roofY = Math.cos(rotation.Yaw * URotationToRadians) * Math.sin(rotation.Roll * URotationToRadians) - Math.cos(rotation.Roll * URotationToRadians) * Math.sin(rotation.Pitch * URotationToRadians) * Math.sin(rotation.Yaw * URotationToRadians);
        double roofZ = Math.cos(rotation.Roll * URotationToRadians) * Math.cos(rotation.Pitch * URotationToRadians);

        return new CarRotation(new Vector3(noseX, noseY, noseZ), new Vector3(roofX, roofY, roofZ));
    }

    private Vector3 convert(PyVector3 location) {
        // Invert the X value so that the axes make more sense.
        return new Vector3(-location.X / PACKET_DISTANCE_TO_CLASSIC, location.Y / PACKET_DISTANCE_TO_CLASSIC, location.Z / PACKET_DISTANCE_TO_CLASSIC);
    }

    public CarData getMyCarData() {
        return team == Bot.Team.BLUE ? blueCar : orangeCar;
    }

    public CarData getEnemyCarData() {
        return team == Bot.Team.BLUE ? orangeCar : blueCar;
    }
}
