package tarehart.rlbot.ui;

import tarehart.rlbot.Bot;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class StatusSummary {
    private JPanel rootPanel;
    private JButton blueBtn;
    private JLabel blueStatus;
    private JLabel orangeStatus;
    private JButton orangeBtn;
    private JLabel portLbl;
    private JPanel bluePanel;
    private JPanel orangePanel;

    private Map<Bot.Team, JFrame> debugPanels = new HashMap<>();

    public StatusSummary() {

        blueStatus.setText("Asleep");
        orangeStatus.setText("Asleep");

        blueBtn.addActionListener(e -> showDebugForm(Bot.Team.BLUE));
        orangeBtn.addActionListener(e -> showDebugForm(Bot.Team.ORANGE));
    }

    public void showDebugForm(Bot.Team team) {
        JFrame frame = debugPanels.get(team);
        frame.pack();
        frame.setVisible(true);
    }

    public void markTeamRunning(Bot.Team team, JFrame debugPanel) {

        JLabel status;
        JPanel statusPnl;
        Color color;
        JButton btn;

        if (team == Bot.Team.BLUE) {
            status = blueStatus;
            statusPnl = bluePanel;
            color = new Color(187, 212, 255);
            btn = blueBtn;
        } else {
            status = orangeStatus;
            statusPnl = orangePanel;
            color = new Color(250, 222, 191);
            btn = orangeBtn;
        }

        status.setText("Running");
        statusPnl.setBackground(color);
        btn.setEnabled(true);

        debugPanels.put(team, debugPanel);
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public void setPort(int port) {
        portLbl.setText(String.format("Port %s", port));
    }
}
