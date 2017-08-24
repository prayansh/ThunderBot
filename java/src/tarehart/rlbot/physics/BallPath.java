package tarehart.rlbot.physics;

import mikera.vectorz.Vector3;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SpaceTimeVelocity;

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

    public Optional<SpaceTimeVelocity> getMotionAt(LocalDateTime time) {
        if (time.isBefore(path.get(0).time) || time.isAfter(path.get(path.size() - 1).time)) {
            return Optional.empty();
        }

        for (int i = 1; i < path.size(); i++) {
            SpaceTime spt = path.get(i);
            if (spt.time.isAfter(time)) {
                SpaceTime previous = path.get(i - 1);
                long simulationStepMillis = Duration.between(previous.time, spt.time).toMillis();
                double tweenPoint = Duration.between(previous.time, time).toMillis() * 1.0 / simulationStepMillis;
                Vector3 prevToNext = (Vector3) spt.space.subCopy(previous.space);
                Vector3 toTween = (Vector3) prevToNext.scaleCopy(tweenPoint);
                Vector3 space = previous.space.addCopy(toTween);
                Vector3 velocity = getVelocity(previous, spt);
                return Optional.of(new SpaceTimeVelocity(new SpaceTime(space, time), velocity));
            }
        }

        return Optional.of(new SpaceTimeVelocity(getEndpoint(), getFinalVelocity()));
    }

    public Optional<SpaceTimeVelocity> getMotionAfterBounce(int targetBounce) {
        Vector3 previousVelocity = null;
        int numBounces = 0;

        for (int i = 1; i < path.size(); i++) {
            SpaceTime spt = path.get(i);
            SpaceTime previous = path.get(i - 1);

            Vector3 currentVelocity = getVelocity(previous, spt);
            if (previousVelocity == null) {
                previousVelocity = currentVelocity;
                continue;
            }

            if (currentVelocity.dotProduct(previousVelocity) < 0) {
                numBounces++;
            }

            if (numBounces == targetBounce) {
                return Optional.of(new SpaceTimeVelocity(spt, currentVelocity));
            }
        }

        return Optional.empty();
    }

    private Vector3 getVelocity(SpaceTime before, SpaceTime after) {
        long millisBetween = Duration.between(before.time, after.time).toMillis();
        double secondsBetween = millisBetween * 1000;
        Vector3 prevToNext = (Vector3) after.space.subCopy(before.space);
        return (Vector3) prevToNext.scaleCopy(1 / secondsBetween);
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
