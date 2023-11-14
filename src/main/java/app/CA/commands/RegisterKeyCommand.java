package app.CA.commands;

import app.Models.Payloads.InitPayload;
import app.Models.Payloads.Payload;
import app.Models.Payloads.Peer.UpdateKeyPayload;
import app.Models.Payloads.ResponsePayload;
import app.Models.PeerDB;
import app.Models.PeerInfo;
import app.Command;
import app.constants.Commands;
import app.constants.Constants;
import app.constants.KeyManager;
import app.utils.AES;
import app.utils.RSA;

import javax.crypto.SecretKey;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;

import static app.constants.Constants.TerminalColors.*;
import static app.constants.Constants.TerminalColors.ANSI_RESET;

public class RegisterKeyCommand implements Command {
    private Map<String, PeerDB> peerDBMap;
    private Properties properties;

    public RegisterKeyCommand(Map<String, PeerDB> peerDBMap, Properties properties) {
        this.peerDBMap = peerDBMap;
        this.properties = properties;
    }

    @Override
    public ResponsePayload execute(Payload payload) throws Exception {
        InitPayload initPayload = (InitPayload) payload;
        PeerInfo peerInfo = initPayload.getPeerInfo();
        String peer_id = peerInfo.getPeer_id();
        System.out.println(ANSI_BLUE + "Registering key for peer " + peer_id + ANSI_RESET);
        byte[] keyBytes = RSA.decrypt(initPayload.getKey(), KeyManager.getPrivateKey());
        SecretKey key = AES.getSecretKey(keyBytes);
        PeerDB peerDBItem = peerDBMap.get(peer_id);
        peerDBItem.setKey(key);
        peerDBMap.put(peer_id, peerDBItem);
        PeerInfo CAInfo = new PeerInfo("CA", 9000);

        keyBytes = RSA.encrypt(key.getEncoded(), KeyManager.getPrivateKey());
        UpdateKeyPayload updateKeyPayload = new UpdateKeyPayload.Builder()
            .setCommand(Commands.updatePeerKey.name())
            .setPeerInfo(CAInfo)
            .setKey(keyBytes)
            .build();

        int successfullyKeyRegistrationCount = 0;
        StringBuilder keyRegistrationSuccessful = new StringBuilder();
        for (Map.Entry<String, PeerDB> peerDBItem1: peerDBMap.entrySet()) {
            // check if the peer is active and
            // if the current peer is the peer requesting keygen,
            // in that case do not send updated key
            if (peerDBItem1.getValue().isActive() && !peerDBItem1.getKey().equals(peerInfo.getPeer_id())) {
                Socket peerSocket = new Socket(properties.getProperty("IP_ADDRESS"), peerDBItem1.getValue().getPort_no());
                ObjectOutputStream peerWriter = new ObjectOutputStream(peerSocket.getOutputStream());
                ObjectInputStream peerReader = new ObjectInputStream(peerSocket.getInputStream());

                peerWriter.writeObject(updateKeyPayload);
                peerWriter.flush();

                ResponsePayload responsePayload = (ResponsePayload) peerReader.readObject();

                if (Constants.HttpStatus.twoHundredClass.contains(responsePayload.getStatusCode())) {
                    System.out.println(ANSI_BLUE + responsePayload.getMessage() + ANSI_RESET);
                    keyRegistrationSuccessful.append(peerDBItem1.getKey() + ", ");
                    successfullyKeyRegistrationCount += 1;
                } else {
                    System.out.println(ANSI_RED + responsePayload.getMessage() + ANSI_RESET);
                }
                System.out.println();
            }
        }

        String message = successfullyKeyRegistrationCount > 0 ? String.format("CA: ACK: SecretKey registration successfully on %s", keyRegistrationSuccessful.substring(0, keyRegistrationSuccessful.length() - 2)) : String.format("CA: ACK: SecretKey registration failed");
        int statusCode = successfullyKeyRegistrationCount > 0 ? 200 : 400;
        return new ResponsePayload.Builder()
            .setStatusCode(statusCode)
            .setMessage(message)
            .build();
    }
}
