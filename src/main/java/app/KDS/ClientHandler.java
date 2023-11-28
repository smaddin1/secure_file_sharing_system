package app.KDS;

import app.Command;
import app.KDS.commands.CommandFactory;
import app.Models.Payloads.*;
import app.Models.PeerDB;
import app.Models.PeerInfo;
import app.ServerConfigurator;
import app.utils.AES;
import app.utils.CObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static app.constants.Constants.TerminalColors.*;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private ObjectInputStream clientReader;
    private ObjectOutputStream clientWriter;
    private PeerInfo peerInfo;
    private static final Map<String, PeerDB> peerDBMap = new ConcurrentHashMap<>();
    private static ServerConfigurator configurator;

    public ClientHandler(Socket clientSocket, ServerConfigurator configurator) {
        this.clientSocket = clientSocket;
        this.configurator = configurator;
    }

    @Override
    public void run() {
        try {
            System.out.println(ANSI_BLUE + "Thread started: " + Thread.currentThread() + "\n" + ANSI_RESET);

            clientWriter = new ObjectOutputStream(clientSocket.getOutputStream());
            clientReader = new ObjectInputStream(clientSocket.getInputStream());

            Object clientInput;
            while ((clientInput = clientReader.readObject()) != null) {
                PeerInfo peerInfo = null;
                Payload payload = null;
                if (clientInput instanceof EncryptedPayload encryptedPayload) {
                    peerInfo = encryptedPayload.getPeerInfo();
                    byte[] decryptedData = AES.decrypt(peerDBMap.get(peerInfo.getPeer_id()).getKey(), encryptedPayload.getData());
                    payload = (Payload) CObject.bytesToObject(decryptedData);
                } else if (clientInput instanceof Payload) {
                    payload = (Payload) clientInput;
                    peerInfo = payload.getPeerInfo();
                }
                this.peerInfo = peerInfo;

                if (payload != null) {
                    ResponsePayload response = processInput(payload);
                    clientWriter.writeObject(response);
                    clientWriter.flush();
                }
            }
        } catch (IOException e) {
            if (e.getMessage() != null) {
                System.out.println(ANSI_RED + "IOException: " + e.getMessage() + ANSI_RESET);
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            System.out.println(ANSI_RED + "ClassNotFoundException: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(ANSI_RED + "Exception: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        }
    }

    public static ResponsePayload processInput(Payload clientPayload) throws Exception {
        PeerInfo peerInfo = clientPayload.getPeerInfo();
        String peer_id = peerInfo.getPeer_id();
        Command command = CommandFactory.getCommand(clientPayload.getCommand().split(" ", 2)[0], peerDBMap, configurator);

        System.out.println(ANSI_BLUE + "Serving Peer: " + peer_id);
        System.out.println("Executing: " + clientPayload.getCommand() + ANSI_RESET + "\n");

        return command.execute(clientPayload);
    }
}
