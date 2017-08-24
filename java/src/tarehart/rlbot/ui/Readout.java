package tarehart.rlbot.ui;

import mikera.vectorz.AVector;
import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.tuning.BallPrediction;
import tarehart.rlbot.tuning.PredictionWarehouse;

import javax.swing.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class Readout {

    private static final int HEIGHT_BAR_MULTIPLIER = 10;

    private JLabel planPosture;
    private JProgressBar ballHeightActual;
    private JProgressBar ballHeightPredicted;
    private JSlider predictionTime;
    private JPanel rootPanel;
    private JLabel maxCarSpeed;
    private JLabel maxBallHeight;
    private BallPredictionRadar ballPredictionReadout;

    private double maxCarSpeedVal;
    private double maxBallHeightVal;

    private PredictionWarehouse warehouse = new PredictionWarehouse();

    public void update(AgentInput input, Plan.Posture posture, BallPath ballPath) {

        double carSpeed = input.getMyVelocity().magnitude();
        maxCarSpeedVal = Math.max(carSpeed, maxCarSpeedVal);
        maxCarSpeed.setText(Double.toString(maxCarSpeedVal));

        maxBallHeightVal = Math.max(input.ballPosition.z, maxBallHeightVal);
        maxBallHeight.setText(Double.toString(maxBallHeightVal));

        ballHeightPredicted.setValue(0);
        planPosture.setText(posture.name());
        ballHeightActual.setValue((int) (input.ballPosition.z * HEIGHT_BAR_MULTIPLIER));

        int predictionMillis = predictionTime.getValue();
        LocalDateTime predictionTime = LocalDateTime.now().plus(Duration.ofMillis(predictionMillis));

        if (ballPath != null) {
            Optional<SpaceTimeVelocity> predictionSpace = ballPath.getMotionAt(predictionTime);
            if (predictionSpace.isPresent()) {
                BallPrediction prediction = new BallPrediction(predictionSpace.get().spaceTime.space, predictionTime);
                warehouse.addPrediction(prediction);
            }
        }

        Optional<BallPrediction> predictionOfNow = warehouse.getPredictionOfNow();
        if (predictionOfNow.isPresent()) {
            Vector3 predictedLocation = predictionOfNow.get().predictedLocation;
            ballHeightPredicted.setValue((int) (predictedLocation.z * HEIGHT_BAR_MULTIPLIER));

            Vector3 predictionRelative = (Vector3) predictedLocation.subCopy(input.ballPosition);
            ballPredictionReadout.setPredictionCoordinates(new Vector2(predictionRelative.x, predictionRelative.y));
            ballPredictionReadout.setVelocity(new Vector2(input.ballVelocity.x, input.ballVelocity.y));
            ballPredictionReadout.repaint();
        }


    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private static Readout readout = new Readout();
    public static Readout get() {
        return readout;
    }
}
