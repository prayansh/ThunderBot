package tarehart.rlbot.input;

import java.util.List;

public class PyGameTickPacket {
    public List<PyCarInfo> CarInfo; // Python type: CarInfo * maxCars
    public int numCars; // Python type: ctypes.c_int
    public List<PyBoostInfo> BoostInfo; // Python type: BoostInfo * maxBoosts
    public int numBoosts; // Python type: ctypes.c_int
    public PyBallInfo gameBall; // Python type: BallInfo
}
