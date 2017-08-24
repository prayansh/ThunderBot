package tarehart.rlbot;

import tarehart.rlbot.ui.Readout;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Agent {

    private Map<Bot.Team, Bot> bots = new HashMap<>();

    public int[] getOutputVector(ArrayList<ArrayList<Double>> input, String teamString) {

        Bot.Team team = Bot.Team.valueOf(teamString.toUpperCase());

        AgentInput translatedInput = new AgentInput(input, team);

        if (!bots.containsKey(team)) {
            bots.put(team, new Bot(team));
        }

        Bot bot = bots.get(team);
        AgentOutput output = bot.processInput(translatedInput);
        int[] outputForPython = output.toPython();
        return outputForPython;
    }
}
