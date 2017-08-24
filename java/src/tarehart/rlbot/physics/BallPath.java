package tarehart.rlbot.physics;

import mikera.vectorz.Vector3;
import tarehart.rlbot.math.SpaceTime;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class BallPath {

    ArrayList<SpaceTime> path = new ArrayList<>();
    private Vector3 finalVelocity;
    private LocalDateTime finalVelocityTime;

    public void addSlice(SpaceTime spaceTime) {
        path.add(spaceTime);
    }

    public Optional<Vector3> getSpace(LocalDateTime time) {
        if (time.isBefore(path.get(0).time) || time.isAfter(path.get(path.size() - 1).time)) {
            return Optional.empty();
        }

        for (int i = 1; i < path.size(); i++) {
            SpaceTime spt = path.get(i);
            if (spt.time.isAfter(time)) {
                SpaceTime previous = path.get(i - 1);
                double tweenPoint = Duration.between(previous.time, time).toMillis() * 1.0 / Duration.between(previous.time, spt.time).toMillis();
                Vector3 prevToNext = (Vector3) spt.space.subCopy(previous.space);
                prevToNext.scale(tweenPoint);
                return Optional.of(previous.space.addCopy(prevToNext));
            }
        }

        return Optional.of(path.get(path.size() - 1).space);
    }

    public SpaceTime getEndpoint() {
        return path.get(path.size() - 1);
    }

    public void setFinalVelocity(Vector3 velocity, LocalDateTime time) {
        finalVelocity = velocity;
        finalVelocityTime = time;
    }

    public boolean canContinueSimulation() {
        return finalVelocityTime != null && !path.isEmpty() && path.get(path.size() - 1).time.isEqual(finalVelocityTime);
    }

    public Vector3 getFinalVelocity() {
        return finalVelocity;
    }
}
