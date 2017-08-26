package tarehart.rlbot.tuning;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Optional;

public class PredictionWarehouse {

    private LinkedList<BallPrediction> ballPredictions = new LinkedList<>();

    public Optional<BallPrediction> getPredictionOfNow() {
        if (ballPredictions.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(ballPredictions.getFirst().predictedMoment)) {
            return Optional.empty();
        }


        BallPrediction oldest;
        do {
            if (ballPredictions.isEmpty()) {
                return Optional.empty();
            }

            oldest = ballPredictions.removeFirst();
        } while (now.isAfter(oldest.predictedMoment));

        return Optional.of(oldest);
    }

    public void addPrediction(BallPrediction prediction) {
        ballPredictions.add(prediction);
    }
}
