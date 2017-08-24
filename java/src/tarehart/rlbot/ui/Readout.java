package tarehart.rlbot.ui;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
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
    private JPanel ballPredictionReadout;
    private JSlider predictionTime;
    private JPanel rootPanel;

    private PredictionWarehouse warehouse = new PredictionWarehouse();

    public void update(AgentInput input, Plan.Posture posture, BallPath ballPath) {
        planPosture.setText(posture.name());
        ballHeightActual.setValue((int) (input.ballPosition.z * HEIGHT_BAR_MULTIPLIER));

        int predictionMillis = predictionTime.getValue();
        LocalDateTime predictionTime = LocalDateTime.now().plus(Duration.ofMillis(predictionMillis));

        if (ballPath != null) {
            Optional<Vector3> predictionSpace = ballPath.getSpace(predictionTime);
            if (predictionSpace.isPresent()) {
                BallPrediction prediction = new BallPrediction(predictionSpace.get(), predictionTime);
                warehouse.addPrediction(prediction);
            }
        }

        Optional<BallPrediction> predictionOfNow = warehouse.getPredictionOfNow();
        if (predictionOfNow.isPresent()) {
            Vector3 predictedLocation = predictionOfNow.get().predictedLocation;
            ballHeightPredicted.setValue((int) (predictedLocation.z * HEIGHT_BAR_MULTIPLIER));
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
