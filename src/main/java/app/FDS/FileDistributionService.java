package app.FDS;

import java.net.*;
import java.io.*;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import app.DatabaseConnectionManager;
import app.ServerConfigurator;
import app.constants.Constants;
import app.constants.KeyManager;
import app.exceptions.KeyCreationException;
import app.utils.RSA;

import static app.constants.Constants.TerminalColors.*;

public class FileDistributionService {
    private static final ServerConfigurator configurator = ServerConfigurator.loadConfiguration();
    private static ServerSocket serverSocket = null;

    public static void generateKeysIfNotExists() throws IOException, NoSuchAlgorithmException {
        File keysFolder = new File(Constants.FilePaths.FDSKeys);

        if (!keysFolder.exists()) {
            keysFolder.mkdir();
        }

        File privateKeyFile = new File(Constants.FilePaths.FDSKeys + "/private.der");
        File publicKeyFile = new File(Constants.FilePaths.FDSKeys + "/public.der");

        if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
            KeyPair keyPair = RSA.generateKeyPair(2048);

            FileOutputStream fos = new FileOutputStream(publicKeyFile.getAbsolutePath());
            fos.write(keyPair.getPublic().getEncoded());
            fos.close();
            configurator.setProperty("FDS_PBK", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));

            fos = new FileOutputStream(privateKeyFile.getAbsolutePath());
            fos.write(keyPair.getPrivate().getEncoded());
            fos.close();
        }
    }

    public static void main(String[] args) {
        try {
            generateKeysIfNotExists();

            DatabaseConnectionManager.establishConnection(configurator);
            new KeyManager(Constants.FilePaths.FDSKeys);

            serverSocket = new ServerSocket(configurator.getIntProperty("FDS_PORT"));
            System.out.println(ANSI_BLUE + "Trying to start File Distribution Server on " + configurator.getProperty("FDS_PORT") + ANSI_RESET);
            TimeUnit.SECONDS.sleep(1);
            System.out.println(ANSI_BLUE + "Server started...\n" + ANSI_RESET);

            MaliciousActivityDetector maliciousActivityDetector = new MaliciousActivityDetector(configurator);
            Thread maliciousActivityDetectorThread = new Thread(maliciousActivityDetector);
            maliciousActivityDetectorThread.start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(ANSI_BLUE + "Client connected: " + clientSocket + ANSI_RESET);

                ClientHandler clientHandler = new ClientHandler(clientSocket, configurator);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println(ANSI_RED + "IOException: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println(ANSI_RED + "InterruptedException: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(ANSI_RED + "Exception: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.out.println(ANSI_RED + "IOException: Error closing server socket: " + e.getMessage() + ANSI_RESET);
                e.printStackTrace();
            }
        }
    }
}

class KeyGeneratorUtility {
    public static void ensureKeyAvailability(ServerConfigurator configurator) throws KeyCreationException, NoSuchAlgorithmException, IOException {
        File keysFolder = new File(Constants.FilePaths.FDSKeys);

        if (!keysFolder.exists()) {
            keysFolder.mkdir();
        }

        File privateKeyFile = new File(Constants.FilePaths.FDSKeys + "/private.der");
        File publicKeyFile = new File(Constants.FilePaths.FDSKeys + "/public.der");

        if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
            KeyPair keyPair = RSA.generateKeyPair(2048);

            FileOutputStream fos = new FileOutputStream(publicKeyFile.getAbsolutePath());
            fos.write(keyPair.getPublic().getEncoded());
            fos.close();
            configurator.setProperty("FDS_PBK", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));

            fos = new FileOutputStream(privateKeyFile.getAbsolutePath());
            fos.write(keyPair.getPrivate().getEncoded());
            fos.close();
        }
    }
}