package tarehart.rlbot.physics;

import tarehart.rlbot.math.DistanceTimeSpeed;
import tarehart.rlbot.math.TimeUtil;
import tarehart.rlbot.planning.AccelerationModel;
import tarehart.rlbot.planning.StrikeProfile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DistancePlot {

    ArrayList<DistanceTimeSpeed> plot = new ArrayList<>();

    public DistancePlot(DistanceTimeSpeed start) {
        plot.add(start);
    }

    public void addSlice(DistanceTimeSpeed dts) {
        plot.add(dts);
    }

    public List<DistanceTimeSpeed> getSlices() {
        return plot;
    }

    public Optional<DistanceTimeSpeed> getMotionAt(LocalDateTime time) {
        if (time.isBefore(plot.get(0).getTime()) || time.isAfter(plot.get(plot.size() - 1).getTime())) {
            return Optional.empty();
        }

        for (int i = 0; i < plot.size() - 1; i++) {
            DistanceTimeSpeed current = plot.get(i);
            DistanceTimeSpeed next = plot.get(i + 1);
            if (next.getTime().isAfter(time)) {

                long simulationStepMillis = Duration.between(current.getTime(), next.getTime()).toMillis();
                double tweenPoint = Duration.between(current.getTime(), time).toMillis() * 1.0 / simulationStepMillis;
                double distance = (1 - tweenPoint) * current.distance + tweenPoint * next.distance;
                double speed = (1 - tweenPoint) * current.speed + tweenPoint * next.speed;
                return Optional.of(new DistanceTimeSpeed(distance, time, speed));
            }
        }

        return Optional.of(plot.get(plot.size() - 1));
    }

    public Optional<DistanceTimeSpeed> getMotionAt(double distance) {

        for (int i = 0; i < plot.size() - 1; i++) {
            DistanceTimeSpeed current = plot.get(i);
            DistanceTimeSpeed next = plot.get(i + 1);
            if (next.distance > distance) {
                double stepSeconds = TimeUtil.secondsBetween(current.getTime(), next.getTime());
                double tweenPoint = (distance - current.distance) / (next.distance - current.distance);
                LocalDateTime moment = current.getTime().plus(TimeUtil.toDuration(stepSeconds * tweenPoint));
                double speed = (1 - tweenPoint) * current.speed + tweenPoint * next.speed;
                return Optional.of(new DistanceTimeSpeed(distance, moment, speed));
            }
        }
        return Optional.empty();
    }

    public Optional<Double> getTravelTime(double distance) {
        Optional<DistanceTimeSpeed> motionAt = getMotionAt(distance);
        return motionAt.map(distanceTimeSpeed -> TimeUtil.secondsBetween(plot.get(0).getTime(), distanceTimeSpeed.getTime()));
    }

    public Optional<DistanceTimeSpeed> getMotionAfterStrike(LocalDateTime startTime, LocalDateTime momentAfterStrike, double speedupSeconds, double speedBoost) {

        if (speedupSeconds == 0 || speedBoost == 0) {
            return getMotionAt(momentAfterStrike);
        }

        double totalSeconds = TimeUtil.secondsBetween(startTime, momentAfterStrike);
        if (totalSeconds < speedupSeconds) {
            // Not enough time for a full strike.
            double beginningSpeed = plot.get(0).speed;
            double realizedBoost = speedBoost * totalSeconds / speedupSeconds;
            double endSpeed = Math.min(realizedBoost, AccelerationModel.SUPERSONIC_SPEED);
            double averageSpeed = beginningSpeed + endSpeed / 2;

            return Optional.of(new DistanceTimeSpeed(averageSpeed * totalSeconds, momentAfterStrike, endSpeed));
        }

        LocalDateTime momentBeforeStrike = momentAfterStrike.minus(TimeUtil.toDuration(speedupSeconds));
        Optional<DistanceTimeSpeed> dtsOption = getMotionAt(momentBeforeStrike);

        if (dtsOption.isPresent()) {
            DistanceTimeSpeed dts = dtsOption.get();
            double beginningSpeed = dts.speed;
            double endSpeed = Math.min(speedBoost, AccelerationModel.SUPERSONIC_SPEED);
            double averageSpeed = beginningSpeed + endSpeed / 2;

            return Optional.of(new DistanceTimeSpeed(dts.distance + averageSpeed * speedupSeconds, momentAfterStrike, endSpeed));
        } else {
            // We ran out of data in the distance plot.
            return Optional.empty();
        }
    }
}
