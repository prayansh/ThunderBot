package tarehart.rlbot.physics;


import mikera.vectorz.Vector3;
import org.junit.Test;

import javax.vecmath.Vector3f;
import java.time.Duration;
import java.time.LocalDateTime;

public class ArenaModelTest {

    @Test
    public void testConstruct() {
        ArenaModel model = new ArenaModel();
    }


    @Test
    public void testSimulate() {
        ArenaModel model = new ArenaModel();
        BallPath ballPath = model.simulateBall(new Vector3(0, 0, 20), new Vector3(5, 60, -10), LocalDateTime.now(), Duration.ofSeconds(3));
        System.out.println(ballPath.getEndpoint());
    }

    @Test
    public void testFallDownOnRail() {
        ArenaModel model = new ArenaModel();
        BallPath ballPath = model.simulateBall(new Vector3(0, ArenaModel.BACK_WALL - ArenaModel.BALL_RADIUS, 20), new Vector3(0, 0, 0), LocalDateTime.now(), Duration.ofSeconds(3));
        System.out.println(ballPath.getEndpoint());
    }

}