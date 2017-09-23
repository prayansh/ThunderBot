package tarehart.rlbot;

import com.google.gson.Gson;
import rlbot.input.PyGameTickPacket;
import tarehart.rlbot.input.Chronometer;
import tarehart.rlbot.input.SpinTracker;
import tarehart.rlbot.math.TimeUtil;

import java.util.HashMap;
import java.util.Map;

public class Agent {

    private Map<Bot.Team, Bot> bots = new HashMap<>();
    private Gson gson = new Gson();
    private Chronometer chronometer = new Chronometer();
    private SpinTracker spinTracker = new SpinTracker();

    public int[] getOutputVector(String packetJson, String teamString) {

        AgentOutput output;

        try {
            Bot.Team team = Bot.Team.valueOf(teamString.toUpperCase());

            PyGameTickPacket packet = gson.fromJson(packetJson, PyGameTickPacket.class);

            chronometer.readInput(packet.gameInfo);
            double elapsedSeconds = TimeUtil.toSeconds(chronometer.getTimeDiff());
            if (elapsedSeconds > 0) {
                spinTracker.readInput(packet, elapsedSeconds);
            }

            AgentInput translatedInput = new AgentInput(packet, team, chronometer.getGameTime(), spinTracker.getSpinList());

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
