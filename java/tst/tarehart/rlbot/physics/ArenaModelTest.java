package tarehart.rlbot.physics;


import mikera.vectorz.Vector3;
import org.junit.Assert;
import org.junit.Test;
import tarehart.rlbot.math.SpaceTimeVelocity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

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
    public void testFallNextToBackWall() {
        ArenaModel model = new ArenaModel();
        float nextToBackWall = ArenaModel.BACK_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new Vector3(0, nextToBackWall, 30), new Vector3(0, 0, 0), LocalDateTime.now(), Duration.ofSeconds(1));
        System.out.println(ballPath.getEndpoint());
        Assert.assertEquals(nextToBackWall, ballPath.getEndpoint().space.y, .001);
    }

    @Test
    public void testFallToRailNextToBackWall() {
        ArenaModel model = new ArenaModel();
        float nextToBackWall = ArenaModel.BACK_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new Vector3(0, nextToBackWall, 30), new Vector3(0, 0, 0), LocalDateTime.now(), Duration.ofSeconds(4));
        System.out.println(nextToBackWall - ballPath.getEndpoint().space.y);
        Assert.assertTrue(nextToBackWall - ballPath.getEndpoint().space.y > 10);
    }

    @Test
    public void testFallToRailNextToSideWall() {
        ArenaModel model = new ArenaModel();
        float nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new Vector3(nextToSideWall, 0, 30), new Vector3(0, 0, 0), LocalDateTime.now(), Duration.ofSeconds(4));
        System.out.println(nextToSideWall - ballPath.getEndpoint().space.x);
        Assert.assertTrue(nextToSideWall - ballPath.getEndpoint().space.x > 10);
    }

    @Test
    public void testFallNextToSideWall() {
        ArenaModel model = new ArenaModel();
        float nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new Vector3(nextToSideWall, 0, 30), new Vector3(0, 0, 0), LocalDateTime.now(), Duration.ofSeconds(1));
        System.out.println(ballPath.getEndpoint());
        Assert.assertEquals(nextToSideWall, ballPath.getEndpoint().space.x, .001);
    }

    @Test
    public void testBounceOffSideWall() {
        ArenaModel model = new ArenaModel();
        float nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new Vector3(nextToSideWall - 10, 0, 30), new Vector3(20, 0, 0), LocalDateTime.now(), Duration.ofSeconds(1));
        System.out.println(ballPath.getEndpoint());
        Assert.assertEquals(0, ballPath.getEndpoint().space.y, .001);
        Assert.assertTrue(ballPath.getFinalVelocity().x < -10);
        Assert.assertTrue(ballPath.getFinalVelocity().x > -20);

        Optional<SpaceTimeVelocity> motionAfterBounce = ballPath.getMotionAfterWallBounce(1);
        Assert.assertTrue(motionAfterBounce.isPresent());
        Assert.assertEquals(nextToSideWall, motionAfterBounce.get().getSpace().x, 3);
    }

    @Test
    public void testBounceOffSideWallFromCenter() {
        ArenaModel model = new ArenaModel();
        float nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new Vector3(0, 0, 30), new Vector3(60, 0, 5), LocalDateTime.now(), Duration.ofSeconds(2));
        System.out.println(ballPath.getEndpoint());
        Assert.assertEquals(0, ballPath.getEndpoint().space.y, .001);
        Assert.assertTrue(ballPath.getFinalVelocity().x < -10);
        Assert.assertTrue(ballPath.getFinalVelocity().x > -60);

        Optional<SpaceTimeVelocity> motionAfterBounce = ballPath.getMotionAfterWallBounce(1);
        Assert.assertTrue(motionAfterBounce.isPresent());
        Assert.assertEquals(nextToSideWall, motionAfterBounce.get().getSpace().x, 3);
    }

    @Test
    public void testBounceOffCornerAngle() {
        ArenaModel model = new ArenaModel();
        float nextToSideWall = ArenaModel.SIDE_WALL - ArenaModel.BALL_RADIUS;
        BallPath ballPath = model.simulateBall(new Vector3(nextToSideWall, ArenaModel.BACK_WALL * .7, 30), new Vector3(0, 30, 0), LocalDateTime.now(), Duration.ofSeconds(3));
        System.out.println(ballPath.getEndpoint());
        Assert.assertTrue(nextToSideWall - ballPath.getEndpoint().space.x > 10);
    }

}