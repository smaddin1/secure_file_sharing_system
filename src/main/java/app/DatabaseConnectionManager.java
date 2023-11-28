package app;

import app.exceptions.DatabaseConnectionException;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

public class DatabaseConnectionManager {
    private static MongoClient mongoClient;
    private static MongoDatabase database;

    public static void establishConnection(ServerConfigurator configurator) throws DatabaseConnectionException {
        try {
            String connectionString = configurator.getProperty("CONNECTION_STRING");
            String dbName = configurator.getProperty("DATABASE_NAME");

            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(dbName);
            System.out.println("Connected to MongoDB database: " + dbName);
        } catch (Exception e) {
            throw new DatabaseConnectionException("Failed to establish MongoDB connection", e);
        }
    }

    public static MongoDatabase getDatabase() {
        if (database == null) {
            throw new IllegalStateException("Database connection has not been established.");
        }
        return database;
    }
}
