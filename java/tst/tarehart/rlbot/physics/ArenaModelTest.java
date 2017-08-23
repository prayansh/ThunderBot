package tarehart.rlbot.physics;


import org.junit.Test;

import javax.vecmath.Vector3f;

public class ArenaModelTest {

    @Test
    public void testConstruct() {
        ArenaModel model = new ArenaModel();
    }


    @Test
    public void testSimulate() {
        ArenaModel model = new ArenaModel();
        Vector3f finalLocation = model.simulateBall(new Vector3f(0, 0, 20), new Vector3f(5, 60, -10), 3);
        System.out.println(finalLocation);
    }

}