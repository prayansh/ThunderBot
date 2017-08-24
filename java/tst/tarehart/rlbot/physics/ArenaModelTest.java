package tarehart.rlbot.physics;


import mikera.vectorz.Vector3;
import org.junit.Test;

import javax.vecmath.Vector3f;
import java.time.LocalDateTime;

public class ArenaModelTest {

    @Test
    public void testConstruct() {
        ArenaModel model = new ArenaModel();
    }


    @Test
    public void testSimulate() {
        ArenaModel model = new ArenaModel();
        BallPath ballPath = model.simulateBall(new Vector3(0, 0, 20), new Vector3(5, 60, -10), LocalDateTime.now().plusSeconds(3));
        System.out.println(ballPath.getEndpoint());
    }

}