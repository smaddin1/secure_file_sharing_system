package app.KDS.commands;

import app.Models.Payloads.FetchKeyResponsePayload;
import app.Models.Payloads.Payload;
import app.Models.Payloads.Peer.FetchKeyPayload;
import app.Models.Payloads.ResponsePayload;
import app.Models.PeerDB;
import app.Command;
import app.constants.KeyManager;
import app.utils.RSA;

import java.util.Map;

import static app.constants.Constants.TerminalColors.*;
import static app.constants.Constants.TerminalColors.ANSI_RESET;

public class FetchKeyCommand implements Command {
    private Map<String, PeerDB> peerDBMap;

    public FetchKeyCommand(Map<String, PeerDB> peerDBMap) {
        this.peerDBMap = peerDBMap;
    }

    @Override
    public ResponsePayload execute(Payload payload) throws Exception {
        FetchKeyPayload fetchKeyPayload = (FetchKeyPayload) payload;
        String fetchKeyOf = fetchKeyPayload.getRequestingPeerId();
        System.out.println(ANSI_BLUE + "Fetching Key for peer " + fetchKeyOf + ANSI_RESET);
        byte[] keyBytes = null;
        int statusCode;
        String message;

        if (peerDBMap.containsKey(fetchKeyOf)) {
            PeerDB peerDBItem = peerDBMap.get(fetchKeyOf);
            statusCode = 200;
            message = "CA: ACK: Handshake Successful!";
            keyBytes = RSA.encrypt(peerDBItem.getKey().getEncoded(), KeyManager.getPrivateKey());
            if (peerDBItem.isActive()) {
                System.out.println(ANSI_RED + String.format("%s inactive. Sharing the key anyway", fetchKeyOf) + ANSI_RESET);
            }
        } else {
            statusCode = 404;
            message = "CA: Peer not found";
        }

        return new FetchKeyResponsePayload.Builder()
            .setStatusCode(statusCode)
            .setMessage(message)
            .setKey(keyBytes)
            .build();
    }
}
