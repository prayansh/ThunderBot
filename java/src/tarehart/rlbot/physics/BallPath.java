package tarehart.rlbot.physics;

import mikera.vectorz.Vector2;
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

    /**
     * Bounce counting starts at 1.
     *
     * 0 is not a valid input.
     */
    public Optional<SpaceTimeVelocity> getMotionAfterWallBounce(int targetBounce) {

        assert targetBounce > 0;

        Vector3 previousVelocity = null;
        int numBounces = 0;

        for (int i = 1; i < path.size(); i++) {
            SpaceTime spt = path.get(i);
            SpaceTime previous = path.get(i - 1);

            Vector3 currentVelocity = getVelocity(previous, spt);
            if (previousVelocity != null) {
                if (isWallBounce(previousVelocity, currentVelocity)) {
                    numBounces++;
                }

                if (numBounces == targetBounce) {
                    if (path.size() == i + 1) {
                        return Optional.empty();
                    }
                    SpaceTime next = path.get(i + 1);
                    return Optional.of(new SpaceTimeVelocity(next, getVelocity(spt, next)));
                }
            }

            previousVelocity = currentVelocity;
        }

        return Optional.empty();
    }

    private boolean isWallBounce(Vector3 previousVelocity, Vector3 currentVelocity) {
        Vector2 prev = new Vector2(previousVelocity.x, previousVelocity.y);
        Vector2 curr = new Vector2(currentVelocity.x, currentVelocity.y);

        prev.normalise();
        curr.normalise();

        return prev.dotProduct(curr) < .95;
    }

    private boolean isFloorBounce(Vector3 previousVelocity, Vector3 currentVelocity) {
        return previousVelocity.z < 0 && currentVelocity.z > 0;
    }

    private Vector3 getVelocity(SpaceTime before, SpaceTime after) {
        long millisBetween = Duration.between(before.time, after.time).toMillis();
        double secondsBetween = millisBetween / 1000.0;
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


    public Optional<SpaceTimeVelocity> getLanding(LocalDateTime startOfSearch) {
        Vector3 previousVelocity = null;

        for (int i = 1; i < path.size(); i++) {
            SpaceTime spt = path.get(i);

            if (spt.time.isBefore(startOfSearch)) {
                continue;
            }

            SpaceTime previous = path.get(i - 1);

            Vector3 currentVelocity = getVelocity(previous, spt);
            if (previousVelocity != null) {
                if (isFloorBounce(previousVelocity, currentVelocity)) {
                    if (path.size() == i + 1) {
                        return Optional.empty();
                    }

                    double floorGapOfPrev = previous.space.z - ArenaModel.BALL_RADIUS;
                    double floorGapOfCurrent = spt.space.z - ArenaModel.BALL_RADIUS;

                    SpaceTime bouncePosition = new SpaceTime(new Vector3(spt.space.x, spt.space.y, ArenaModel.BALL_RADIUS), spt.time);
                    if (floorGapOfPrev < floorGapOfCurrent) {
                        // TODO: consider interpolating instead of just picking the more accurate.
                        bouncePosition.space.x = previous.space.x;
                        bouncePosition.space.y = previous.space.y;
                        bouncePosition.time = previous.time;
                    }

                    SpaceTime next = path.get(i + 1);
                    return Optional.of(new SpaceTimeVelocity(bouncePosition, getVelocity(spt, next)));
                }
            }
            previousVelocity = currentVelocity;

        }

        return Optional.empty();
    }
}
