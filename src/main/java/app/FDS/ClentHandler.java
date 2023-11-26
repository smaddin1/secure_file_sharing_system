package app.FDS;

import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import app.Models.Payloads.DeleteFileResponsePayload;
import app.Models.Payloads.Peer.ListFilesResponsePayload;
import app.constants.Constants;
import app.utils.RandomUniquePicker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import static com.mongodb.client.model.Filters.eq;

import app.Models.Payloads.*;
import app.MongoConnectionManager;
import app.constants.KeyManager;
import app.utils.AES;
import app.utils.CObject;
import app.utils.RSA;
import com.mongodb.client.MongoDatabase;

import app.Models.PeerDB;
import app.Models.PeerInfo;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import javax.crypto.SecretKey;

import static app.constants.Constants.TerminalColors.*;

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private PeerInfo peerInfo;
    private static Map<String, PeerDB> peerDBMap = new ConcurrentHashMap<>();
    private final Properties properties;
    private final String peerDBFile = Paths.get(Constants.FilePaths.FDSStorageBucket, "peerDBMap.dat").toString();

    public ClientHandler(Socket clientSocket, Properties properties) {
        this.clientSocket = clientSocket;
        this.properties = properties;
    }

    public void run() {
        System.out.println(ANSI_BLUE + "Thread started: " + Thread.currentThread() + "\n" + ANSI_RESET);
        try (ObjectInputStream clientReader = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream clientWriter = new ObjectOutputStream(clientSocket.getOutputStream())) {

            processClientRequests(clientReader, clientWriter);

        } catch (IOException e) {
            handleException("IOException", e);
        } catch (ClassNotFoundException e) {
            handleException("ClassNotFoundException", e);
        } catch (Exception e) {
            handleException("Exception", e);
        }
    }

    private void processClientRequests(ObjectInputStream clientReader, ObjectOutputStream clientWriter) throws Exception {
        Object clientInput;
        while ((clientInput = clientReader.readObject()) != null) {
            processClientInput(clientInput, clientWriter);
        }
    }

    private void processClientInput(Object clientInput, ObjectOutputStream clientWriter) throws Exception {
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

    private void handleException(String exceptionType, Exception e) {
        System.out.println(ANSI_RED + exceptionType + ": " + e.getMessage() + ANSI_RESET);
        if (this.peerInfo != null) {
            PeerDB peerDBItem = peerDBMap.get(this.peerInfo.getPeer_id());
            if (peerDBItem != null) {
                peerDBItem.setActive(false);
                peerDBMap.put(this.peerInfo.getPeer_id(), peerDBItem);
            }
        }
        e.printStackTrace();
    }

    private ResponsePayload processInput(Payload clientPayload) throws Exception {
        PeerInfo peerInfo = clientPayload.getPeerInfo();
        String peer_id = this.peerInfo.getPeer_id();
        System.out.println(ANSI_BLUE + "Serving Peer: " + peer_id);
        System.out.println("Executing: " + clientPayload.getCommand() + ANSI_RESET);

        ResponsePayload responsePayload;
        MongoDatabase db = MongoConnectionManager.getDatabase();

        String commandName = clientPayload.getCommand();
        switch (commandName) {
            case "registerPeer":
                InitPayload initPayload = (InitPayload) clientPayload;
                byte[] keyBytes = RSA.decrypt(initPayload.getKey(), KeyManager.getPrivateKey());
                SecretKey key = AES.getSecretKey(keyBytes);

                PeerDB peerDBItem = new PeerDB(peerInfo, true, key);
                peerDBMap.put(peer_id, peerDBItem);

                // dump data to .dat file
                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(peerDBFile))) {
                    out.writeObject(peerDBMap);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String response = String.format("FDS: ACK: %s registered successfully", peer_id);
                responsePayload = new ResponsePayload.Builder()
                    .setStatusCode(200)
                    .setMessage(response)
                    .build();
                break;
            case "registerKey":
                initPayload = (InitPayload) clientPayload;
                System.out.println(ANSI_BLUE + "Registering key for peer " + peer_id + ANSI_RESET);
                keyBytes = RSA.decrypt(initPayload.getKey(), KeyManager.getPrivateKey());
                key = AES.getSecretKey(keyBytes);
                peerDBItem = peerDBMap.get(peer_id);
                peerDBItem.setKey(key);
                peerDBMap.put(peer_id, peerDBItem);

                response = "FDS: ACK: SecretKey register successfully";
                responsePayload = new ResponsePayload.Builder()
                    .setStatusCode(200)
                    .setMessage(response)
                    .build();
                break;
            case "mkdir":
                CreateFilePayload createFilePayload = (CreateFilePayload) clientPayload;

                responsePayload = registerFile(db, createFilePayload, true);

                break;
            case "touch":
                createFilePayload = (CreateFilePayload) clientPayload;

                responsePayload = registerFile(db, createFilePayload, false);

                break;
            case "ls":
                ListFilesPayload listFilesPayload = (ListFilesPayload) clientPayload;
                String parent = listFilesPayload.getPwd().equals("/") ? "": listFilesPayload.getPwd();

                // check for starts with
                Pattern pattern = Pattern.compile("^" + parent + ".*");

                // get Collection
                MongoCollection<Document> collection = db.getCollection("file_metadata");

                FindIterable<Document> results = collection.find(new Document("parent", pattern));
                MongoCursor<Document> cursor = results.iterator();
                List<String> allLines = new ArrayList<>();
                while (cursor.hasNext()) {
                    Map<String, Object> documentMap = new HashMap<>(cursor.next());
                    Map<String, String> permissions = (Map<String, String>) documentMap.get("permissions");
                    String owner = (String) documentMap.get("owner");
                    boolean isDeleted = (boolean) documentMap.get("isDeleted");

                    if ((owner.equals(peer_id)
                        || permissions.size() == 0
                            || checkPermissions(permissions, peer_id, "r")
                                || checkPermissions(permissions, peer_id, "w"))
                                    && !documentMap.get("name").equals("")) {

                        // do not show deleted files to non-owner peers
                        if (isDeleted && !owner.equals(peer_id)) {
                            continue;
                        }
                        Date date = ((ObjectId) documentMap.get("_id")).getDate();
                        String formattedDate = new SimpleDateFormat("MMM dd HH:mm").format(date);
                        int replicatedPeerCount = ((ArrayList<String>) documentMap.get("replicatedPeers")).size();

                        StringBuilder sb = new StringBuilder();

                        sb.append((boolean) documentMap.get("isDirectory") ? "d" : "-");
                        sb.append(" ");
                        sb.append(permissions.get(peer_id) != null ? permissions.get(peer_id) : "w");
                        sb.append(" ");
                        sb.append(String.format("%s", isDeleted ? "* ": "  "));
                        sb.append(replicatedPeerCount);
                        sb.append(" ");
                        sb.append(String.format("%-" + 15 + "s", documentMap.get("owner")));
                        sb.append(String.format("%-" + 15 + "s", formattedDate));
                        sb.append(Paths.get((String) documentMap.get("parent"), (String) documentMap.get("name")).normalize());
                        allLines.add(sb.toString());
                    }
                }

                String message = String.format("%d: files/directories found", allLines.size());
                responsePayload = new ListFilesResponsePayload.Builder()
                    .setLines(allLines)
                    .setStatusCode(200)
                    .setMessage(message)
                    .build();
                break;
            case "cd":
                ChangeDirectoryPayload changeDirectoryPayload = (ChangeDirectoryPayload) clientPayload;
                String path = Paths.get(changeDirectoryPayload.getPwd(), changeDirectoryPayload.getChangeInto()).normalize().toString();

                collection = db.getCollection("file_metadata");

                Document query = new Document("$expr", new Document("$eq", Arrays.asList(new Document("$concat", Arrays.asList("$parent", "$name")), path)));
                Document document = collection.find(query).first();

                int statusCode;
                if (document != null) {
                    Map<String, Object> documentMap = convertDocumentToMap(document);
                    Map<String, String> permissions = (Map<String, String>) documentMap.get("permissions");

                    // check if the peer has read or write permissions over the directory
                    if (permissions.size() == 0
                        || checkPermissions(permissions, peer_id, "r")
                            || checkPermissions(permissions, peer_id, "w")
                                || documentMap.get("owner").equals(peer_id)) {
                        if ((boolean) document.get("isDirectory")) {
                            message = "";
                            statusCode = 200;
                        } else {
                            message = String.format("%s: Can't CD into a file", path);
                            statusCode = 400;
                        }
                    } else {
                        message = String.format("%s: Access denied", path);
                        statusCode = 401;
                    }
                } else {
                    message = String.format("%s: Not found", path);
                    statusCode = 404;
                }

                responsePayload = new ResponsePayload.Builder()
                    .setStatusCode(statusCode)
                    .setMessage(message)
                    .build();
                break;
            case "rm":
                DeleteFilePayload deleteFilePayload = (DeleteFilePayload) clientPayload;

                collection = db.getCollection("file_metadata");
                String absoluteFilePath = Paths.get(deleteFilePayload.getParent(), deleteFilePayload.getFileName()).normalize().toString();

                Bson filters = Filters.and(
                    eq("$expr", new Document("$eq", Arrays.asList(new Document("$concat", Arrays.asList("$parent", "$name")), absoluteFilePath))),
                    eq("isDirectory", false)
                );
                document = collection.find(filters).first();

                Map<String, Integer> toBeDeletedPeers = null;

                if (document != null) {
                    Map<String, Object> documentMap = convertDocumentToMap(document);
                    String owner = (String) documentMap.get("owner");

                    // if user is trying to delete root folder or
                    // any folder they are not owner to
                    if (!owner.equals(peer_id) || owner.equals("admin")) {
                        message = String.format("FDS: `rm %s`: Access Denied", absoluteFilePath);
                        statusCode = 401;
                    } else {
                        if ((boolean) documentMap.get("isDeleted")) {
                            message = String.format("FDS: `%s` already deleted.\nTo restore file: `restore --filename`", absoluteFilePath);
                            statusCode = 400;
                        } else {
                            ArrayList<String> replicatedPeerIds = (ArrayList<String>) document.get("replicatedPeers");

                            toBeDeletedPeers = replicatedPeerIds.stream()
                                .filter(peerId -> !peerId.equals(peer_id))
                                .filter(peerDBMap::containsKey)
                                .collect(Collectors.toMap(
                                    Function.identity(),
                                    peerId -> peerDBMap.get(peerId).getPort_no()
                                ));

                            Document update = new Document("$set", new Document()
                                .append("isDeleted", true)
                                .append("replicatedPeers", new HashSet<>() {{
                                    add(peer_id);
                                }}));

                            UpdateResult result = collection.updateOne(filters, update);

                            if (result.getModifiedCount() == 1) {
                                message = String.format("FDS: ACK: `%s` marked as deleted", absoluteFilePath);
                                statusCode = 200;
                            } else {
                                message = String.format("FDS: Error deleting %s", absoluteFilePath);
                                statusCode = 400;
                            }
                        }
                    }
                } else {
                    message = String.format("FDS: `%s` not found", absoluteFilePath);
                    statusCode = 404;
                }

                responsePayload = new DeleteFileResponsePayload.Builder()
                    .setMessage(message)
                    .setStatusCode(statusCode)
                    .setToBeDeletedPeers(toBeDeletedPeers)
                    .build();
                break;
            case "restore":
                RestoreFilePayload restoreFilePayload = (RestoreFilePayload) clientPayload;
                absoluteFilePath = Paths.get(restoreFilePayload.getParent(), restoreFilePayload.getFileName()).normalize().toString();

                collection = db.getCollection("file_metadata");

                filters = Filters.and(
                    eq("owner", peer_id),
                    eq("isDeleted", true),
                    eq("$expr", new Document("$eq", Arrays.asList(new Document("$concat", Arrays.asList("$parent", "$name")), absoluteFilePath)))
                );

                document = collection.find(filters).first();

                Map<String, Integer> toBeReplicatedPeers = null;
                if (document != null) {
                    String[] activePeers = peerDBMap.values()
                        .stream()
                        .filter(PeerDB::isActive)
                        .map(PeerDB::getPeer_id)
                        .toArray(String[]::new);
                    Set<String> uniqueRandomPeers = RandomUniquePicker.pick(activePeers, Integer.parseInt(this.properties.getProperty("REPLICATION_FACTOR")));

                    toBeReplicatedPeers = uniqueRandomPeers.stream()
                        .filter(peerDBMap::containsKey)
                        .filter(peer -> !peer.equals(peer_id))
                        .collect(Collectors.toMap(Function.identity(), peer -> peerDBMap.get(peer).getPort_no()));

                    Document update = new Document("$set", new Document()
                        .append("isDeleted", false)
                        .append("replicatedPeers", uniqueRandomPeers)
                    );

                    UpdateResult result = collection.updateOne(filters, update);

                    if (result.getModifiedCount() == 1) {
                        message = String.format("FDS: ACK: `%s` marked as deleted", absoluteFilePath);
                        statusCode = 200;
                    } else {
                        message = String.format("FDS: Error deleting %s", absoluteFilePath);
                        statusCode = 400;
                    }
                } else {
                    message = String.format("FDS: `%s` file not found", absoluteFilePath);
                    statusCode = 404;
                }

                responsePayload = new RestoreFileResponsePayload.Builder()
                    .setMessage(message)
                    .setStatusCode(statusCode)
                    .setToBeReplicatedPeers(toBeReplicatedPeers)
                    .build();
                break;
            case "chmod":
                UpdatePermissionsPayload updatePermissionsPayload = (UpdatePermissionsPayload) clientPayload;

                absoluteFilePath = Paths.get(updatePermissionsPayload.getParent(), updatePermissionsPayload.getFileName()).normalize().toString();

                collection = db.getCollection("file_metadata");

                filters = Filters.and(
                    eq("$expr", new Document("$eq", Arrays.asList(new Document("$concat", Arrays.asList("$parent", "$name")), absoluteFilePath))),
                    eq("isDeleted", false)
                );

                document = collection.find(filters).first();

                if (document != null) {
                    Map<String, Object> documentMap = convertDocumentToMap(document);
                    String owner = (String) documentMap.get("owner");

                    if (owner.equals(peer_id)) {
                        Map<String, String> existingPermissions = (Map<String, String>) documentMap.get("permissions");
                        Map<String, String> newPermissions = new HashMap<>(existingPermissions);

                        newPermissions.putAll(updatePermissionsPayload.getAccessList());

                        Document update = new Document("$set", new Document("permissions", newPermissions));

                        UpdateResult result = collection.updateOne(filters, update);

                        message = String.format("FDS: `%s` permissions updated successfully", absoluteFilePath);
                        statusCode = 200;
                    } else {
                        message = String.format("FDS: `%s` permissions can be changed only by %s", absoluteFilePath, owner);
                        statusCode = 401;
                    }
                } else {
                    message = String.format("FDS: `%s` not found", absoluteFilePath);
                    statusCode = 404;
                }

                responsePayload = new ResponsePayload.Builder()
                    .setMessage(message)
                    .setStatusCode(statusCode)
                    .build();
                break;
            default:
                responsePayload = new ResponsePayload.Builder()
                    .setStatusCode(400)
                    .setMessage("FDS: Command handler not found")
                    .build();
                System.out.println(ANSI_YELLOW + "Invalid command issued: " + commandName + ANSI_RESET);
        }

        return responsePayload;
    }

    private ResponsePayload registerFile(MongoDatabase db, CreateFilePayload createFilePayload, boolean isDirectory) {
        ResponsePayload responsePayload;
        String peer_id = this.peerInfo.getPeer_id();

        String message;
        int statusCode;
        String parent = createFilePayload.getParent().equals("/") ? "" : createFilePayload.getParent();
        Map<String, Integer> toBeReplicatedPeers = null;
        boolean isReadOnly = true;

        // get Collection
        MongoCollection<Document> collection = db.getCollection("file_metadata");

        Document query = new Document("$expr", new Document("$eq", Arrays.asList(new Document("$concat", Arrays.asList("$parent", "$name")), parent)));
        Document document = collection.find(query).first();

        if (document != null) {
            Map<String, Object> documentMap = convertDocumentToMap(document);
            Map<String, String> permissions = (Map<String, String>) documentMap.get("permissions");

            boolean hasPermission = checkPermissions(permissions, peer_id, "w")
                || permissions.size() == 0
                || documentMap.get("owner").equals(peer_id);

            // the parent where the file is being created should not be deleted
            // the peer should have permissions to create the file in the parent
            // or it should be root directory (every peer can create a file/folder in root)
            if ((!(boolean) documentMap.get("isDeleted")) && (parent.equals("/") || hasPermission)) {
                String[] activePeers = peerDBMap.values()
                    .stream()
                    .filter(PeerDB::isActive)
                    .map(PeerDB::getPeer_id)
                    .toArray(String[]::new);
                Set<String> uniqueRandomPeers = RandomUniquePicker.pick(activePeers, Integer.parseInt(this.properties.getProperty("REPLICATION_FACTOR")));

                // In case `RandomUniquePicker.pick` missed it, add owner peer
                if (createFilePayload.getParent().equals(peer_id)) {
                    uniqueRandomPeers.add(peer_id);
                }

                toBeReplicatedPeers = uniqueRandomPeers.stream()
                    .filter(peerDBMap::containsKey)
                    .collect(Collectors.toMap(Function.identity(), peer -> peerDBMap.get(peer).getPort_no()));

                // create new Document
                document = new Document();
                document.append("name", createFilePayload.getFileName());
                document.append("owner", peer_id);
                document.append("parent", parent);
                document.append("permissions", createFilePayload.getAccessList());
                document.append("isDirectory", isDirectory);
                document.append("isDeleted", false);
                document.append("replicatedPeers", uniqueRandomPeers);

                try {
                    // insert document into collection
                    collection.insertOne(document);

                    message = String.format("`%s` registered on network", createFilePayload.getFileName());
                    statusCode = 201;

                    isReadOnly = false;
                } catch (MongoWriteException e) {
                    message = "Unknown Exception caused while writing to MongoDB";
                    statusCode = 400;
                    if (e.getCode() == 11000) {
                        List<Bson> filters = new ArrayList<>();
                        filters.add(Filters.eq("parent", parent));
                        filters.add(Filters.eq("name", createFilePayload.getFileName()));

                        // Construct the query
                        Bson BsonQuery = Filters.and(filters);

                        document = collection.find(BsonQuery).first();

                        if (document != null) {
                            documentMap = convertDocumentToMap(document);
                            ArrayList<String> replicatedPeerIds = (ArrayList<String>) documentMap.get("replicatedPeers");
                            permissions = (Map<String, String>) documentMap.get("permissions");

                            if (!(boolean) documentMap.get("isDeleted")) {
                                toBeReplicatedPeers = replicatedPeerIds.stream()
                                    .filter(peerDBMap::containsKey)
                                    .collect(Collectors.toMap(
                                        Function.identity(),
                                        peerId -> peerDBMap.get(peerId).getPort_no()
                                    ));
                                isReadOnly = permissions != null && checkPermissions(permissions, peer_id, "r");
                                if (permissions == null || documentMap.get("owner").equals(peer_id)) {
                                    isReadOnly = false;
                                }
                                message = String.format("`%s` already exists on the network", Paths.get(parent, createFilePayload.getFileName()).normalize());
                                statusCode = 409;
                            } else {
                                String owner = (String) documentMap.get("owner");
                                String fileAbsolutePath = Paths.get(parent, createFilePayload.getFileName()).toString();
                                message =  owner.equals(peer_id) ?
                                    String.format("`%s` was deleted.\nRun `restore %s` to restore the file.", fileAbsolutePath, fileAbsolutePath) :
                                    String.format("`%s` Not found!", fileAbsolutePath);
                                statusCode = owner.equals(peer_id) ?
                                    401 :
                                    404;
                            }
                        }
                    } else {
                        toBeReplicatedPeers = null;
                    }
                }
            } else {
                message = String.format("`%s` do not have permissions to create file at `%s`", peer_id, parent);
                statusCode = 401;
            }
        } else {
            statusCode = 404;
            message = String.format("`%s` Not found!", parent);
        }

        responsePayload = new CreateFileResponsePayload.Builder()
            .setStatusCode(statusCode)
            .setMessage(message)
            .setToBeReplicatedPeers(toBeReplicatedPeers)
            .setReadOnly(isReadOnly)
            .build();

        return responsePayload;
    }

    public boolean checkPermissions(Map<String, String> permissions, String peerId, String permissionType) {
        String permission = permissions.getOrDefault(peerId, "");

        return permission.equals(permissionType);
    }

    public Map<String, Object> convertDocumentToMap(Document document) {
        String jsonString = document.toJson();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> documentMap = null;
        try {
            documentMap = objectMapper.readValue(jsonString, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return documentMap;
    }
}