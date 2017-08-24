package tarehart.rlbot;

import py4j.GatewayServer;

public class AgentEntryPoint {

    public static final int PORT = 25368;
    private Agent agent;

    public AgentEntryPoint() {
        agent = new Agent();
    }

    public Agent getAgent() {
        return agent;
    }

    public static void main(String[] args) {
        GatewayServer gatewayServer = new GatewayServer(new AgentEntryPoint(), PORT);
        gatewayServer.start();
        System.out.println("Gateway Server Started");
    }

}
