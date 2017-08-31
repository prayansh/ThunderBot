package tarehart.rlbot.tuning;

import com.google.gson.Gson;
import mikera.vectorz.Vector3;
import org.junit.Assert;
import org.junit.Test;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class PredictedVsActualTest {


    private static final double THRESHOLD = 1;
    private ArenaModel arenaModel = new ArenaModel();


    private BallPath readRecording(String filename) {
        InputStream in = getClass().getResourceAsStream("/tarehart/rlbot/tuning/ballrecordings/" + filename);
        Scanner s = new Scanner(in).useDelimiter("\\A");
        String content = s.hasNext() ? s.next() : "";

        Gson gson = new Gson();
        return gson.fromJson(content, BallPath.class);
    }

    @Test
    public void testCornerAngle() throws UnsupportedEncodingException {
        testFile("corner-angle.json");
    }


    private void testFile(String filename) throws UnsupportedEncodingException {

        BallPath actualPath = readRecording(filename);
        actualPath = finesseActualPath(actualPath);
        BallPath predictedPath = makePrediction(actualPath);
        // (-73.29997, 65.447556, 4.5342107) after first time step

        List<SpaceTimeVelocity> actual = actualPath.getSlices();
        for (int i = 0; i < actual.size(); i++) {
            if (i < 20) {
                Vector3 velocity = actual.get(i).getVelocity();
                double speedNow = velocity.magnitude();

                Vector3 velocityAfter = actual.get(i+1).getVelocity();
                double speedNext = velocityAfter.magnitude();

                double drag = speedNext / speedNow;
                double dragPerSpeed = drag / speedNow;
                System.out.println(String.format("Velocity: %s Speed: %s Drag: %s DragPerSpeed: %s", velocity, speedNow, drag, dragPerSpeed));
            }
        }

        List<SpaceTimeVelocity> predicted = predictedPath.getSlices();

        List<SpaceTimeVelocity> actualTrimmed = new ArrayList<>(predicted.size());

        for (int i = 0; i < predicted.size() - 1; i++) {
            SpaceTimeVelocity spaceTimeVelocity = actualPath.getMotionAt(predicted.get(i).getTime()).get();
            actualTrimmed.add(spaceTimeVelocity);

        }
        actualTrimmed.add(actualPath.getEndpoint());


        System.out.println("Starting velocity: " + actualPath.getStartPoint().getVelocity());
        double secondsElapsedActual = Duration.between(actualTrimmed.get(0).getTime(), actualTrimmed.get(1).getTime()).toMillis() / 1000.0;
        double secondsElapsedPredicted = Duration.between(actualTrimmed.get(0).getTime(), actualTrimmed.get(1).getTime()).toMillis() / 1000.0;
        Vector3 actualVelApparent = (Vector3) actualTrimmed.get(1).getSpace().subCopy(actualTrimmed.get(0).getSpace()).scaleCopy(1 / secondsElapsedActual);
        Vector3 predictedVelApparent = (Vector3) predicted.get(1).getSpace().subCopy(predicted.get(0).getSpace()).scaleCopy(1 / secondsElapsedPredicted);
        System.out.println("Apparent from actual: " + actualVelApparent);
        System.out.println("Apparent from predicted: " + predictedVelApparent);

        // 0.89623108669 of original velocity after .1 seconds?? (x)
        // 0.89619423608 (y)
        // 0.86549176933 (z)


        for (int i = 0; i < predicted.size(); i++) {

            Vector3 actualSlice = actualTrimmed.get(i).getSpace();
            Vector3 actualToPredicted = (Vector3) predicted.get(i).getSpace().subCopy(actualSlice);
            double error = actualToPredicted.magnitude();
            if (error > THRESHOLD) {
                Duration duration = Duration.between(actualTrimmed.get(0).getTime(), actualTrimmed.get(i).getTime());
                double seconds = duration.toMillis() / 1000.0;
                Assert.fail(String.format("Diverged to %.2f after %.2f seconds!", error, seconds));
            }
        }
    }

    private BallPath finesseActualPath(BallPath actualPath) {
        Optional<SpaceTimeVelocity> newStart = actualPath.getMotionAt(actualPath.getStartPoint().getTime().plus(Duration.ofMillis(100)));
        BallPath finessed = new BallPath(newStart.get());
        List<SpaceTimeVelocity> slices = actualPath.getSlices();
        for (int i = 0; i < actualPath.getSlices().size(); i++) {
            if (slices.get(i).getTime().isAfter(newStart.get().getTime())) {
                finessed.addSlice(slices.get(i));
            }
        }
        return finessed;
    }

    private BallPath makePrediction(BallPath backWallActual) {
        return arenaModel.simulateBall(backWallActual.getStartPoint(), backWallActual.getEndpoint().getTime());
    }

}
