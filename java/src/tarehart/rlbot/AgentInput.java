package tarehart.rlbot;

import mikera.vectorz.Vector3;
import rlbot.input.PyCarInfo;
import rlbot.input.PyGameTickPacket;
import rlbot.input.PyRotator;
import rlbot.input.PyVector3;
import tarehart.rlbot.input.*;
import tarehart.rlbot.math.TimeUtil;

import java.time.LocalDateTime;

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
    public static final int UROT_IN_SEMICIRCLE = 32768;
    public static final double RADIANS_PER_UROT = Math.PI / UROT_IN_SEMICIRCLE;
    private static final double PACKET_DISTANCE_TO_CLASSIC = 50;

    public AgentInput(PyGameTickPacket gameTickPacket, Bot.Team team, Chronometer chronometer, SpinTracker spinTracker) {

        chronometer.readInput(gameTickPacket.gameInfo);

        this.team = team;
        time = chronometer.getGameTime();

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


        blueScore = blueCarInput.Score.Goals + orangeCarInput.Score.OwnGoals;
        orangeScore = orangeCarInput.Score.Goals + blueCarInput.Score.OwnGoals;
        blueDemo = blueCarInput.Score.Demolitions;
        orangeDemo = orangeCarInput.Score.Demolitions;

        ballPosition = convert(gameTickPacket.gameball.Location);
        ballVelocity = convert(gameTickPacket.gameball.Velocity);

        Vector3 orangePosition = convert(orangeCarInput.Location);
        Vector3 orangeVelocity = convert(orangeCarInput.Velocity);
        CarOrientation orangeOrientation = convert(orangeCarInput.Rotation);

        double orangeBoost = orangeCarInput.Boost;


        Vector3 bluePosition = convert(blueCarInput.Location);
        Vector3 blueVelocity = convert(blueCarInput.Velocity);
        CarOrientation blueOrientation = convert(blueCarInput.Rotation);

        double blueBoost = blueCarInput.Boost;


        double elapsedSeconds = TimeUtil.toSeconds(chronometer.getTimeDiff());
        spinTracker.readInput(blueOrientation, orangeOrientation, elapsedSeconds);

        final CarSpin blueSpin = spinTracker.getBlueSpin();
        final CarSpin orangeSpin = spinTracker.getOrangeSpin();

        System.out.println(String.format("%.2f\t%.2f\t%.2f\t%.4f", blueSpin.pitchRate, blueSpin.yawRate, blueSpin.rollRate, elapsedSeconds));


        orangeCar = new CarData(orangePosition, orangeVelocity, orangeOrientation, orangeSpin, orangeBoost,
                orangeCarInput.bSuperSonic, Bot.Team.ORANGE, time);

        blueCar = new CarData(bluePosition, blueVelocity, blueOrientation, blueSpin, blueBoost,
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
