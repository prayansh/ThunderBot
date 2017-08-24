package tarehart.rlbot.tuning;

import tarehart.rlbot.Bot;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class PredictionWarehouse {

    private LinkedList<BallPrediction> ballPredictions = new LinkedList<>();

    public Optional<BallPrediction> getPredictionOfNow() {
        if (ballPredictions.isEmpty()) {
            return Optional.empty();
        }

        BallPrediction oldest = ballPredictions.getFirst();
        if (!oldest.predictedMoment.isBefore(LocalDateTime.now())) {
            ballPredictions.removeFirst();
            return Optional.of(oldest);
        }
        return Optional.empty();
    }

    public void addPrediction(BallPrediction prediction) {
        ballPredictions.add(prediction);
    }
}
