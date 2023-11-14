package app.CA;

import app.MongoConnectionManager;
import app.constants.Constants;
import app.constants.KeyManager;
import app.utils.RSA;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static app.constants.Constants.TerminalColors.*;
import static app.constants.Constants.TerminalColors.ANSI_RED;

public class CertificateAuthority {
    private static ServerSocket serverSocket = null;
    static Properties properties = new Properties();
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    private static void loadProperties() {
        try (InputStream input = new FileInputStream("src/main/resources/config.properties")) {
            properties.load(input);
            System.out.println(ANSI_GREEN + "Properties file loaded successfully." + ANSI_RESET);
        } catch (IOException ex) {
            System.out.println(ANSI_RED + "Error loading properties file: " + ex.getMessage() + ANSI_RESET);
            System.exit(1);
        }
    }

    private static void generateKeysIfNotExists() {
        try {
            File keysFolder = new File(Constants.FilePaths.CAKeys);
            if (!keysFolder.exists() && !keysFolder.mkdir()) {
                System.out.println(ANSI_RED + "Failed to create keys directory." + ANSI_RESET);
                return;
            }

            File privateKeyFile = new File(Constants.FilePaths.CAKeys, "private.der");
            File publicKeyFile = new File(Constants.FilePaths.CAKeys, "public.der");

            if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
                KeyPair keyPair = RSA.generateKeyPair(2048);
                try (FileOutputStream publicFos = new FileOutputStream(publicKeyFile);
                     FileOutputStream privateFos = new FileOutputStream(privateKeyFile)) {
                    publicFos.write(keyPair.getPublic().getEncoded());
                    privateFos.write(keyPair.getPrivate().getEncoded());
                    properties.setProperty("CA_PBK", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
                    properties.store(new FileOutputStream("src/main/resources/config.properties"), null);
                    System.out.println(ANSI_GREEN + "Keys generated and saved successfully." + ANSI_RESET);
                }
            }
        } catch (IOException | NoSuchAlgorithmException ex) {
            System.out.println(ANSI_RED + "Error generating keys: " + ex.getMessage() + ANSI_RESET);
        }
    }

    private static void initializeMongoConnection() {
        new MongoConnectionManager(properties.getProperty("CONNECTION_STRING"), properties.getProperty("DATABASE_NAME"));
        System.out.println(ANSI_GREEN + "MongoDB connection initialized." + ANSI_RESET);
    }

    private static void startServer() {
        try {
            serverSocket = new ServerSocket(Integer.parseInt(properties.getProperty("CA_PORT")));
            System.out.println(ANSI_GREEN + "Certificate Authority started on port " + properties.getProperty("CA_PORT") + ANSI_RESET);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println(ANSI_GREEN + "Client connected: " + clientSocket + ANSI_RESET);
                    ClientHandler clientHandler = new ClientHandler(clientSocket, properties);
                    threadPool.submit(clientHandler);
                } catch (IOException ex) {
                    System.out.println(ANSI_RED + "Error accepting client connection: " + ex.getMessage() + ANSI_RESET);
                }
            }
        } catch (IOException ex) {
            System.out.println(ANSI_RED + "Error starting server: " + ex.getMessage() + ANSI_RESET);
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                threadPool.shutdown();
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
                System.out.println(ANSI_GREEN + "Server shut down successfully." + ANSI_RESET);
            } catch (IOException | InterruptedException ex) {
                System.out.println(ANSI_RED + "Error closing server: " + ex.getMessage() + ANSI_RESET);
            }
        }
    }

    public static void main(String args[]) {
        loadProperties();
        generateKeysIfNotExists();
        initializeMongoConnection();
        startServer();
    }
}
