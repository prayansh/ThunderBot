package tarehart.rlbot.input;

import mikera.vectorz.Vector3;
import tarehart.rlbot.math.VectorUtil;

import java.util.function.Function;

public class SpinTracker {

    private CarOrientation previousBlue;
    private CarOrientation previousOrange;
    private CarSpin blueSpin = new CarSpin(0, 0, 0);
    private CarSpin orangeSpin = new CarSpin(0, 0, 0);

    public void readInput(CarOrientation blueOrientation, CarOrientation orangeOrientation, double secondsElapsed) {

        if (secondsElapsed > 0) {
            if (previousBlue != null && previousOrange != null) {
                blueSpin = getCarSpin(previousBlue, blueOrientation, secondsElapsed);
                orangeSpin = getCarSpin(previousOrange, orangeOrientation, secondsElapsed);
            }
            previousBlue = blueOrientation;
            previousOrange = orangeOrientation;
        }
    }

    private CarSpin getCarSpin(CarOrientation prevData, CarOrientation currData, double secondsElapsed) {

        double rateConversion = 1 / secondsElapsed;

        double pitchAmount = getRotationAmount(currData.noseVector, prevData.roofVector);
        double yawAmount = getRotationAmount(currData.noseVector, prevData.rightVector);
        double rollAmount = getRotationAmount(currData.roofVector, prevData.rightVector);

        return new CarSpin(pitchAmount * rateConversion, yawAmount * rateConversion, rollAmount * rateConversion);
    }

    private double getRotationAmount(Vector3 currentMoving, Vector3 previousOrthogonal) {
        Vector3 projection = VectorUtil.project(currentMoving, previousOrthogonal);
        return Math.asin(projection.magnitude() * Math.signum(projection.dotProduct(previousOrthogonal)));
    }

    public CarSpin getOrangeSpin() {
        return orangeSpin;
    }

    public CarSpin getBlueSpin() {
        return blueSpin;
    }
}
