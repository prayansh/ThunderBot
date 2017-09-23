package tarehart.rlbot.input;

import rlbot.input.PyCarInfo;
import rlbot.input.PyGameTickPacket;
import rlbot.input.PyRotator;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.TimeUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SpinTracker {

    PyGameTickPacket previousInput;
    private List<CarSpin> spinList;

    public void readInput(PyGameTickPacket input, double secondsElapsed) {
        spinList = new ArrayList<>(input.gamecars.size());

        for (int i = 0; i < input.gamecars.size(); i++) {
            PyCarInfo carInfo = input.gamecars.get(i);
            if (previousInput != null) {
                PyRotator previousRotation = previousInput.gamecars.get(i).Rotation;
                CarSpin calculatedSpin = calculateSpin(previousRotation, carInfo.Rotation, secondsElapsed);
                spinList.add(calculatedSpin);
            } else {
                spinList.add(new CarSpin(0, 0, 0));
            }
        }

        previousInput = input;
    }

    private CarSpin calculateSpin(PyRotator prevRotation, PyRotator currRotation, double secondsElapsed) {

        double rateConversion = AgentInput.RADIANS_PER_UROT / secondsElapsed;

        double pitchRate = (currRotation.Pitch - prevRotation.Pitch) * rateConversion;
        double yawRate = (currRotation.Yaw - prevRotation.Yaw) * rateConversion;
        double rollRate = (currRotation.Roll - prevRotation.Roll) * rateConversion;

        return new CarSpin(pitchRate, yawRate, rollRate);
    }

    public List<CarSpin> getSpinList() {
        return spinList;
    }
}
