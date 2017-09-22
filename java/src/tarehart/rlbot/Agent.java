package tarehart.rlbot;

import com.google.gson.Gson;
import rlbot.input.PyGameTickPacket;

import java.util.HashMap;
import java.util.Map;

public class Agent {

    private Map<Bot.Team, Bot> bots = new HashMap<>();
    private Gson gson = new Gson();

    public int[] getOutputVector(String packetJson, String teamString) {

        AgentOutput output;

        try {
            Bot.Team team = Bot.Team.valueOf(teamString.toUpperCase());

            PyGameTickPacket packet = gson.fromJson(packetJson, PyGameTickPacket.class);

            AgentInput translatedInput = new AgentInput(packet, team);

            if (!bots.containsKey(team)) {
                bots.put(team, new Bot(team));
            }

            Bot bot = bots.get(team);

            output = bot.processInput(translatedInput);
        } catch (Exception e) {
            e.printStackTrace();
            output = new AgentOutput();
        }
        int[] outputForPython = output.toPython();
        return outputForPython;
    }
}
