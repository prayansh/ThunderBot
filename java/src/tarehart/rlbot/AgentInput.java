package tarehart.rlbot;

import mikera.vectorz.Vector3;
import rlbot.input.PyCarInfo;
import rlbot.input.PyGameTickPacket;
import rlbot.input.PyRotator;
import rlbot.input.PyVector3;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.input.CarOrientation;
import tarehart.rlbot.input.CarSpin;

import java.time.LocalDateTime;
import java.util.List;

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
    public static final double RADIANS_PER_UROT = Math.PI / 32768;
    private static final double PACKET_DISTANCE_TO_CLASSIC = 50;

    public AgentInput(PyGameTickPacket gameTickPacket, Bot.Team team, LocalDateTime gameTime, List<CarSpin> spins) {
        this.team = team;
        time = gameTime;

        final int blueIndex;
        final int orangeIndex;

        if (gameTickPacket.gamecars.get(0).Team == 0) {
            blueIndex = 0;
            orangeIndex = 1;
        } else {
            orangeIndex = 0;
            blueIndex = 1;
        }

        final PyCarInfo blueCarInput = gameTickPacket.gamecars.get(blueIndex);
        final PyCarInfo orangeCarInput = gameTickPacket.gamecars.get(orangeIndex);
        final CarSpin blueSpin = spins.get(blueIndex);
        final CarSpin orangeSpin = spins.get(orangeIndex);


        blueScore = blueCarInput.Score.Goals + orangeCarInput.Score.OwnGoals;
        orangeScore = orangeCarInput.Score.Goals + blueCarInput.Score.OwnGoals;
        blueDemo = blueCarInput.Score.Demolitions;
        orangeDemo = orangeCarInput.Score.Demolitions;

        ballPosition = convert(gameTickPacket.gameball.Location);
        ballVelocity = convert(gameTickPacket.gameball.Velocity);

        Vector3 orangePosition = convert(orangeCarInput.Location);
        Vector3 orangeVelocity = convert(orangeCarInput.Velocity);
        CarOrientation orangeRotation = convert(orangeCarInput.Rotation);

        double orangeBoost = orangeCarInput.Boost;


        Vector3 bluePosition = convert(blueCarInput.Location);
        Vector3 blueVelocity = convert(blueCarInput.Velocity);
        CarOrientation blueRotation = convert(blueCarInput.Rotation);

        double blueBoost = blueCarInput.Boost;




        orangeCar = new CarData(orangePosition, orangeVelocity, orangeRotation, orangeSpin, orangeBoost,
                orangeCarInput.bSuperSonic, Bot.Team.ORANGE, time);

        blueCar = new CarData(bluePosition, blueVelocity, blueRotation, blueSpin, blueBoost,
                blueCarInput.bSuperSonic, Bot.Team.BLUE, time);


    }

    private CarOrientation convert(PyRotator rotation) {

        double noseX = -1 * Math.cos(rotation.Pitch * RADIANS_PER_UROT) * Math.cos(rotation.Yaw * RADIANS_PER_UROT);
        double noseY = Math.cos(rotation.Pitch * RADIANS_PER_UROT) * Math.sin(rotation.Yaw * RADIANS_PER_UROT);
        double noseZ = Math.sin(rotation.Pitch * RADIANS_PER_UROT);

        double roofX = Math.cos(rotation.Roll * RADIANS_PER_UROT) * Math.sin(rotation.Pitch * RADIANS_PER_UROT) * Math.cos(rotation.Yaw * RADIANS_PER_UROT) + Math.sin(rotation.Roll * RADIANS_PER_UROT) * Math.sin(rotation.Yaw * RADIANS_PER_UROT);
        double roofY = Math.cos(rotation.Yaw * RADIANS_PER_UROT) * Math.sin(rotation.Roll * RADIANS_PER_UROT) - Math.cos(rotation.Roll * RADIANS_PER_UROT) * Math.sin(rotation.Pitch * RADIANS_PER_UROT) * Math.sin(rotation.Yaw * RADIANS_PER_UROT);
        double roofZ = Math.cos(rotation.Roll * RADIANS_PER_UROT) * Math.cos(rotation.Pitch * RADIANS_PER_UROT);

        return new CarOrientation(new Vector3(noseX, noseY, noseZ), new Vector3(roofX, roofY, roofZ));
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
