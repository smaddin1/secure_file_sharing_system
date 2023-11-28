package app.KDS.commands;

import app.Models.Payloads.InitPayload;
import app.Models.Payloads.Payload;
import app.Models.Payloads.ResponsePayload;
import app.Models.PeerDB;
import app.Models.PeerInfo;
import app.Command;
import app.constants.KeyManager;
import app.utils.AES;
import app.utils.RSA;

import javax.crypto.SecretKey;
import java.util.Map;

public class RegisterPeerCommand implements Command {
    private Map<String, PeerDB> peerDBMap;

    public RegisterPeerCommand(Map<String, PeerDB> peerDBMap) {
        this.peerDBMap = peerDBMap;
    }

    @Override
    public ResponsePayload execute(Payload payload) throws Exception {
        InitPayload initPayload = (InitPayload) payload;
        PeerInfo peerInfo = initPayload.getPeerInfo();
        String peer_id = peerInfo.getPeer_id();
        byte[] keyBytes = RSA.decrypt(initPayload.getKey(), KeyManager.getPrivateKey());
        SecretKey key = AES.getSecretKey(keyBytes);

        PeerDB peerDBItem = new PeerDB(peerInfo, true, key);
        peerDBMap.put(peer_id, peerDBItem);

        String message = String.format("CA: ACK: %s keys registered successfully", peer_id);
        int statusCode = 200;

        return new ResponsePayload.Builder()
            .setStatusCode(statusCode)
            .setMessage(message)
            .build();
    }
}

