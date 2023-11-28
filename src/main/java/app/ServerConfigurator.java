package app;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfigurator {
    private Properties properties;

    private ServerConfigurator(Properties properties) {
        this.properties = properties;
    }

    public static ServerConfigurator loadConfiguration() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
        return new ServerConfigurator(properties);
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public int getIntProperty(String key) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid format for integer property: " + key, e);
            }
        }
        throw new RuntimeException("Property not found: " + key);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        saveProperties();
    }

    private void saveProperties() {
        try (FileOutputStream output = new FileOutputStream("config.properties")) {
            properties.store(output, null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save configuration", e);
        }
    }
}
