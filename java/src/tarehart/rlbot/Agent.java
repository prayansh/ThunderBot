package tarehart.rlbot;

import com.google.gson.Gson;
import rlbot.input.PyGameTickPacket;
import tarehart.rlbot.input.Chronometer;
import tarehart.rlbot.input.SpinTracker;
import tarehart.rlbot.ui.StatusSummary;

import java.util.HashMap;
import java.util.Map;

public class Agent {

    private Map<Bot.Team, Bot> bots = new HashMap<>();
    private Gson gson = new Gson();
    private Chronometer chronometer = new Chronometer();
    private SpinTracker spinTracker = new SpinTracker();
    private StatusSummary statusSummary;

    public Agent(StatusSummary statusSummary) {
        this.statusSummary = statusSummary;
    }

    public int[] getOutputVector(String packetJson, String teamString) {

        AgentOutput output;

        try {
            Bot.Team team = Bot.Team.valueOf(teamString.toUpperCase());

            PyGameTickPacket packet = gson.fromJson(packetJson, PyGameTickPacket.class);

            AgentInput translatedInput = new AgentInput(packet, team, chronometer, spinTracker);

            synchronized (this) {
                if (!bots.containsKey(team)) {
                    ReliefBot bot = new ReliefBot(team);
                    bots.put(team, bot);
                    statusSummary.markTeamRunning(team, bot.getDebugWindow());
                }
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
