package tarehart.rlbot.steps;

import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SplineHandle;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.AccelerationModel;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GetBoostStep implements Step {
    private SplineHandle targetLocation = null;

    private static final float MIDFIELD_BOOST_WIDTH = 71.5f;
    private static final float CORNER_BOOST_WIDTH = 61.5f;
    private static final float CORNER_BOOST_DEPTH = 82;

    private static final float S_HNDL = 40;
    private static final float C_HNDL = 20;

    private static final List<SplineHandle> boostLocations = Arrays.asList(
            new SplineHandle(new Vector3(MIDFIELD_BOOST_WIDTH, 0, 0), new Vector3(0, S_HNDL, 0), new Vector3(0, -S_HNDL, 0)),
            new SplineHandle(new Vector3(-MIDFIELD_BOOST_WIDTH, 0, 0), new Vector3(0, S_HNDL, 0), new Vector3(0, -S_HNDL, 0)),
            new SplineHandle(new Vector3(-CORNER_BOOST_WIDTH, -CORNER_BOOST_DEPTH, 0), new Vector3(-C_HNDL, C_HNDL, 0), new Vector3(C_HNDL, -C_HNDL, 0)),
            new SplineHandle(new Vector3(-CORNER_BOOST_WIDTH, CORNER_BOOST_DEPTH, 0), new Vector3(-C_HNDL, -C_HNDL, 0), new Vector3(C_HNDL, C_HNDL, 0)),
            new SplineHandle(new Vector3(CORNER_BOOST_WIDTH, -CORNER_BOOST_DEPTH, 0), new Vector3(C_HNDL, C_HNDL, 0), new Vector3(-C_HNDL, -C_HNDL, 0)),
            new SplineHandle(new Vector3(CORNER_BOOST_WIDTH, CORNER_BOOST_DEPTH, 0), new Vector3(C_HNDL, -C_HNDL, 0), new Vector3(-C_HNDL, -C_HNDL, 0))
    );

    private Plan plan;

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (targetLocation == null) {
            init(input);
        }

        CarData car = input.getMyCarData();

        double distance = SteerUtil.getDistanceFromCar(car, targetLocation.getLocation());

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        if (distance < 3 || !canRun(car)) {
            return Optional.empty();
        } else {

            CarData carData = input.getMyCarData();
            Vector3 myPosition = carData.position;
            Vector3 target = targetLocation.isWithinHandleRange(myPosition) ? targetLocation.getLocation() : targetLocation.getNearestHandle(myPosition);

            Optional<Plan> sensibleFlip = SteerUtil.getSensibleFlip(car, target);
            if (sensibleFlip.isPresent()) {
                BotLog.println("Flipping toward boost", input.team);
                plan = sensibleFlip.get();
                plan.begin();
                return plan.getOutput(input);
            }

            // TODO: use SteerUtil.getThereWithFacing
            return Optional.of(SteerUtil.arcTowardPosition(car, targetLocation));
        }
    }

    private void init(AgentInput input) {
        targetLocation = getTacticalBoostLocation(input);
    }

    private static SplineHandle getTacticalBoostLocation(AgentInput input) {
        SplineHandle nearestLocation = null;
        double minTime = Double.MAX_VALUE;
        CarData carData = input.getMyCarData();
        DistancePlot distancePlot = AccelerationModel.simulateAcceleration(carData, Duration.ofSeconds(4), carData.boost);
        for (SplineHandle loc: boostLocations) {
            Optional<Double> travelSeconds = AccelerationModel.getTravelSeconds(carData, distancePlot, loc.getLocation());
            if (travelSeconds.isPresent() && travelSeconds.get() < minTime) {
                minTime = travelSeconds.get();
                nearestLocation = loc;
            }
        }
        if (minTime < .5) {
            return nearestLocation;
        }

        BallPath ballPath = SteerUtil.predictBallPath(input, input.time, Duration.ofSeconds(3));
        return getNearestBoost(ballPath.getEndpoint().space);
    }

    private static SplineHandle getNearestBoost(Vector3 position) {
        SplineHandle location = null;
        double minDistance = Double.MAX_VALUE;
        for (SplineHandle loc: boostLocations) {
            double distance = position.distance(loc.getLocation());
            if (distance < minDistance) {
                minDistance = distance;
                location = loc;
            }
        }
        return location;
    }


    @Override
    public boolean isBlindlyComplete() {
        return false;
    }

    @Override
    public void begin() {
    }

    public static boolean canRun(CarData car) {
        return car.position.z < 1;
    }

    public static boolean seesOpportunisticBoost(CarData input) {
        SplineHandle location = getNearestBoost(input.position);
        return location.getLocation().distance(input.position) < 20 &&
                Math.abs(SteerUtil.getCorrectionAngleRad(input, location.getLocation())) < Math.PI / 6;

    }

    @Override
    public boolean canInterrupt() {
        return plan == null || plan.canInterrupt();
    }

    @Override
    public String getSituation() {
        return "Going for boost";
    }
}
