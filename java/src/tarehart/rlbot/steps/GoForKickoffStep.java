package tarehart.rlbot.steps;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.DistancePlot;
import tarehart.rlbot.planning.*;
import tarehart.rlbot.steps.strikes.DirectedNoseHitStep;
import tarehart.rlbot.steps.strikes.KickAtEnemyGoal;
import tarehart.rlbot.tuning.BotLog;

import java.time.Duration;
import java.util.Optional;

public class GoForKickoffStep implements Step {

    private static final double DIAGONAL_KICKOFF_X = 40.98;
    private static final double CHEATER_KICKOFF_X = 5.09;
    private static final double CENTER_KICKOFF_X = 0;
    private static final double WIGGLE_ROOM = 2;
    private static final double CHEATIN_BOOST_Y = 58;

    private Plan plan;
    private KickoffType kickoffType;

    private enum KickoffType {
        CENTER,
        CHEATIN,
        SLANTERD,
        UNKNOWN
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null && !plan.isComplete()) {
            return plan.getOutput(input);
        }

        if (VectorUtil.flatten(input.ballPosition).magnitudeSquared() > 0) {
            return Optional.empty();
        }

        CarData car = input.getMyCarData();

        if (kickoffType == null) {
            kickoffType = getKickoffType(car);
        }

        double distance = car.position.magnitude();
        if (distance < 14) {
            plan = SetPieces.frontFlip();
            plan.begin();
            return plan.getOutput(input);
        }

        double ySide = Math.signum(car.position.y);

        if (kickoffType == KickoffType.CHEATIN && Math.abs(car.position.y) > CHEATIN_BOOST_Y + 10) {
            // Steer toward boost
            Vector2 target = new Vector2(0, ySide * CHEATIN_BOOST_Y);
            return Optional.of(SteerUtil.steerTowardGroundPosition(car, target));
        } else {
            DistancePlot plot = AccelerationModel.simulateAcceleration(car, Duration.ofSeconds(5), car.boost + 15); // We'll pickup a boost
            SteerPlan planForCircleTurn = SteerUtil.getPlanForCircleTurn(car, plot, new Vector2(0, ySide * 10), new Vector2(0, -ySide));
            return Optional.of(planForCircleTurn.immediateSteer);
        }
    }

    private KickoffType getKickoffType(CarData car) {
        double xPosition = car.position.x;
        if (getNumberDistance(CENTER_KICKOFF_X, xPosition) < WIGGLE_ROOM){
            BotLog.println("it be center" , car.team);
            return KickoffType.CENTER;
        }

        if (getNumberDistance(CHEATER_KICKOFF_X, Math.abs(xPosition)) < WIGGLE_ROOM){
            BotLog.println("it be cheatin" , car.team);
            return KickoffType.CHEATIN;
        }

        if (getNumberDistance(DIAGONAL_KICKOFF_X , Math.abs(xPosition)) < WIGGLE_ROOM){
            BotLog.println("it be slanterd" , car.team);
            return KickoffType.SLANTERD;
        }

        BotLog.println("what on earth" , car.team);
        return KickoffType.UNKNOWN;
    }

    private static double getNumberDistance(double first, double second){
        return Math.abs(first - second);
    }

    @Override
    public boolean isBlindlyComplete() {
        return false;
    }

    @Override
    public void begin() {
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return Plan.concatSituation("Going for kickoff", plan);
    }
}
