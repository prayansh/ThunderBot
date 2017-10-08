package tarehart.rlbot;

import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SteerUtil;
import tarehart.rlbot.steps.*;
import tarehart.rlbot.steps.defense.GetOnDefenseStep;
import tarehart.rlbot.steps.defense.ThreatAssessor;
import tarehart.rlbot.steps.defense.WhatASaveStep;
import tarehart.rlbot.steps.landing.LandGracefullyStep;
import tarehart.rlbot.steps.strikes.*;
import tarehart.rlbot.steps.wall.DescendFromWallStep;
import tarehart.rlbot.steps.wall.MountWallStep;
import tarehart.rlbot.steps.wall.WallTouchStep;
import tarehart.rlbot.tuning.BallTelemetry;
import tarehart.rlbot.tuning.BotLog;
import tarehart.rlbot.ui.Readout;

import javax.swing.*;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class LatencyBot extends Bot {

    public LatencyBot(Team team) {
        super(team);
    }

    @Override
    protected AgentOutput getOutput(AgentInput input) {

        if (VectorUtil.flatDistance(input.ballPosition, new Vector3()) > 0) {
            return new AgentOutput().withJump();
        }

        final CarData car = input.getMyCarData();
        return SteerUtil.steerTowardGroundPosition(car, input.ballPosition.addCopy(new Vector3(20, 0, 0)));
    }
}
