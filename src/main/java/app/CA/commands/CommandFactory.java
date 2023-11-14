package app.CA.commands;

import app.Models.PeerDB;
import app.Command;

import java.util.Map;
import java.util.Properties;

public class CommandFactory {
    public static Command getCommand(String commandName, Map<String, PeerDB> peerDBMap, Properties properties) {
        switch (commandName) {
            case "registerPeer":
                return new RegisterPeerCommand(peerDBMap);
            case "registerKey":
                return new RegisterKeyCommand(peerDBMap, properties);
            case "fetchKey":
                return new FetchKeyCommand(peerDBMap);
            default:
                return new IllegalCommand();
        }
    }
}
