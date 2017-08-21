package tarehart.rlbot.planning;

import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.steps.BlindStep;
import tarehart.rlbot.steps.ChaseBallStep;
import tarehart.rlbot.steps.GetBoostStep;
import tarehart.rlbot.steps.LandGracefullyStep;

import java.time.Duration;

public class SetPieces {

    public static Plan frontFlip() {

        return new Plan()
                .withStep(new BlindStep(
                        new AgentOutput()
                                .withJump(true)
                                .withAcceleration(1),
                        Duration.ofMillis(50)))
                .withStep(new BlindStep(
                        new AgentOutput()
                                .withAcceleration(1),
                        Duration.ofMillis(50)
                ))
                .withStep(new BlindStep(
                        new AgentOutput()
                                .withJump(true)
                                .withAcceleration(1)
                                .withPitch(-1),
                        Duration.ofMillis(50)));
    }

    public static Plan chaseBall() {
        return new Plan()
                .withStep(new ChaseBallStep(Duration.ofSeconds(5)));
    }

    public static Plan getBoostAndAerial() {
        return new Plan()
                .withStep(new GetBoostStep())
                .withSubPlan(performAerial());
    }

    public static Plan performAerial() {
        return new Plan()
                .withStep(new ChaseBallStep(Duration.ofSeconds(2)))
                .withStep(new BlindStep(
                        new AgentOutput()
                            .withJump(true)
                            .withPitch(1),
                        Duration.ofMillis(320)
                ))
                .withStep(new BlindStep(
                        new AgentOutput()
                            .withJump(true)
                            .withPitch(-1)
                            .withBoost(true),
                        Duration.ofMillis(320)
                ))
                .withStep(new BlindStep(
                        new AgentOutput()
                            .withJump(true)
                            .withBoost(true),
                        Duration.ofMillis(500)
                ))
                .withStep(new BlindStep(
                        new AgentOutput()
                            .withJump(false)
                            .withBoost(true),
                        Duration.ofMillis(50)
                ))
                .withStep(new BlindStep(
                        new AgentOutput()
                            .withJump(true)
                            .withPitch(-1),
                        Duration.ofMillis(50)
                ))
                .withStep(new BlindStep(
                        new AgentOutput(),
                        Duration.ofMillis(400)
                ))
                .withStep(new LandGracefullyStep());
    }

}
