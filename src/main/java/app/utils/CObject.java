package app.utils;

import java.io.*;

/**
 * The CObject class provides static utility methods for converting objects to a byte array and vice versa.
 * This is commonly used for serialization and deserialization of objects for purposes such as deep copying,
 * saving to a file, or sending over a network.
 */
public class CObject {

    /**
     * Converts a serializable object to a byte array.
     *
     * @param object The object to be serialized. Must implement the Serializable interface.
     * @return A byte array representing the serialized object.
     * @throws IOException If any I/O error occurs during writing to the ByteArrayOutputStream.
     */
    public static byte[] objectToBytes(Object object) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Converts a byte array back to an object.
     *
     * @param bytes The byte array to be deserialized into an object.
     * @return The deserialized object.
     * @throws IOException If an I/O error occurs while reading from the ByteArrayInputStream.
     * @throws ClassNotFoundException If the class of the serialized object cannot be found.
     */
    public static Object bytesToObject(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object obj = ois.readObject();
        ois.close();
        bis.close();
        return obj;
    }
}

