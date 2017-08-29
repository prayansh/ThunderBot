package tarehart.rlbot.ui;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.tuning.BallPrediction;
import tarehart.rlbot.tuning.PredictionWarehouse;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
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
    private BallPredictionRadar ballPredictionReadout;
    private JProgressBar ballHeightActualMax;
    private JProgressBar ballHeightPredictedMax;
    private JTextPane situationText;
    private JLabel predictionTimeSeconds;
    private JTextArea logViewer;

    private double maxCarSpeedVal;

    private LocalDateTime actualMaxTime;
    private LocalDateTime predictedMaxTime;

    private PredictionWarehouse warehouse = new PredictionWarehouse();


    public Readout() {
        DefaultCaret caret = (DefaultCaret)logViewer.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    public void update(AgentInput input, Plan.Posture posture, String situation, String log, BallPath ballPath) {

        situationText.setText(situation);
        predictionTimeSeconds.setText(String.format("%.2f", predictionTime.getValue() / 1000.0));
        logViewer.append(log);

        // ballHeightPredicted.setValue(0); // Commented out to avoid flicker. Should always be fresh anyway.
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


        if (ballHeightActualMax.getValue() < ballHeightActual.getValue()) {
            ballHeightActualMax.setValue(ballHeightActual.getValue());
            actualMaxTime = input.time;
        } else if(Duration.between(input.time, actualMaxTime).abs().getSeconds() > 3) {
            ballHeightActualMax.setValue(0);
        }

        if (ballHeightPredictedMax.getValue() < ballHeightPredicted.getValue()) {
            ballHeightPredictedMax.setValue(ballHeightPredicted.getValue());
            predictedMaxTime = input.time;
        } else if(Duration.between(input.time, predictedMaxTime).abs().getSeconds() > 3) {
            ballHeightPredictedMax.setValue(0);
        }

    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
