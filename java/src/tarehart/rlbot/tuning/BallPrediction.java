package tarehart.rlbot.tuning;

import mikera.vectorz.Vector3;

import java.time.LocalDateTime;

public class BallPrediction {

    public LocalDateTime creationDate;
    public LocalDateTime predictedMoment;
    public Vector3 predictedLocation;

    public BallPrediction(Vector3 predictedLocation, LocalDateTime predictedMoment) {
        this.creationDate = LocalDateTime.now();
        this.predictedLocation = predictedLocation;
        this.predictedMoment = predictedMoment;
    }

}
