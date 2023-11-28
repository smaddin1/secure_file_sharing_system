package app.KDS.commands;

import app.Models.PeerDB;
import app.Command;
import app.ServerConfigurator;

import java.util.Map;
import java.util.Properties;

public class CommandFactory {
    public static Command getCommand(String commandName, Map<String, PeerDB> peerDBMap, ServerConfigurator configurator) {
        switch (commandName) {
            case "registerPeer":
                return new RegisterPeerCommand(peerDBMap);
            case "registerKey":
                return new RegisterKeyCommand(peerDBMap, configurator);
            case "fetchKey":
                return new FetchKeyCommand(peerDBMap);
            default:
                return new IllegalCommand();
        }
    }
}
